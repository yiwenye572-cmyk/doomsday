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
        long startedAtMs = System.currentTimeMillis();
        // 尝试从参数中提取 TurnContext 以记录 span
        TurnContext ctx = extractContext(pjp);
        try {
            Object ret = pjp.proceed();
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            record(agent, "success", startedAt);
            metricsStore.recordAgentCall(agent, elapsedMs, true);
            if (ctx != null) {
                ctx.agentSpans.add(new AgentMetricsStore.AgentSpan(agent, elapsedMs, "success", null));
            }
            log.info("[AgentMetrics] agent={} status=success elapsed={}ms traceId={}",
                    agent, elapsedMs, ctx != null ? ctx.traceId : "-");
            return ret;
        } catch (Throwable ex) {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            record(agent, "error", startedAt);
            metricsStore.recordAgentCall(agent, elapsedMs, false);
            if (ctx != null) {
                ctx.agentSpans.add(new AgentMetricsStore.AgentSpan(agent, elapsedMs, "error", ex.getMessage()));
            }
            log.warn("[AgentMetrics] agent={} status=error elapsed={}ms traceId={} message={}",
                    agent, elapsedMs, ctx != null ? ctx.traceId : "-", ex.getMessage());
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

    private TurnContext extractContext(ProceedingJoinPoint pjp) {
        for (Object arg : pjp.getArgs()) {
            if (arg instanceof TurnContext tc) return tc;
        }
        return null;
    }
}
