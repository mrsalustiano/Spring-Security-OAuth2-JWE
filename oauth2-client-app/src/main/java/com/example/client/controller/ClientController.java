package com.example.client.controller;

import com.example.client.dto.ApiResponse;
import com.example.client.service.OAuth2ClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ClientController {

    private static final Logger logger = LoggerFactory.getLogger(ClientController.class);

    @Autowired
    private OAuth2ClientService oauth2ClientService;

    @GetMapping("/hello")
    public ResponseEntity<ApiResponse<String>> hello() {
        try {
            logger.info("Hello endpoint called - authenticating with OAuth2 server");

            // Get access token and make authenticated request
            String accessToken = oauth2ClientService.getAccessToken();
            logger.info("Access token obtained successfully");

            // Validate token
            boolean isValid = oauth2ClientService.validateToken(accessToken);
            if (!isValid) {
                logger.warn("Token validation failed");
                return ResponseEntity.ok(ApiResponse.error("Token validation failed", 401));
            }

            logger.info("Token validated successfully - returning authenticated response");
            ApiResponse<String> response = new ApiResponse<>("autenticado", 200, "hello");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error in hello endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Authentication failed: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile() {
        try {
            logger.info("Profile endpoint called");

            Map<String, Object> profile = oauth2ClientService.getUserProfile();
            ApiResponse<Map<String, Object>> response = new ApiResponse<>("autenticado", 200, profile);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get profile: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getData() {
        try {
            logger.info("Data endpoint called");

            Map<String, Object> data = oauth2ClientService.getProtectedData();
            ApiResponse<Map<String, Object>> response = new ApiResponse<>("autenticado", 200, data);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get data: " + e.getMessage(), 500));
        }
    }

    @PostMapping("/create-data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createData(@RequestBody Map<String, Object> requestData) {
        try {
            logger.info("Create data endpoint called with data: {}", requestData);

            ResponseEntity<Map> response = oauth2ClientService.makeAuthenticatedRequest(
                    "/data",
                    org.springframework.http.HttpMethod.POST,
                    requestData,
                    Map.class
            );

            ApiResponse<Map<String, Object>> apiResponse = new ApiResponse<>("autenticado", 200, response.getBody());

            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            logger.error("Error creating data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create data: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/token-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTokenInfo() {
        try {
            logger.info("Token info endpoint called");

            boolean hasValidToken = oauth2ClientService.hasValidToken();
            String accessToken = null;

            if (hasValidToken) {
                accessToken = oauth2ClientService.getAccessToken();
            }

            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("has_token", accessToken != null);
            tokenInfo.put("is_valid", hasValidToken);
            tokenInfo.put("token_length", accessToken != null ? accessToken.length() : 0);
            tokenInfo.put("token_preview", accessToken != null ?
                    accessToken.substring(0, Math.min(50, accessToken.length())) + "..." : null);

            ApiResponse<Map<String, Object>> response = new ApiResponse<>("Token information", 200, tokenInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting token info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get token info: " + e.getMessage(), 500));
        }
    }

    @PostMapping("/clear-token")
    public ResponseEntity<ApiResponse<String>> clearToken() {
        try {
            oauth2ClientService.clearToken();
            ApiResponse<String> response = new ApiResponse<>("Token cleared successfully", 200, "cleared");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error clearing token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to clear token: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", System.currentTimeMillis());
        healthInfo.put("service", "OAuth2 Client App");
        healthInfo.put("using", "RestTemplate");

        ApiResponse<Map<String, Object>> response = new ApiResponse<>("Service is healthy", 200, healthInfo);
        return ResponseEntity.ok(response);
    }
}