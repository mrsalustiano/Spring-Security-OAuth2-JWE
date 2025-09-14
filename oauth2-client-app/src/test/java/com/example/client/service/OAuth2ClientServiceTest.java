package com.example.client.service;

import com.example.client.dto.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2ClientServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private OAuth2ClientService oauth2ClientService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        oauth2ClientService = new OAuth2ClientService(restTemplate);
        objectMapper = new ObjectMapper();

        // Set test properties using reflection
        ReflectionTestUtils.setField(oauth2ClientService, "serverBaseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(oauth2ClientService, "tokenEndpoint", "/auth/oauth/v2/token-jew");
        ReflectionTestUtils.setField(oauth2ClientService, "validateEndpoint", "/auth/oauth/v2/validate");
        ReflectionTestUtils.setField(oauth2ClientService, "protectedApiBase", "/api/v1/protected");
        ReflectionTestUtils.setField(oauth2ClientService, "clientId", "test-client");
        ReflectionTestUtils.setField(oauth2ClientService, "clientSecret", "test-secret");
        ReflectionTestUtils.setField(oauth2ClientService, "username", "testuser");
        ReflectionTestUtils.setField(oauth2ClientService, "password", "testpass");
        ReflectionTestUtils.setField(oauth2ClientService, "scopes", "read,write");
    }

    @Test
    void getAccessToken_ShouldReturnToken_WhenRequestIsSuccessful() {
        // Given
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("test-access-token");
        tokenResponse.setTokenType("Bearer");
        tokenResponse.setExpiresIn(3600L);
        tokenResponse.setRefreshToken("test-refresh-token");
        tokenResponse.setScope("read,write");

        ResponseEntity<TokenResponse> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(TokenResponse.class)))
                .thenReturn(responseEntity);

        // When
        String accessToken = oauth2ClientService.getAccessToken();

        // Then
        assertThat(accessToken).isEqualTo("test-access-token");
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(TokenResponse.class));
    }

    @Test
    void getAccessToken_ShouldThrowException_WhenRequestFails() {
        // Given
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(TokenResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        // When & Then
        assertThatThrownBy(() -> oauth2ClientService.getAccessToken())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to obtain access token");

        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(TokenResponse.class));
    }

    @Test
    void getAccessToken_ShouldReuseToken_WhenTokenIsStillValid() {
        // Given
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("test-access-token");
        tokenResponse.setTokenType("Bearer");
        tokenResponse.setExpiresIn(3600L);

        ResponseEntity<TokenResponse> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(TokenResponse.class)))
                .thenReturn(responseEntity);

        // When
        String firstToken = oauth2ClientService.getAccessToken();
        String secondToken = oauth2ClientService.getAccessToken();

        // Then
        assertThat(firstToken).isEqualTo(secondToken);
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(TokenResponse.class));
    }

    @Test
    void validateToken_ShouldReturnTrue_WhenTokenIsValid() {
        // Given
        Map<String, Object> validationResponse = new HashMap<>();
        validationResponse.put("valid", true);
        validationResponse.put("username", "testuser");

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(validationResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // When
        boolean isValid = oauth2ClientService.validateToken("test-token");

        // Then
        assertThat(isValid).isTrue();
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsInvalid() {
        // Given
        Map<String, Object> validationResponse = new HashMap<>();
        validationResponse.put("valid", false);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(validationResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // When
        boolean isValid = oauth2ClientService.validateToken("invalid-token");

        // Then
        assertThat(isValid).isFalse();
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenRequestFails() {
        // Given
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("Connection failed"));

        // When
        boolean isValid = oauth2ClientService.validateToken("test-token");

        // Then
        assertThat(isValid).isFalse();
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void makeAuthenticatedRequest_ShouldReturnResponse_WhenRequestIsSuccessful() {
        // Given
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("test-access-token");
        tokenResponse.setTokenType("Bearer");
        tokenResponse.setExpiresIn(3600L);

        ResponseEntity<TokenResponse> tokenResponseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("message", "success");
        apiResponse.put("data", "test-data");

        ResponseEntity<Map> apiResponseEntity = new ResponseEntity<>(apiResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(TokenResponse.class)))
                .thenReturn(tokenResponseEntity);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(apiResponseEntity);

        // When
        ResponseEntity<Map> result = oauth2ClientService.makeAuthenticatedRequest("/test", HttpMethod.GET, null, Map.class);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsEntry("message", "success");
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void makeAuthenticatedRequest_ShouldThrowException_WhenRequestFails() {
        // Given
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("test-access-token");
        tokenResponse.setTokenType("Bearer");
        tokenResponse.setExpiresIn(3600L);

        ResponseEntity<TokenResponse> tokenResponseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(TokenResponse.class)))
                .thenReturn(tokenResponseEntity);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN, "Forbidden"));

        // When & Then
        assertThatThrownBy(() -> oauth2ClientService.makeAuthenticatedRequest("/test", HttpMethod.GET, null, Map.class))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to make authenticated request");
    }

    @Test
    void getUserProfile_ShouldReturnProfile_WhenRequestIsSuccessful() {
        // Given
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("test-access-token");
        tokenResponse.setTokenType("Bearer");
        tokenResponse.setExpiresIn(3600L);

        ResponseEntity<TokenResponse> tokenResponseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        Map<String, Object> profileResponse = new HashMap<>();
        profileResponse.put("username", "testuser");
        profileResponse.put("email", "test@example.com");

        ResponseEntity<Map> profileResponseEntity = new ResponseEntity<>(profileResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(TokenResponse.class)))
                .thenReturn(tokenResponseEntity);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(profileResponseEntity);

        // When
        Map<String, Object> profile = oauth2ClientService.getUserProfile();

        // Then
        assertThat(profile).containsEntry("username", "testuser");
        assertThat(profile).containsEntry("email", "test@example.com");
    }

    @Test
    void hasValidToken_ShouldReturnFalse_WhenNoToken() {
        // When
        boolean hasValidToken = oauth2ClientService.hasValidToken();

        // Then
        assertThat(hasValidToken).isFalse();
    }

    @Test
    void clearToken_ShouldClearCurrentToken() {
        // Given
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("test-access-token");
        tokenResponse.setTokenType("Bearer");
        tokenResponse.setExpiresIn(3600L);

        ResponseEntity<TokenResponse> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(TokenResponse.class)))
                .thenReturn(responseEntity);

        // Get a token first
        oauth2ClientService.getAccessToken();

        // When
        oauth2ClientService.clearToken();

        // Then
        assertThat(oauth2ClientService.hasValidToken()).isFalse();
        assertThat(oauth2ClientService.getCurrentToken()).isNull();
    }
}