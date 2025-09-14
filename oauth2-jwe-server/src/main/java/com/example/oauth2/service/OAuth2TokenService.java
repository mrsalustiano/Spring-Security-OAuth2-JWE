package com.example.oauth2.service;

import com.example.oauth2.dto.TokenRequest;
import com.example.oauth2.dto.TokenResponse;
import com.example.oauth2.model.AccessToken;
import com.example.oauth2.model.Role;
import com.example.oauth2.model.Usuario;
import com.example.oauth2.repository.AccessTokenRepository;
import com.example.oauth2.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OAuth2TokenService {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2TokenService.class);

    @Autowired
    private JweTokenService jweTokenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AccessTokenRepository accessTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final long ACCESS_TOKEN_VALIDITY_HOURS = 1;
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 30;

    public TokenResponse generateToken(TokenRequest request) {
        logger.info("Generating token for grant_type: {}", request.getGrant_type());

        switch (request.getGrant_type().toLowerCase()) {
            case "password":
                return handlePasswordGrant(request);
            case "refresh_token":
                return handleRefreshTokenGrant(request);
            case "client_credentials":
                return handleClientCredentialsGrant(request);
            default:
                throw new IllegalArgumentException("Unsupported grant type: " + request.getGrant_type());
        }
    }

    private TokenResponse handlePasswordGrant(TokenRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Username and password are required for password grant");
        }

        // Validate user credentials
        Optional<Usuario> userOpt = usuarioRepository.findByLogin(request.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        Usuario user = userOpt.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getSenha())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (!user.getAtivo()) {
            throw new IllegalArgumentException("User account is disabled");
        }

        return createTokenResponse(user, request.getClient_id(), request.getScope());
    }

    private TokenResponse handleRefreshTokenGrant(TokenRequest request) {
        if (request.getRefresh_token() == null) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        // Find the access token by refresh token
        Optional<AccessToken> tokenOpt = accessTokenRepository.findByRefreshToken(request.getRefresh_token());
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        AccessToken existingToken = tokenOpt.get();
        if (existingToken.getRevoked()) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        // Get user
        Optional<Usuario> userOpt = usuarioRepository.findById(existingToken.getUsuarioId());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        Usuario user = userOpt.get();
        if (!user.getAtivo()) {
            throw new IllegalArgumentException("User account is disabled");
        }

        // Revoke the old token
        accessTokenRepository.revokeToken(existingToken.getTokenId());

        // Create new token
        return createTokenResponse(user, existingToken.getClientId(), existingToken.getScopes());
    }

    private TokenResponse handleClientCredentialsGrant(TokenRequest request) {
        if (request.getClient_id() == null || request.getClient_secret() == null) {
            throw new IllegalArgumentException("Client credentials are required");
        }

        // For simplicity, we'll create a system user token
        // In a real implementation, you'd validate client credentials against a client store
        if (!"oauth2-client".equals(request.getClient_id()) && !"api-client".equals(request.getClient_id())) {
            throw new IllegalArgumentException("Invalid client credentials");
        }

        // Create a system token without a specific user
        String tokenId = UUID.randomUUID().toString();
        String refreshToken = jweTokenService.generateRefreshToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(ACCESS_TOKEN_VALIDITY_HOURS);

        List<String> scopes = parseScopes(request.getScope());
        List<String> roles = List.of("API_CLIENT");

        String jweToken = jweTokenService.generateJweToken(
                null, // No specific user for client credentials
                request.getClient_id(),
                roles,
                request.getClient_id(),
                scopes,
                expiresAt
        );

        // Save token to database
        AccessToken accessToken = new AccessToken(
                tokenId,
                jweToken,
                refreshToken,
                null, // No user ID for client credentials
                request.getClient_id(),
                String.join(",", scopes),
                expiresAt
        );
        accessTokenRepository.save(accessToken);

        return new TokenResponse(
                jweToken,
                "Bearer",
                ACCESS_TOKEN_VALIDITY_HOURS * 3600,
                refreshToken,
                String.join(" ", scopes)
        );
    }

    private TokenResponse createTokenResponse(Usuario user, String clientId, String scopeString) {
        String tokenId = UUID.randomUUID().toString();
        String refreshToken = jweTokenService.generateRefreshToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(ACCESS_TOKEN_VALIDITY_HOURS);

        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());

        List<String> scopes = parseScopes(scopeString);

        String jweToken = jweTokenService.generateJweToken(
                user.getId(),
                user.getLogin(),
                roles,
                clientId != null ? clientId : "default-client",
                scopes,
                expiresAt
        );

        // Save token to database
        AccessToken accessToken = new AccessToken(
                tokenId,
                jweToken,
                refreshToken,
                user.getId(),
                clientId != null ? clientId : "default-client",
                String.join(",", scopes),
                expiresAt
        );
        accessTokenRepository.save(accessToken);

        logger.info("Token generated successfully for user: {}", user.getLogin());

        return new TokenResponse(
                jweToken,
                "Bearer",
                ACCESS_TOKEN_VALIDITY_HOURS * 3600,
                refreshToken,
                String.join(" ", scopes)
        );
    }

    private List<String> parseScopes(String scopeString) {
        if (scopeString == null || scopeString.trim().isEmpty()) {
            return List.of("read");
        }
        return Arrays.stream(scopeString.split("[\\s,]+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public boolean validateToken(String token) {
        try {
            return jweTokenService.isTokenValid(token);
        } catch (Exception e) {
            logger.error("Token validation failed", e);
            return false;
        }
    }

    public void revokeToken(String tokenId) {
        accessTokenRepository.revokeToken(tokenId);
        logger.info("Token revoked: {}", tokenId);
    }

    public void revokeAllUserTokens(Long userId) {
        accessTokenRepository.revokeAllUserTokens(userId);
        logger.info("All tokens revoked for user: {}", userId);
    }
}