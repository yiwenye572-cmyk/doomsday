package com.doomsday.game.config;

import com.doomsday.game.common.TraceIdSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = TraceIdSupport.ensureTraceId(request.getHeader(TraceIdSupport.TRACE_HEADER));
        request.setAttribute(TraceIdSupport.TRACE_ATTRIBUTE, traceId);
        response.setHeader(TraceIdSupport.TRACE_HEADER, traceId);
        MDC.put(TraceIdSupport.TRACE_MDC_KEY, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceIdSupport.TRACE_MDC_KEY);
        }
    }
}
