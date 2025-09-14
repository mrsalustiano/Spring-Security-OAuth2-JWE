package com.example.client.integration;

import com.example.client.dto.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "oauth2.server.base-url=http://localhost:8089",
        "oauth2.client.client-id=test-client",
        "oauth2.client.client-secret=test-secret",
        "oauth2.client.username=testuser",
        "oauth2.client.password=testpass"
})
class OAuth2ClientIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void hello_ShouldReturnAuthenticatedResponse_WhenOAuth2ServerRespondsCorrectly() throws Exception {
        // Given - Mock OAuth2 server responses
        stubFor(post(urlEqualTo("/auth/oauth/v2/token-jew"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "access_token": "test-access-token-12345",
                                    "token_type": "Bearer",
                                    "expires_in": 3600,
                                    "refresh_token": "test-refresh-token",
                                    "scope": "read,write"
                                }
                                """)));

        stubFor(post(urlMatching("/auth/oauth/v2/validate.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "valid": true,
                                    "username": "testuser",
                                    "scope": "read,write"
                                }
                                """)));

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/hello", String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ApiResponse<String> apiResponse = objectMapper.readValue(response.getBody(),
                new TypeReference<ApiResponse<String>>() {});

        assertThat(apiResponse.getMessage()).isEqualTo("autenticado");
        assertThat(apiResponse.getStatus()).isEqualTo(200);
        assertThat(apiResponse.getPayload()).isEqualTo("hello");

        // Verify interactions with OAuth2 server
        verify(postRequestedFor(urlEqualTo("/auth/oauth/v2/token-jew")));
        verify(postRequestedFor(urlMatching("/auth/oauth/v2/validate.*")));
    }

    @Test
    void hello_ShouldReturnError_WhenOAuth2ServerReturnsInvalidToken() throws Exception {
        // Given - Mock OAuth2 server to return invalid token
        stubFor(post(urlEqualTo("/auth/oauth/v2/token-jew"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "access_token": "invalid-token",
                                    "token_type": "Bearer",
                                    "expires_in": 3600,
                                    "refresh_token": "test-refresh-token",
                                    "scope": "read,write"
                                }
                                """)));

        stubFor(post(urlMatching("/auth/oauth/v2/validate.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "valid": false,
                                    "error": "Invalid token"
                                }
                                """)));

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/hello", String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ApiResponse<String> apiResponse = objectMapper.readValue(response.getBody(),
                new TypeReference<ApiResponse<String>>() {});

        assertThat(apiResponse.getMessage()).isEqualTo("Token validation failed");
        assertThat(apiResponse.getStatus()).isEqualTo(401);
    }

    @Test
    void hello_ShouldReturnError_WhenOAuth2ServerIsDown() {
        // Given - Stop the mock server to simulate server down
        wireMockServer.stop();

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/hello", String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void tokenInfo_ShouldReturnTokenInformation() throws Exception {
        // Given - Mock OAuth2 server
        stubFor(post(urlEqualTo("/auth/oauth/v2/token-jew"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "access_token": "test-access-token-12345",
                                    "token_type": "Bearer",
                                    "expires_in": 3600,
                                    "refresh_token": "test-refresh-token",
                                    "scope": "read,write"
                                }
                                """)));

        // First, get a token by calling hello endpoint
        restTemplate.getForEntity("http://localhost:" + port + "/api/hello", String.class);

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/token-info", String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ApiResponse<Map<String, Object>> apiResponse = objectMapper.readValue(response.getBody(),
                new TypeReference<ApiResponse<Map<String, Object>>>() {});

        assertThat(apiResponse.getMessage()).isEqualTo("Token information");
        assertThat(apiResponse.getStatus()).isEqualTo(200);
        assertThat((Boolean) apiResponse.getPayload().get("has_token")).isTrue();
        assertThat((Boolean) apiResponse.getPayload().get("is_valid")).isTrue();
        assertThat((Integer) apiResponse.getPayload().get("token_length")).isGreaterThan(0);
    }

    @Test
    void health_ShouldReturnHealthStatus() throws Exception {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/health", String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ApiResponse<Map<String, Object>> apiResponse = objectMapper.readValue(response.getBody(),
                new TypeReference<ApiResponse<Map<String, Object>>>() {});

        assertThat(apiResponse.getMessage()).isEqualTo("Service is healthy");
        assertThat(apiResponse.getStatus()).isEqualTo(200);
        assertThat(apiResponse.getPayload().get("status")).isEqualTo("UP");
        assertThat(apiResponse.getPayload().get("service")).isEqualTo("OAuth2 Client App");
        assertThat(apiResponse.getPayload().get("using")).isEqualTo("RestTemplate");
    }
}