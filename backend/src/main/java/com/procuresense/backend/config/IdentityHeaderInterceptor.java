package com.procuresense.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class IdentityHeaderInterceptor implements HandlerInterceptor {

    public static final String ORG_HEADER = "X-Org-Id";
    public static final String ROLE_HEADER = "X-Role";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getRequestURI().startsWith("/actuator")) {
            return true;
        }
        boolean hasOrg = request.getHeader(ORG_HEADER) != null && !request.getHeader(ORG_HEADER).isBlank();
        boolean hasRole = request.getHeader(ROLE_HEADER) != null && !request.getHeader(ROLE_HEADER).isBlank();
        if (hasOrg && hasRole) {
            return true;
        }
        response.sendError(HttpStatus.BAD_REQUEST.value(), "Missing required identity headers");
        return false;
    }
}
