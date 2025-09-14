package com.example.oauth2.controller;

import com.example.oauth2.dto.TokenRequest;
import com.example.oauth2.dto.TokenResponse;
import com.example.oauth2.service.OAuth2TokenService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth/oauth/v2")
public class OAuth2Controller {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2Controller.class);

    @Autowired
    private OAuth2TokenService tokenService;

    @PostMapping("/token-jew")
    public ResponseEntity<?> generateToken(@Valid @RequestBody TokenRequest request) {
        try {
            logger.info("Token request received for grant_type: {}, username: {}",
                    request.getGrant_type(), request.getUsername());

            TokenResponse response = tokenService.generateToken(request);

            logger.info("Token generated successfully for grant_type: {}", request.getGrant_type());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid token request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("invalid_request", e.getMessage()));

        } catch (Exception e) {
            logger.error("Error generating token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("server_error", "Internal server error"));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestParam("token") String token) {
        try {
            boolean isValid = tokenService.validateToken(token);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);

            if (isValid) {
                response.put("message", "Token is valid");
            } else {
                response.put("message", "Token is invalid or expired");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error validating token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("server_error", "Internal server error"));
        }
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revokeToken(@RequestParam("token_id") String tokenId) {
        try {
            tokenService.revokeToken(tokenId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Token revoked successfully");
            response.put("token_id", tokenId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error revoking token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("server_error", "Internal server error"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam("user_id") Long userId) {
        try {
            tokenService.revokeAllUserTokens(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User logged out successfully");
            response.put("user_id", userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during logout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("server_error", "Internal server error"));
        }
    }

    private Map<String, String> createErrorResponse(String error, String description) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("error_description", description);
        return errorResponse;
    }
}