package com.example.client.controller;

import com.example.client.dto.ApiResponse;
import com.example.client.service.OAuth2ClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OAuth2ClientService oauth2ClientService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void hello_ShouldReturnAuthenticatedResponse_WhenTokenIsValid() throws Exception {
        // Given
        when(oauth2ClientService.getAccessToken()).thenReturn("test-access-token");
        when(oauth2ClientService.validateToken("test-access-token")).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("autenticado"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.payload").value("hello"));

        verify(oauth2ClientService, times(1)).getAccessToken();
        verify(oauth2ClientService, times(1)).validateToken("test-access-token");
    }

    @Test
    void hello_ShouldReturnError_WhenTokenValidationFails() throws Exception {
        // Given
        when(oauth2ClientService.getAccessToken()).thenReturn("invalid-token");
        when(oauth2ClientService.validateToken("invalid-token")).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Token validation failed"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.payload").doesNotExist());

        verify(oauth2ClientService, times(1)).getAccessToken();
        verify(oauth2ClientService, times(1)).validateToken("invalid-token");
    }

    @Test
    void hello_ShouldReturnError_WhenExceptionOccurs() throws Exception {
        // Given
        when(oauth2ClientService.getAccessToken()).thenThrow(new RuntimeException("Token request failed"));

        // When & Then
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Authentication failed: Token request failed"))
                .andExpect(jsonPath("$.status").value(500));

        verify(oauth2ClientService, times(1)).getAccessToken();
    }

    @Test
    void getProfile_ShouldReturnProfile_WhenRequestIsSuccessful() throws Exception {
        // Given
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("username", "testuser");
        profileData.put("email", "test@example.com");

        when(oauth2ClientService.getUserProfile()).thenReturn(profileData);

        // When & Then
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("autenticado"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.payload.username").value("testuser"))
                .andExpect(jsonPath("$.payload.email").value("test@example.com"));

        verify(oauth2ClientService, times(1)).getUserProfile();
    }

    @Test
    void getData_ShouldReturnData_WhenRequestIsSuccessful() throws Exception {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        data.put("count", 42);

        when(oauth2ClientService.getProtectedData()).thenReturn(data);

        // When & Then
        mockMvc.perform(get("/api/data"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("autenticado"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.payload.key").value("value"))
                .andExpect(jsonPath("$.payload.count").value(42));

        verify(oauth2ClientService, times(1)).getProtectedData();
    }

    @Test
    void createData_ShouldReturnCreatedData_WhenRequestIsSuccessful() throws Exception {
        // Given
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("name", "Test Item");
        requestData.put("value", "Test Value");

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("id", 1);
        responseData.put("name", "Test Item");
        responseData.put("value", "Test Value");

        org.springframework.http.ResponseEntity<Map> mockResponse =
                new org.springframework.http.ResponseEntity<>(responseData, org.springframework.http.HttpStatus.OK);

        when(oauth2ClientService.makeAuthenticatedRequest(eq("/data"), eq(org.springframework.http.HttpMethod.POST),
                any(), eq(Map.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/create-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestData)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("autenticado"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.payload.id").value(1))
                .andExpect(jsonPath("$.payload.name").value("Test Item"));

        verify(oauth2ClientService, times(1)).makeAuthenticatedRequest(eq("/data"),
                eq(org.springframework.http.HttpMethod.POST),
                any(), eq(Map.class));
    }

    @Test
    void getTokenInfo_ShouldReturnTokenInfo_WhenTokenExists() throws Exception {
        // Given
        when(oauth2ClientService.hasValidToken()).thenReturn(true);
        when(oauth2ClientService.getAccessToken()).thenReturn("test-access-token-with-long-value");

        // When & Then
        mockMvc.perform(get("/api/token-info"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Token information"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.payload.has_token").value(true))
                .andExpect(jsonPath("$.payload.is_valid").value(true))
                .andExpect(jsonPath("$.payload.token_length").value(32));

        verify(oauth2ClientService, times(1)).hasValidToken();
        verify(oauth2ClientService, times(1)).getAccessToken();
    }

    @Test
    void getTokenInfo_ShouldReturnNoTokenInfo_WhenNoToken() throws Exception {
        // Given
        when(oauth2ClientService.hasValidToken()).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/token-info"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Token information"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.payload.has_token").value(false))
                .andExpect(jsonPath("$.payload.is_valid").value(false))
                .andExpect(jsonPath("$.payload.token_length").value(0));

        verify(oauth2ClientService, times(1)).hasValidToken();
        verify(oauth2ClientService, never()).getAccessToken();
    }

    @Test
    void clearToken_ShouldClearToken_WhenCalled() throws Exception {
        // Given
        doNothing().when(oauth2ClientService).clearToken();

        // When & Then
        mockMvc.perform(post("/api/clear-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Token cleared successfully"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.payload").value("cleared"));

        verify(oauth2ClientService, times(1)).clearToken();
    }

    @Test
    void health_ShouldReturnHealthInfo() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Service is healthy"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.payload.status").value("UP"))
                .andExpect(jsonPath("$.payload.service").value("OAuth2 Client App"))
                .andExpect(jsonPath("$.payload.using").value("RestTemplate"))
                .andExpect(jsonPath("$.payload.timestamp").exists());
    }
}