package com.example.oauth2server.service;

import com.example.oauth2server.dto.TokenRequest;
import com.example.oauth2server.dto.TokenResponse;
import com.example.oauth2server.model.AccessToken;
import com.example.oauth2server.model.Role;
import com.example.oauth2server.model.Usuario;
import com.example.oauth2server.repository.AccessTokenRepository;
import com.example.oauth2server.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2TokenServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AccessTokenRepository accessTokenRepository;

    @Mock
    private JweTokenService jweTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private OAuth2TokenService oauth2TokenService;

    @BeforeEach
    void setUp() {
        oauth2TokenService = new OAuth2TokenService(
                usuarioRepository,
                accessTokenRepository,
                jweTokenService,
                passwordEncoder
        );
    }

    @Test
    void generateToken_ShouldReturnTokenResponse_WhenCredentialsAreValid() {
        // Given
        TokenRequest request = new TokenRequest();
        request.setGrant_type("password");
        request.setUsername("testuser");
        request.setPassword("testpass");
        request.setClient_id("test-client");
        request.setClient_secret("test-secret");
        request.setScope("read,write");

        Usuario usuario = createTestUser();

        when(usuarioRepository.findByUsername("testuser")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("testpass", "hashedpassword")).thenReturn(true);
        when(jweTokenService.generateToken(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn("encrypted-access-token");
        when(jweTokenService.generateRefreshToken(anyString(), anyString()))
                .thenReturn("encrypted-refresh-token");
        when(accessTokenRepository.save(any(AccessToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TokenResponse response = oauth2TokenService.generateToken(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("encrypted-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("encrypted-refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getScope()).isEqualTo("read,write");

        verify(usuarioRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("testpass", "hashedpassword");
        verify(jweTokenService, times(1)).generateToken("testuser", "test-client", "read,write", 3600L);
        verify(accessTokenRepository, times(1)).save(any(AccessToken.class));
    }

    @Test
    void generateToken_ShouldThrowException_WhenUserNotFound() {
        // Given
        TokenRequest request = new TokenRequest();
        request.setGrant_type("password");
        request.setUsername("nonexistent");
        request.setPassword("testpass");
        request.setClient_id("test-client");
        request.setClient_secret("test-secret");
        request.setScope("read,write");

        when(usuarioRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> oauth2TokenService.generateToken(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");

        verify(usuarioRepository, times(1)).findByUsername("nonexistent");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void generateToken_ShouldThrowException_WhenPasswordIsInvalid() {
        // Given
        TokenRequest request = new TokenRequest();
        request.setGrant_type("password");
        request.setUsername("testuser");
        request.setPassword("wrongpass");
        request.setClient_id("test-client");
        request.setClient_secret("test-secret");
        request.setScope("read,write");

        Usuario usuario = createTestUser();

        when(usuarioRepository.findByUsername("testuser")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrongpass", "hashedpassword")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> oauth2TokenService.generateToken(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");

        verify(usuarioRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("wrongpass", "hashedpassword");
    }

    @Test
    void generateToken_ShouldThrowException_WhenUserIsDisabled() {
        // Given
        TokenRequest request = new TokenRequest();
        request.setGrant_type("password");
        request.setUsername("testuser");
        request.setPassword("testpass");
        request.setClient_id("test-client");
        request.setClient_secret("test-secret");
        request.setScope("read,write");

        Usuario usuario = createTestUser();
        usuario.setEnabled(false);

        when(usuarioRepository.findByUsername("testuser")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("testpass", "hashedpassword")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> oauth2TokenService.generateToken(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User account is disabled");

        verify(usuarioRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("testpass", "hashedpassword");
    }

    @Test
    void generateToken_ShouldThrowException_WhenGrantTypeIsInvalid() {
        // Given
        TokenRequest request = new TokenRequest();
        request.setGrant_type("invalid_grant");
        request.setUsername("testuser");
        request.setPassword("testpass");
        request.setClient_id("test-client");
        request.setClient_secret("test-secret");
        request.setScope("read,write");

        // When & Then
        assertThatThrownBy(() -> oauth2TokenService.generateToken(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unsupported grant type");

        verify(usuarioRepository, never()).findByUsername(anyString());
    }

    @Test
    void validateToken_ShouldReturnValidationResponse_WhenTokenIsValid() {
        // Given
        String token = "valid-token";
        String username = "testuser";

        when(jweTokenService.validateToken(token)).thenReturn(true);
        when(jweTokenService.extractUsername(token)).thenReturn(username);

        Usuario usuario = createTestUser();
        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.of(usuario));

        AccessToken accessToken = new AccessToken();
        accessToken.setTokenValue(token);
        accessToken.setRevoked(false);
        accessToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(accessTokenRepository.findByTokenValue(token)).thenReturn(Optional.of(accessToken));

        // When
        var response = oauth2TokenService.validateToken(token);

        // Then
        assertThat(response.get("valid")).isEqualTo(true);
        assertThat(response.get("username")).isEqualTo(username);
        assertThat(response.get("scope")).isNotNull();

        verify(jweTokenService, times(1)).validateToken(token);
        verify(jweTokenService, times(1)).extractUsername(token);
        verify(usuarioRepository, times(1)).findByUsername(username);
        verify(accessTokenRepository, times(1)).findByTokenValue(token);
    }

    @Test
    void validateToken_ShouldReturnInvalidResponse_WhenTokenIsInvalid() {
        // Given
        String token = "invalid-token";

        when(jweTokenService.validateToken(token)).thenReturn(false);

        // When
        var response = oauth2TokenService.validateToken(token);

        // Then
        assertThat(response.get("valid")).isEqualTo(false);
        assertThat(response.get("error")).isEqualTo("Invalid token");

        verify(jweTokenService, times(1)).validateToken(token);
        verify(jweTokenService, never()).extractUsername(anyString());
    }

    @Test
    void validateToken_ShouldReturnInvalidResponse_WhenTokenIsRevoked() {
        // Given
        String token = "revoked-token";
        String username = "testuser";

        when(jweTokenService.validateToken(token)).thenReturn(true);
        when(jweTokenService.extractUsername(token)).thenReturn(username);

        Usuario usuario = createTestUser();
        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.of(usuario));

        AccessToken accessToken = new AccessToken();
        accessToken.setTokenValue(token);
        accessToken.setRevoked(true);
        accessToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(accessTokenRepository.findByTokenValue(token)).thenReturn(Optional.of(accessToken));

        // When
        var response = oauth2TokenService.validateToken(token);

        // Then
        assertThat(response.get("valid")).isEqualTo(false);
        assertThat(response.get("error")).isEqualTo("Token has been revoked");

        verify(accessTokenRepository, times(1)).findByTokenValue(token);
    }

    @Test
    void revokeToken_ShouldRevokeToken_WhenTokenExists() {
        // Given
        String token = "valid-token";

        AccessToken accessToken = new AccessToken();
        accessToken.setTokenValue(token);
        accessToken.setRevoked(false);
        when(accessTokenRepository.findByTokenValue(token)).thenReturn(Optional.of(accessToken));
        when(accessTokenRepository.save(any(AccessToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        oauth2TokenService.revokeToken(token);

        // Then
        assertThat(accessToken.isRevoked()).isTrue();
        verify(accessTokenRepository, times(1)).findByTokenValue(token);
        verify(accessTokenRepository, times(1)).save(accessToken);
    }

    @Test
    void revokeToken_ShouldThrowException_WhenTokenNotFound() {
        // Given
        String token = "nonexistent-token";

        when(accessTokenRepository.findByTokenValue(token)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> oauth2TokenService.revokeToken(token))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Token not found");

        verify(accessTokenRepository, times(1)).findByTokenValue(token);
        verify(accessTokenRepository, never()).save(any(AccessToken.class));
    }

    private Usuario createTestUser() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("testuser");
        usuario.setPassword("hashedpassword");
        usuario.setEmail("test@example.com");
        usuario.setEnabled(true);
        usuario.setCreatedAt(LocalDateTime.now());
        usuario.setUpdatedAt(LocalDateTime.now());

        Role role = new Role();
        role.setId(1L);
        role.setName("USER");
        role.setDescription("User role");

        usuario.setRoles(Set.of(role));
        return usuario;
    }
}