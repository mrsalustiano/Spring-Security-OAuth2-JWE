package com.example.client.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        logRequest(request, body);

        long startTime = System.currentTimeMillis();
        ClientHttpResponse response = execution.execute(request, body);
        long duration = System.currentTimeMillis() - startTime;

        logResponse(response, duration);

        return response;
    }

    private void logRequest(HttpRequest request, byte[] body) {
        if (logger.isDebugEnabled()) {
            logger.debug("=== HTTP REQUEST ===");
            logger.debug("Method: {}", request.getMethod());
            logger.debug("URI: {}", request.getURI());
            logger.debug("Headers: {}", request.getHeaders());

            if (body.length > 0) {
                String bodyStr = new String(body, StandardCharsets.UTF_8);
                // Don't log sensitive information like passwords
                if (!bodyStr.contains("password") && !bodyStr.contains("client_secret")) {
                    logger.debug("Body: {}", bodyStr);
                } else {
                    logger.debug("Body: [SENSITIVE DATA HIDDEN]");
                }
            }
            logger.debug("==================");
        } else {
            logger.info("HTTP Request: {} {}", request.getMethod(), request.getURI());
        }
    }

    private void logResponse(ClientHttpResponse response, long duration) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("=== HTTP RESPONSE ===");
            logger.debug("Status: {}", response.getStatusCode());
            logger.debug("Headers: {}", response.getHeaders());
            logger.debug("Duration: {}ms", duration);
            logger.debug("====================");
        } else {
            logger.info("HTTP Response: {} - Duration: {}ms", response.getStatusCode(), duration);
        }
    }
}