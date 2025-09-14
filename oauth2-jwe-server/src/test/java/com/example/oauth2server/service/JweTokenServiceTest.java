package com.example.oauth2server.service;

import com.example.oauth2.service.JweTokenService;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JweTokenServiceTest {

    private JweTokenService jweTokenService;

    @BeforeEach
    void setUp() {
        jweTokenService = new JweTokenService();
        ReflectionTestUtils.setField(jweTokenService, "encryptionKey", "test-encryption-key-32-characters");
        ReflectionTestUtils.setField(jweTokenService, "signingKey", "test-signing-key-must-be-32-chars");
        jweTokenService.init();
    }

    @Test
    void generateToken_ShouldCreateValidJWEToken() {
        // Given
        String username = "testuser";
        String clientId = "test-client";
        String scope = "read,write";
        long expiresIn = 3600;

        // When
        String token = jweTokenService.generateToken(username, clientId, scope, expiresIn);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(5); // JWE has 5 parts
    }

    @Test
    void validateToken_ShouldReturnTrue_ForValidToken() {
        // Given
        String username = "testuser";
        String clientId = "test-client";
        String scope = "read,write";
        long expiresIn = 3600;

        String token = jweTokenService.generateToken(username, clientId, scope, expiresIn);

        // When
        boolean isValid = jweTokenService.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_ShouldReturnFalse_ForInvalidToken() {
        // Given
        String invalidToken = "invalid.token.value";

        // When
        boolean isValid = jweTokenService.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_ShouldReturnFalse_ForExpiredToken() {
        // Given
        String username = "testuser";
        String clientId = "test-client";
        String scope = "read,write";
        long expiresIn = -1; // Expired token

        String token = jweTokenService.generateToken(username, clientId, scope, expiresIn);

        // When
        boolean isValid = jweTokenService.validateToken(token);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void extractClaims_ShouldReturnCorrectClaims_ForValidToken() {
        // Given
        String username = "testuser";
        String clientId = "test-client";
        String scope = "read,write";
        long expiresIn = 3600;

        String token = jweTokenService.generateToken(username, clientId, scope, expiresIn);

        // When
        Map<String, Object> claims = jweTokenService.extractClaims(token);

        // Then
        assertThat(claims).isNotNull();
        assertThat(claims.get("sub")).isEqualTo(username);
        assertThat(claims.get("client_id")).isEqualTo(clientId);
        assertThat(claims.get("scope")).isEqualTo(scope);
        assertThat(claims.get("exp")).isNotNull();
        assertThat(claims.get("iat")).isNotNull();
    }

    @Test
    void extractClaims_ShouldReturnNull_ForInvalidToken() {
        // Given
        String invalidToken = "invalid.token.value";

        // When
        Map<String, Object> claims = jweTokenService.extractClaims(invalidToken);

        // Then
        assertThat(claims).isNull();
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsername_ForValidToken() {
        // Given
        String username = "testuser";
        String clientId = "test-client";
        String scope = "read,write";
        long expiresIn = 3600;

        String token = jweTokenService.generateToken(username, clientId, scope, expiresIn);

        // When
        String extractedUsername = jweTokenService.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    void extractUsername_ShouldReturnNull_ForInvalidToken() {
        // Given
        String invalidToken = "invalid.token.value";

        // When
        String extractedUsername = jweTokenService.extractUsername(invalidToken);

        // Then
        assertThat(extractedUsername).isNull();
    }

    @Test
    void generateRefreshToken_ShouldCreateValidToken() {
        // Given
        String username = "testuser";
        String clientId = "test-client";

        // When
        String refreshToken = jweTokenService.generateRefreshToken(username, clientId);

        // Then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        assertThat(refreshToken.split("\\.")).hasSize(5); // JWE has 5 parts
    }

    @Test
    void validateRefreshToken_ShouldReturnTrue_ForValidRefreshToken() {
        // Given
        String username = "testuser";
        String clientId = "test-client";

        String refreshToken = jweTokenService.generateRefreshToken(username, clientId);

        // When
        boolean isValid = jweTokenService.validateRefreshToken(refreshToken);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void init_ShouldThrowException_WhenKeysAreTooShort() {
        // Given
        JweTokenService service = new JweTokenService();
        ReflectionTestUtils.setField(service, "encryptionKey", "short");
        ReflectionTestUtils.setField(service, "signingKey", "short");

        // When & Then
        assertThatThrownBy(service::init)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("key must be at least 32 characters");
    }
}