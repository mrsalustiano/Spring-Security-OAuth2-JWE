package com.example.oauth2server.controller;

import com.example.oauth2server.service.JweTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiController.class)
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JweTokenService jweTokenService;

    @Test
    void getProfile_ShouldReturnProfile_WhenTokenIsValid() throws Exception {
        // Given
        String token = "Bearer valid-token";
        String username = "testuser";

        when(jweTokenService.validateToken("valid-token")).thenReturn(true);
        when(jweTokenService.extractUsername("valid-token")).thenReturn(username);

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", username);
        claims.put("scope", "read,write");
        claims.put("client_id", "test-client");
        when(jweTokenService.extractClaims("valid-token")).thenReturn(claims);

        // When & Then
        mockMvc.perform(get("/api/v1/protected/profile")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.scope").value("read,write"))
                .andExpect(jsonPath("$.client_id").value("test-client"));

        verify(jweTokenService, times(1)).validateToken("valid-token");
        verify(jweTokenService, times(1)).extractUsername("valid-token");
        verify(jweTokenService, times(1)).extractClaims("valid-token");
    }

    @Test
    void getProfile_ShouldReturnUnauthorized_WhenTokenIsInvalid() throws Exception {
        // Given
        String token = "Bearer invalid-token";

        when(jweTokenService.validateToken("invalid-token")).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/v1/protected/profile")
                        .header("Authorization", token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("invalid_token"))
                .andExpect(jsonPath("$.error_description").value("Invalid or expired token"));

        verify(jweTokenService, times(1)).validateToken("invalid-token");
        verify(jweTokenService, never()).extractUsername(anyString());
    }

    @Test
    void getProfile_ShouldReturnUnauthorized_WhenAuthorizationHeaderIsMissing() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/protected/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("invalid_token"))
                .andExpect(jsonPath("$.error_description").value("Authorization header is required"));

        verify(jweTokenService, never()).validateToken(anyString());
    }

    @Test
    void getProfile_ShouldReturnUnauthorized_WhenAuthorizationHeaderIsInvalid() throws Exception {
        // Given
        String invalidHeader = "InvalidHeader";

        // When & Then
        mockMvc.perform(get("/api/v1/protected/profile")
                        .header("Authorization", invalidHeader))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("invalid_token"))
                .andExpect(jsonPath("$.error_description").value("Invalid authorization header format"));

        verify(jweTokenService, never()).validateToken(anyString());
    }

    @Test
    void getData_ShouldReturnData_WhenTokenIsValid() throws Exception {
        // Given
        String token = "Bearer valid-token";
        String username = "testuser";

        when(jweTokenService.validateToken("valid-token")).thenReturn(true);
        when(jweTokenService.extractUsername("valid-token")).thenReturn(username);

        // When & Then
        mockMvc.perform(get("/api/v1/protected/data")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Protected data accessed successfully"))
                .andExpect(jsonPath("$.user").value(username))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").exists());

        verify(jweTokenService, times(1)).validateToken("valid-token");
        verify(jweTokenService, times(1)).extractUsername("valid-token");
    }

    @Test
    void createData_ShouldCreateData_WhenTokenIsValid() throws Exception {
        // Given
        String token = "Bearer valid-token";
        String username = "testuser";
        String requestBody = "{\"name\":\"Test Item\",\"value\":\"Test Value\"}";

        when(jweTokenService.validateToken("valid-token")).thenReturn(true);
        when(jweTokenService.extractUsername("valid-token")).thenReturn(username);

        // When & Then
        mockMvc.perform(post("/api/v1/protected/data")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Data created successfully"))
                .andExpect(jsonPath("$.user").value(username))
                .andExpect(jsonPath("$.created_data.name").value("Test Item"))
                .andExpect(jsonPath("$.created_data.value").value("Test Value"));

        verify(jweTokenService, times(1)).validateToken("valid-token");
        verify(jweTokenService, times(1)).extractUsername("valid-token");
    }

    @Test
    void createData_ShouldReturnUnauthorized_WhenTokenIsInvalid() throws Exception {
        // Given
        String token = "Bearer invalid-token";
        String requestBody = "{\"name\":\"Test Item\",\"value\":\"Test Value\"}";

        when(jweTokenService.validateToken("invalid-token")).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/protected/data")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("invalid_token"))
                .andExpect(jsonPath("$.error_description").value("Invalid or expired token"));

        verify(jweTokenService, times(1)).validateToken("invalid-token");
        verify(jweTokenService, never()).extractUsername(anyString());
    }
}