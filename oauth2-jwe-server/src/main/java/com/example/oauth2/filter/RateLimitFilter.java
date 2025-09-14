package com.example.oauth2.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    @Value("${rate-limit.auth.requests-per-second:5}")
    private int requestsPerSecond;

    @Value("${rate-limit.auth.burst-capacity:10}")
    private int burstCapacity;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Only apply rate limiting to auth endpoints
        String requestURI = httpRequest.getRequestURI();
        if (!requestURI.startsWith("/auth/oauth/v2/")) {
            chain.doFilter(request, response);
            return;
        }

        String clientId = getClientIdentifier(httpRequest);
        Bucket bucket = getBucket(clientId);

        if (bucket.tryConsume(1)) {
            logger.debug("Request allowed for client: {}", clientId);
            chain.doFilter(request, response);
        } else {
            logger.warn("Rate limit exceeded for client: {}", clientId);
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                    "{\"error\":\"rate_limit_exceeded\",\"message\":\"Too many requests. Please try again later.\"}"
            );
        }
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get client_id from request body or parameters
        String clientId = request.getParameter("client_id");
        if (clientId != null) {
            return clientId;
        }

        // Fall back to IP address
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private Bucket getBucket(String clientId) {
        return buckets.computeIfAbsent(clientId, this::createBucket);
    }

    private Bucket createBucket(String clientId) {
        logger.debug("Creating rate limit bucket for client: {}", clientId);

        Bandwidth limit = Bandwidth.classic(burstCapacity, Refill.intervally(requestsPerSecond, Duration.ofSeconds(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Rate limit filter initialized with {} requests per second, burst capacity: {}",
                requestsPerSecond, burstCapacity);
    }

    @Override
    public void destroy() {
        buckets.clear();
        logger.info("Rate limit filter destroyed");
    }
}