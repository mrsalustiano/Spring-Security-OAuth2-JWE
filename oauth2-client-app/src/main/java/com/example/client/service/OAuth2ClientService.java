package com.example.client.service;

import com.example.client.dto.TokenRequest;
import com.example.client.dto.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class OAuth2ClientService {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2ClientService.class);

    @Value("${oauth2.server.base-url}")
    private String serverBaseUrl;

    @Value("${oauth2.server.token-endpoint}")
    private String tokenEndpoint;

    @Value("${oauth2.server.validate-endpoint}")
    private String validateEndpoint;

    @Value("${oauth2.server.protected-api-base}")
    private String protectedApiBase;

    @Value("${oauth2.client.client-id}")
    private String clientId;

    @Value("${oauth2.client.client-secret}")
    private String clientSecret;

    @Value("${oauth2.client.username}")
    private String username;

    @Value("${oauth2.client.password}")
    private String password;

    @Value("${oauth2.client.scopes}")
    private String scopes;

    private final RestTemplate restTemplate;
    private TokenResponse currentToken;
    private LocalDateTime tokenExpiryTime;

    public OAuth2ClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getAccessToken() {
        if (currentToken == null || isTokenExpired()) {
            logger.info("Token is null or expired, requesting new token");
            requestNewToken();
        }
        return currentToken.getAccessToken();
    }

    private void requestNewToken() {
        try {
            String url = serverBaseUrl + tokenEndpoint;

            TokenRequest request = new TokenRequest(
                    "password",
                    username,
                    password,
                    clientId,
                    clientSecret,
                    scopes
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            HttpEntity<TokenRequest> entity = new HttpEntity<>(request, headers);

            logger.info("Requesting token from: {}", url);
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(url, entity, TokenResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                currentToken = response.getBody();
                tokenExpiryTime = LocalDateTime.now().plusSeconds(currentToken.getExpiresIn() - 60); // 1 minute buffer
                logger.info("Token obtained successfully, expires in {} seconds", currentToken.getExpiresIn());
            } else {
                throw new RuntimeException("Failed to obtain token: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            logger.error("HTTP error requesting token: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to obtain access token: " + e.getMessage(), e);
        } catch (RestClientException e) {
            logger.error("Rest client error requesting token", e);
            throw new RuntimeException("Failed to obtain access token: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error requesting token", e);
            throw new RuntimeException("Failed to obtain access token", e);
        }
    }

    private boolean isTokenExpired() {
        return tokenExpiryTime == null || LocalDateTime.now().isAfter(tokenExpiryTime);
    }

    public <T> ResponseEntity<T> makeAuthenticatedRequest(String endpoint, HttpMethod method,
                                                          Object requestBody, Class<T> responseType) {
        try {
            String accessToken = getAccessToken();
            String url = serverBaseUrl + protectedApiBase + endpoint;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            HttpEntity<?> entity = new HttpEntity<>(requestBody, headers);

            logger.info("Making authenticated {} request to: {}", method, url);
            return restTemplate.exchange(url, method, entity, responseType);

        } catch (HttpClientErrorException e) {
            logger.error("HTTP error making authenticated request to {}: {} - {}",
                    endpoint, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to make authenticated request: " + e.getMessage(), e);
        } catch (RestClientException e) {
            logger.error("Rest client error making authenticated request to {}", endpoint, e);
            throw new RuntimeException("Failed to make authenticated request: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error making authenticated request to {}", endpoint, e);
            throw new RuntimeException("Failed to make authenticated request", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            String url = serverBaseUrl + validateEndpoint + "?token=" + token;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Boolean valid = (Boolean) body.get("valid");
                return Boolean.TRUE.equals(valid);
            }

            return false;

        } catch (HttpClientErrorException e) {
            logger.error("HTTP error validating token: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (RestClientException e) {
            logger.error("Rest client error validating token", e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error validating token", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserProfile() {
        try {
            ResponseEntity<Map> response = makeAuthenticatedRequest("/profile", HttpMethod.GET, null, Map.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error getting user profile", e);
            throw new RuntimeException("Failed to get user profile: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getProtectedData() {
        try {
            ResponseEntity<Map> response = makeAuthenticatedRequest("/data", HttpMethod.GET, null, Map.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error getting protected data", e);
            throw new RuntimeException("Failed to get protected data: " + e.getMessage(), e);
        }
    }

    public TokenResponse getCurrentToken() {
        return currentToken;
    }

    public boolean hasValidToken() {
        return currentToken != null && !isTokenExpired();
    }

    public void clearToken() {
        currentToken = null;
        tokenExpiryTime = null;
        logger.info("Token cleared");
    }
}