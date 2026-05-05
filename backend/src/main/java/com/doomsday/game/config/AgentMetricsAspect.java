package com.doomsday.game.config;

import com.doomsday.game.admin.AgentMetricsStore;
import com.doomsday.game.agent.TurnContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class AgentMetricsAspect {

    private static final Logger log = LoggerFactory.getLogger(AgentMetricsAspect.class);

    private final MeterRegistry meterRegistry;
    private final AgentMetricsStore metricsStore;

    public AgentMetricsAspect(MeterRegistry meterRegistry, AgentMetricsStore metricsStore) {
        this.meterRegistry = meterRegistry;
        this.metricsStore = metricsStore;
    }

    @Around("execution(* com.doomsday.game.agent.impl.*Agent.handle(..))")
    public Object aroundAgentHandle(ProceedingJoinPoint pjp) throws Throwable {
        String agent = pjp.getTarget().getClass().getSimpleName();
        long startedAt = System.nanoTime();
        // 尝试从参数中提取 TurnContext 以记录 span
        TurnContext ctx = extractContext(pjp);
        long queueWaitMs = 0;
        if (ctx != null) {
            Object previousDoneNs = ctx.extras.get("metrics.previousAgentDoneNs");
            if (previousDoneNs instanceof Long ns) {
                queueWaitMs = Math.max(0, (startedAt - ns) / 1_000_000);
            }
        }
        try {
            Object ret = pjp.proceed();
            long doneAt = System.nanoTime();
            long elapsedMs = (doneAt - startedAt) / 1_000_000;
            long modelMs = 0;
            int promptTokens = 0;
            int completionTokens = 0;
            int totalTokens = 0;
            String modelName = null;
            if (ctx != null) {
                TurnContext.LlmMetricAgg llm = ctx.llmMetric(agent);
                if (llm != null) {
                    modelMs = llm.modelMs;
                    promptTokens = llm.promptTokens;
                    completionTokens = llm.completionTokens;
                    totalTokens = llm.totalTokens;
                    modelName = llm.modelName;
                }
                ctx.extras.put("metrics.previousAgentDoneNs", doneAt);
            }
            long postProcessMs = Math.max(0, elapsedMs - modelMs);
            double tokensPerSecond = modelMs > 0 ? totalTokens * 1000.0 / modelMs : 0.0;

            record(agent, "success", startedAt);
            recordStage(agent, "queue_wait", queueWaitMs);
            recordStage(agent, "model", modelMs);
            recordStage(agent, "post_process", postProcessMs);
            meterRegistry.counter("doomsday.agent.llm.tokens", "agent", agent, "type", "prompt")
                    .increment(promptTokens);
            meterRegistry.counter("doomsday.agent.llm.tokens", "agent", agent, "type", "completion")
                    .increment(completionTokens);

            metricsStore.recordAgentCall(
                    agent,
                    elapsedMs,
                    queueWaitMs,
                    modelMs,
                    postProcessMs,
                    promptTokens,
                    completionTokens,
                    totalTokens,
                    true
            );
            if (ctx != null) {
                ctx.agentSpans.add(new AgentMetricsStore.AgentSpan(
                        agent,
                        elapsedMs,
                        "success",
                        null,
                        queueWaitMs,
                        modelMs,
                        postProcessMs,
                        promptTokens,
                        completionTokens,
                        totalTokens,
                        tokensPerSecond,
                        modelName
                ));
            }
            log.info("[AgentMetrics] agent={} status=success elapsed={}ms queue={}ms model={}ms post={}ms tokens={} tps={} traceId={}",
                    agent, elapsedMs, queueWaitMs, modelMs, postProcessMs, totalTokens,
                    String.format("%.2f", tokensPerSecond), ctx != null ? ctx.traceId : "-");
            return ret;
        } catch (Throwable ex) {
            long doneAt = System.nanoTime();
            long elapsedMs = (doneAt - startedAt) / 1_000_000;
            long modelMs = 0;
            int promptTokens = 0;
            int completionTokens = 0;
            int totalTokens = 0;
            String modelName = null;
            if (ctx != null) {
                TurnContext.LlmMetricAgg llm = ctx.llmMetric(agent);
                if (llm != null) {
                    modelMs = llm.modelMs;
                    promptTokens = llm.promptTokens;
                    completionTokens = llm.completionTokens;
                    totalTokens = llm.totalTokens;
                    modelName = llm.modelName;
                }
                ctx.extras.put("metrics.previousAgentDoneNs", doneAt);
            }
            long postProcessMs = Math.max(0, elapsedMs - modelMs);
            double tokensPerSecond = modelMs > 0 ? totalTokens * 1000.0 / modelMs : 0.0;

            record(agent, "error", startedAt);
            recordStage(agent, "queue_wait", queueWaitMs);
            recordStage(agent, "model", modelMs);
            recordStage(agent, "post_process", postProcessMs);
            meterRegistry.counter("doomsday.agent.llm.tokens", "agent", agent, "type", "prompt")
                    .increment(promptTokens);
            meterRegistry.counter("doomsday.agent.llm.tokens", "agent", agent, "type", "completion")
                    .increment(completionTokens);

            metricsStore.recordAgentCall(
                    agent,
                    elapsedMs,
                    queueWaitMs,
                    modelMs,
                    postProcessMs,
                    promptTokens,
                    completionTokens,
                    totalTokens,
                    false
            );
            if (ctx != null) {
                ctx.agentSpans.add(new AgentMetricsStore.AgentSpan(
                        agent,
                        elapsedMs,
                        "error",
                        ex.getMessage(),
                        queueWaitMs,
                        modelMs,
                        postProcessMs,
                        promptTokens,
                        completionTokens,
                        totalTokens,
                        tokensPerSecond,
                        modelName
                ));
            }
            log.warn("[AgentMetrics] agent={} status=error elapsed={}ms queue={}ms model={}ms post={}ms tokens={} tps={} traceId={} message={}",
                    agent, elapsedMs, queueWaitMs, modelMs, postProcessMs, totalTokens,
                    String.format("%.2f", tokensPerSecond), ctx != null ? ctx.traceId : "-", ex.getMessage());
            throw ex;
        }
    }

    private void record(String agent, String status, long startedAt) {
        long elapsed = System.nanoTime() - startedAt;
        Timer.builder("doomsday.agent.handle.latency")
                .tag("agent", agent)
                .tag("status", status)
                .register(meterRegistry)
                .record(elapsed, TimeUnit.NANOSECONDS);
    }

    private void recordStage(String agent, String stage, long elapsedMs) {
        Timer.builder("doomsday.agent.stage.latency")
                .tag("agent", agent)
                .tag("stage", stage)
                .register(meterRegistry)
                .record(Math.max(0, elapsedMs), TimeUnit.MILLISECONDS);
    }

    private TurnContext extractContext(ProceedingJoinPoint pjp) {
        for (Object arg : pjp.getArgs()) {
            if (arg instanceof TurnContext tc) return tc;
        }
        return null;
    }
}
