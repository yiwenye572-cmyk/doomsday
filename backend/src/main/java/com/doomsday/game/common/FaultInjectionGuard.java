package com.doomsday.game.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FaultInjectionGuard {

    private final boolean enabled;

    public FaultInjectionGuard(@Value("${game.chaos.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public void check(String failpoint) {
        if (!enabled) {
            return;
        }

        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            return;
        }

        HttpServletRequest request = servletAttrs.getRequest();
        String configured = request.getHeader("X-Doomsday-Failpoint");
        if (configured == null || configured.isBlank()) {
            return;
        }
        if (!configured.equals(failpoint)) {
            return;
        }

        throw new ApiException(
                "INJECTED_FAILURE",
                "fault injected at " + failpoint,
                Map.of("failpoint", failpoint)
        );
    }
}
