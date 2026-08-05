package com.gateway.filter;

import com.gateway.limiter.JavaRateLimiterManager;
import com.gateway.limiter.RateLimiter;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RateLimiterFilter implements Filter {
    private final RateLimiter rateLimiter = new JavaRateLimiterManager();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. Add CORS Headers
        httpResponse.setHeader("Access-Control-Allow-Origin", "*");
        httpResponse.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, X-Tenant-API-Key");

        // 2. Handle Browser Preflight OPTIONS Request
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // 3. Validate Tenant API Key
        String tenantKey = httpRequest.getHeader("X-Tenant-API-Key");
        if (tenantKey == null || tenantKey.trim().isEmpty()) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Missing X-Tenant-API-Key header\"}");
            return;
        }

        // 4. Check Rate Limit
        boolean allowed = rateLimiter.allowRequest(tenantKey, 50, 0.2);

        if (!allowed) {
            httpResponse.setStatus(429); // 429 Too Many Requests
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Rate limit exceeded. Please wait before retrying.\"}");
            return;
        }

        // 5. Pass Request Down Chain
        chain.doFilter(request, response);
    }
}