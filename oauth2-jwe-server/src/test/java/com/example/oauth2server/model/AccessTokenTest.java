package com.example.oauth2server.model;

import com.example.oauth2.model.AccessToken;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenTest {

    @Test
    void accessToken_ShouldBeCreatedWithCorrectProperties() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(1);

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("testuser");

        // When
        AccessToken token = new AccessToken();
        token.setId(1L);
        token.setTokenValue("encrypted-token-value");
        token.setTokenType("Bearer");
        token.setExpiresAt(expiresAt);
        token.setScope("read,write");
        token.setClientId("test-client");
        token.setUsuario(usuario);
        token.setCreatedAt(now);
        token.setRevoked(false);

        // Then
        assertThat(token.getId()).isEqualTo(1L);
        assertThat(token.getTokenValue()).isEqualTo("encrypted-token-value");
        assertThat(token.getTokenType()).isEqualTo("Bearer");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(token.getScope()).isEqualTo("read,write");
        assertThat(token.getClientId()).isEqualTo("test-client");
        assertThat(token.getUsuario()).isEqualTo(usuario);
        assertThat(token.getCreatedAt()).isEqualTo(now);
        assertThat(token.isRevoked()).isFalse();
    }

    @Test
    void accessToken_ShouldCheckIfExpired() {
        // Given
        AccessToken expiredToken = new AccessToken();
        expiredToken.setExpiresAt(LocalDateTime.now().minusHours(1));

        AccessToken validToken = new AccessToken();
        validToken.setExpiresAt(LocalDateTime.now().plusHours(1));

        // Then
        assertThat(expiredToken.isExpired()).isTrue();
        assertThat(validToken.isExpired()).isFalse();
    }

    @Test
    void accessToken_ShouldCheckIfActive() {
        // Given
        AccessToken activeToken = new AccessToken();
        activeToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        activeToken.setRevoked(false);

        AccessToken revokedToken = new AccessToken();
        revokedToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        revokedToken.setRevoked(true);

        AccessToken expiredToken = new AccessToken();
        expiredToken.setExpiresAt(LocalDateTime.now().minusHours(1));
        expiredToken.setRevoked(false);

        // Then
        assertThat(activeToken.isActive()).isTrue();
        assertThat(revokedToken.isActive()).isFalse();
        assertThat(expiredToken.isActive()).isFalse();
    }
}