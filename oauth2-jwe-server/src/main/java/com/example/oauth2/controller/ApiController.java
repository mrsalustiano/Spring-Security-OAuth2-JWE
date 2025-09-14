package com.example.oauth2.controller;

import com.example.oauth2.service.JweTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    private JweTokenService jweTokenService;

    @GetMapping("/public/health")
    public ResponseEntity<?> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("service", "OAuth2 JWE Server");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/protected/profile")
    @PreAuthorize("hasAuthority('SCOPE_read')")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractTokenFromHeader(authHeader);

            String username = jweTokenService.extractUsername(token);
            Long userId = jweTokenService.extractUserId(token);
            List<String> roles = jweTokenService.extractRoles(token);
            List<String> scopes = jweTokenService.extractScopes(token);

            Map<String, Object> profile = new HashMap<>();
            profile.put("user_id", userId);
            profile.put("username", username);
            profile.put("roles", roles);
            profile.put("scopes", scopes);

            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            logger.error("Error getting profile", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "invalid_token", "message", "Invalid or expired token"));
        }
    }

    @GetMapping("/protected/data")
    @PreAuthorize("hasAuthority('SCOPE_read')")
    public ResponseEntity<?> getData() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "This is protected data");
        data.put("timestamp", System.currentTimeMillis());
        data.put("data", List.of("item1", "item2", "item3"));

        return ResponseEntity.ok(data);
    }

    @PostMapping("/protected/data")
    @PreAuthorize("hasAuthority('SCOPE_write')")
    public ResponseEntity<?> createData(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Data created successfully");
        response.put("timestamp", System.currentTimeMillis());
        response.put("created_data", requestData);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsers() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin endpoint - users list");
        response.put("users", List.of(
                Map.of("id", 1, "username", "admin", "role", "ADMIN"),
                Map.of("id", 2, "username", "user", "role", "USER")
        ));

        return ResponseEntity.ok(response);
    }

    private String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid authorization header");
    }
}