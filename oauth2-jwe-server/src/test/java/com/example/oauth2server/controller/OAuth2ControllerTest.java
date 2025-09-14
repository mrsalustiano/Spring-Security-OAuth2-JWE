package com.example.oauth2server.controller;

import com.example.oauth2server.dto.TokenRequest;
import com.example.oauth2server.dto.TokenResponse;
import com.example.oauth2server.service.OAuth2TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OAuth2Controller.class)
class OAuth2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OAuth2TokenService oauth2TokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void generateToken_ShouldReturnTokenResponse_WhenRequestIsValid() throws Exception {
        // Given
        TokenRequest request = new TokenRequest();
        request.setGrant_type("password");
        request.setUsername("testuser");
        request.setPassword("testpass");
        request.setClient_id("test-client");
        request.setClient_secret("test-secret");
        request.setScope("read,write");

        TokenResponse response = new TokenResponse();
        response.setAccessToken("encrypted-access-token");
        response.setRefreshToken("encrypted-refresh-token");
        response.setTokenType("Bearer");
        response.setExpiresIn(3600L);
        response.setScope("read,write");

        when(oauth2TokenService.generateToken(any(TokenRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/auth/oauth/v2/token-jew")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.access_token").value("encrypted-access-token"))
                .andExpect(jsonPath("$.refresh_token").value("encrypted-refresh-token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600))
                .andExpect(jsonPath("$.scope").value("read,write"));

        verify(oauth2TokenService, times(1)).generateToken(any(TokenRequest.class));
    }

    @Test
    void generateToken_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        // Given
        TokenRequest request = new TokenRequest();
        // Missing required fields

        when(oauth2TokenService.generateToken(any(TokenRequest.class)))
                .thenThrow(new RuntimeException("Invalid request"));

        // When & Then
        mockMvc.perform(post("/auth/oauth/v2/token-jew")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description").value("Invalid request"));

        verify(oauth2TokenService, times(1)).generateToken(any(TokenRequest.class));
    }

    @Test
    void generateToken_ShouldReturnUnauthorized_WhenCredentialsAreInvalid() throws Exception {
        // Given
        TokenRequest request = new TokenRequest();
        request.setGrant_type("password");
        request.setUsername("testuser");
        request.setPassword("wrongpass");
        request.setClient_id("test-client");
        request.setClient_secret("test-secret");
        request.setScope("read,write");

        when(oauth2TokenService.generateToken(any(TokenRequest.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        // When & Then
        mockMvc.perform(post("/auth/oauth/v2/token-jew")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description").value("Invalid credentials"));

        verify(oauth2TokenService, times(1)).generateToken(any(TokenRequest.class));
    }

    @Test
    void validateToken_ShouldReturnValidResponse_WhenTokenIsValid() throws Exception {
        // Given
        String token = "valid-token";

        Map<String, Object> validationResponse = new HashMap<>();
        validationResponse.put("valid", true);
        validationResponse.put("username", "testuser");
        validationResponse.put("scope", "read,write");

        when(oauth2TokenService.validateToken(token)).thenReturn(validationResponse);

        // When & Then
        mockMvc.perform(post("/auth/oauth/v2/validate")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.scope").value("read,write"));

        verify(oauth2TokenService, times(1)).validateToken(token);
    }

    @Test
    void validateToken_ShouldReturnInvalidResponse_WhenTokenIsInvalid() throws Exception {
        // Given
        String token = "invalid-token";

        Map<String, Object> validationResponse = new HashMap<>();
        validationResponse.put("valid", false);
        validationResponse.put("error", "Invalid token");

        when(oauth2TokenService.validateToken(token)).thenReturn(validationResponse);

        // When & Then
        mockMvc.perform(post("/auth/oauth/v2/validate")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.error").value("Invalid token"));

        verify(oauth2TokenService, times(1)).validateToken(token);
    }

    @Test
    void validateToken_ShouldReturnBadRequest_WhenTokenParameterIsMissing() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/oauth/v2/validate"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description").value("Token parameter is required"));

        verify(oauth2TokenService, never()).validateToken(anyString());
    }

    @Test
    void revokeToken_ShouldReturnSuccess_WhenTokenIsRevoked() throws Exception {
        // Given
        String token = "valid-token";

        doNothing().when(oauth2TokenService).revokeToken(token);

        // When & Then
        mockMvc.perform(post("/auth/oauth/v2/revoke")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.revoked").value(true))
                .andExpect(jsonPath("$.message").value("Token revoked successfully"));

        verify(oauth2TokenService, times(1)).revokeToken(token);
    }

    @Test
    void revokeToken_ShouldReturnBadRequest_WhenTokenNotFound() throws Exception {
        // Given
        String token = "nonexistent-token";

        doThrow(new RuntimeException("Token not found")).when(oauth2TokenService).revokeToken(token);

        // When & Then
        mockMvc.perform(post("/auth/oauth/v2/revoke")
                        .param("token", token))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description").value("Token not found"));

        verify(oauth2TokenService, times(1)).revokeToken(token);
    }

    @Test
    void revokeToken_ShouldReturnBadRequest_WhenTokenParameterIsMissing() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/oauth/v2/revoke"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description").value("Token parameter is required"));

        verify(oauth2TokenService, never()).revokeToken(anyString());
    }
}