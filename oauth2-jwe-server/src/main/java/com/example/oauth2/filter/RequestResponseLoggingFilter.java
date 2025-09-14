package com.example.oauth2.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestResponseLoggingFilter implements Filter {

    private static final Logger requestLogger = LoggerFactory.getLogger("REQUEST_RESPONSE_LOGGER");
    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Generate trace ID for request tracking
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);

        long startTime = System.currentTimeMillis();

        try {
            // Log incoming request
            logRequest(httpRequest, traceId);

            // Continue with the request
            chain.doFilter(request, response);

            // Log response
            long duration = System.currentTimeMillis() - startTime;
            logResponse(httpRequest, httpResponse, duration, traceId);

        } finally {
            MDC.clear();
        }
    }

    private void logRequest(HttpServletRequest request, String traceId) {
        try {
            String clientIp = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String contentType = request.getContentType();

            StringBuilder logMessage = new StringBuilder();
            logMessage.append("INCOMING REQUEST [").append(traceId).append("] ");
            logMessage.append("Method: ").append(request.getMethod()).append(" ");
            logMessage.append("URI: ").append(request.getRequestURI());

            if (request.getQueryString() != null) {
                logMessage.append("?").append(request.getQueryString());
            }

            logMessage.append(" Client-IP: ").append(clientIp);
            logMessage.append(" Content-Type: ").append(contentType != null ? contentType : "N/A");
            logMessage.append(" User-Agent: ").append(userAgent != null ? userAgent : "N/A");

            requestLogger.info(logMessage.toString());

        } catch (Exception e) {
            logger.error("Error logging request", e);
        }
    }

    private void logResponse(HttpServletRequest request, HttpServletResponse response,
                             long duration, String traceId) {
        try {
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("OUTGOING RESPONSE [").append(traceId).append("] ");
            logMessage.append("Method: ").append(request.getMethod()).append(" ");
            logMessage.append("URI: ").append(request.getRequestURI()).append(" ");
            logMessage.append("Status: ").append(response.getStatus()).append(" ");
            logMessage.append("Duration: ").append(duration).append("ms");

            requestLogger.info(logMessage.toString());

        } catch (Exception e) {
            logger.error("Error logging response", e);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Request/Response logging filter initialized");
    }

    @Override
    public void destroy() {
        logger.info("Request/Response logging filter destroyed");
    }
}