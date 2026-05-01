package com.doomsday.game.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AgentMetricsAspect {

    private static final Logger log = LoggerFactory.getLogger(AgentMetricsAspect.class);

    private final MeterRegistry meterRegistry;

    public AgentMetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("execution(* com.doomsday.game.agent.impl.*Agent.handle(..))")
    public Object aroundAgentHandle(ProceedingJoinPoint pjp) throws Throwable {
        String agent = pjp.getTarget().getClass().getSimpleName();
        long startedAt = System.nanoTime();
        try {
            Object ret = pjp.proceed();
            record(agent, "success", startedAt);
            return ret;
        } catch (Throwable ex) {
            record(agent, "error", startedAt);
            log.warn("[AgentMetrics] agent={} status=error message={}", agent, ex.getMessage());
            throw ex;
        }
    }

    private void record(String agent, String status, long startedAt) {
        long elapsed = System.nanoTime() - startedAt;
        Timer.builder("doomsday.agent.handle.latency")
                .tag("agent", agent)
                .tag("status", status)
                .register(meterRegistry)
                .record(elapsed, java.util.concurrent.TimeUnit.NANOSECONDS);
    }
}
