package com.example.oauth2.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class AccessToken {
    private Long id;
    private String tokenId;
    private String tokenValue;
    private String refreshToken;
    private Long usuarioId;
    private String clientId;
    private String scopes;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Boolean revoked;

    // Constructors

    public AccessToken(String tokenId, String tokenValue, String refreshToken,
                       Long usuarioId, String clientId, String scopes,
                       LocalDateTime expiresAt) {
        this.tokenId = tokenId;
        this.tokenValue = tokenValue;
        this.refreshToken = refreshToken;
        this.usuarioId = usuarioId;
        this.clientId = clientId;
        this.scopes = scopes;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getRevoked() {
        return revoked;
    }

    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    @Override
    public String toString() {
        return "AccessToken{" +
                "id=" + id +
                ", tokenId='" + tokenId + '\'' +
                ", usuarioId=" + usuarioId +
                ", clientId='" + clientId + '\'' +
                ", scopes='" + scopes + '\'' +
                ", expiresAt=" + expiresAt +
                ", revoked=" + revoked +
                '}';
    }
}