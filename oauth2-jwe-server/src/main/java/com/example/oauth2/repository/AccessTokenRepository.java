package com.example.oauth2.repository;

import com.example.oauth2.model.AccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AccessTokenRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<AccessToken> tokenRowMapper = new RowMapper<AccessToken>() {
        @Override
        public AccessToken mapRow(ResultSet rs, int rowNum) throws SQLException {
            AccessToken token = new AccessToken();
            token.setId(rs.getLong("id"));
            token.setTokenId(rs.getString("token_id"));
            token.setTokenValue(rs.getString("token_value"));
            token.setRefreshToken(rs.getString("refresh_token"));
            token.setUsuarioId(rs.getLong("usuario_id"));
            token.setClientId(rs.getString("client_id"));
            token.setScopes(rs.getString("scopes"));
            token.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
            token.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            token.setRevoked(rs.getBoolean("revoked"));
            return token;
        }
    };

    public AccessToken save(AccessToken token) {
        if (token.getId() == null) {
            return insert(token);
        } else {
            return update(token);
        }
    }

    private AccessToken insert(AccessToken token) {
        String sql = """
            INSERT INTO access_tokens (token_id, token_value, refresh_token, usuario_id, 
            client_id, scopes, expires_at, created_at, revoked) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        LocalDateTime now = LocalDateTime.now();
        token.setCreatedAt(now);

        jdbcTemplate.update(sql,
                token.getTokenId(),
                token.getTokenValue(),
                token.getRefreshToken(),
                token.getUsuarioId(),
                token.getClientId(),
                token.getScopes(),
                token.getExpiresAt(),
                now,
                token.getRevoked()
        );

        // Get the generated ID
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        token.setId(id);

        return token;
    }

    private AccessToken update(AccessToken token) {
        String sql = """
            UPDATE access_tokens SET token_value = ?, refresh_token = ?, 
            scopes = ?, expires_at = ?, revoked = ? WHERE id = ?
        """;

        jdbcTemplate.update(sql,
                token.getTokenValue(),
                token.getRefreshToken(),
                token.getScopes(),
                token.getExpiresAt(),
                token.getRevoked(),
                token.getId()
        );

        return token;
    }

    public Optional<AccessToken> findByTokenId(String tokenId) {
        String sql = "SELECT * FROM access_tokens WHERE token_id = ? AND revoked = FALSE";
        List<AccessToken> tokens = jdbcTemplate.query(sql, tokenRowMapper, tokenId);
        return tokens.isEmpty() ? Optional.empty() : Optional.of(tokens.get(0));
    }

    public Optional<AccessToken> findByRefreshToken(String refreshToken) {
        String sql = "SELECT * FROM access_tokens WHERE refresh_token = ? AND revoked = FALSE";
        List<AccessToken> tokens = jdbcTemplate.query(sql, tokenRowMapper, refreshToken);
        return tokens.isEmpty() ? Optional.empty() : Optional.of(tokens.get(0));
    }

    public List<AccessToken> findByUsuarioId(Long usuarioId) {
        String sql = "SELECT * FROM access_tokens WHERE usuario_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, tokenRowMapper, usuarioId);
    }

    public List<AccessToken> findByClientId(String clientId) {
        String sql = "SELECT * FROM access_tokens WHERE client_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, tokenRowMapper, clientId);
    }

    public void revokeToken(String tokenId) {
        String sql = "UPDATE access_tokens SET revoked = TRUE WHERE token_id = ?";
        jdbcTemplate.update(sql, tokenId);
    }

    public void revokeAllUserTokens(Long usuarioId) {
        String sql = "UPDATE access_tokens SET revoked = TRUE WHERE usuario_id = ?";
        jdbcTemplate.update(sql, usuarioId);
    }

    public void deleteExpiredTokens() {
        String sql = "DELETE FROM access_tokens WHERE expires_at < ? OR revoked = TRUE";
        jdbcTemplate.update(sql, LocalDateTime.now());
    }

    public List<AccessToken> findExpiredTokens() {
        String sql = "SELECT * FROM access_tokens WHERE expires_at < ? AND revoked = FALSE";
        return jdbcTemplate.query(sql, tokenRowMapper, LocalDateTime.now());
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM access_tokens WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public boolean existsByTokenId(String tokenId) {
        String sql = "SELECT COUNT(*) FROM access_tokens WHERE token_id = ? AND revoked = FALSE";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tokenId);
        return count != null && count > 0;
    }
}