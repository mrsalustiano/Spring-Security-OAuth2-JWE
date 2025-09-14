package com.example.oauth2.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.EncryptedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JweTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JweTokenService.class);

    @Value("${jwe.encryption.key}")
    private String encryptionKey;

    @Value("${jwe.signing.key}")
    private String signingKey;

    public String generateJweToken(Long userId, String username, List<String> roles,
                                   String clientId, List<String> scopes,
                                   LocalDateTime expiresAt) {
        try {
            // Create JWT claims
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(username)
                    .issuer("oauth2-jwe-server")
                    .audience(clientId)
                    .expirationTime(Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant()))
                    .issueTime(new Date())
                    .notBeforeTime(new Date())
                    .jwtID(UUID.randomUUID().toString())
                    .claim("user_id", userId)
                    .claim("username", username)
                    .claim("roles", roles)
                    .claim("client_id", clientId)
                    .claim("scopes", scopes)
                    .build();

            // Create signed JWT
            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet
            );

            // Sign the JWT
            JWSSigner signer = new MACSigner(signingKey.getBytes());
            signedJWT.sign(signer);

            // Create JWE header
            JWEHeader jweHeader = new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
                    .contentType("JWT")
                    .build();

            // Create encrypted JWT
            EncryptedJWT encryptedJWT = new EncryptedJWT(jweHeader, claimsSet);

            // Encrypt the JWT
            JWEEncrypter encrypter = new DirectEncrypter(encryptionKey.getBytes());
            encryptedJWT.encrypt(encrypter);

            return encryptedJWT.serialize();

        } catch (Exception e) {
            logger.error("Error generating JWE token", e);
            throw new RuntimeException("Failed to generate JWE token", e);
        }
    }

    public JWTClaimsSet validateAndParseToken(String jweToken) {
        try {
            // Parse the encrypted JWT
            EncryptedJWT encryptedJWT = EncryptedJWT.parse(jweToken);

            // Decrypt the JWT
            JWEDecrypter decrypter = new DirectDecrypter(encryptionKey.getBytes());
            encryptedJWT.decrypt(decrypter);

            // Get the claims
            JWTClaimsSet claimsSet = encryptedJWT.getJWTClaimsSet();

            // Verify expiration
            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime != null && expirationTime.before(new Date())) {
                throw new RuntimeException("Token has expired");
            }

            // Verify not before
            Date notBeforeTime = claimsSet.getNotBeforeTime();
            if (notBeforeTime != null && notBeforeTime.after(new Date())) {
                throw new RuntimeException("Token not yet valid");
            }

            return claimsSet;

        } catch (Exception e) {
            logger.error("Error validating JWE token", e);
            throw new RuntimeException("Invalid JWE token", e);
        }
    }

    public boolean isTokenValid(String jweToken) {
        try {
            validateAndParseToken(jweToken);
            return true;
        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String jweToken) {
        try {
            JWTClaimsSet claims = validateAndParseToken(jweToken);
            return claims.getSubject();
        } catch (Exception e) {
            logger.error("Error extracting username from token", e);
            return null;
        }
    }

    public Long extractUserId(String jweToken) {
        try {
            JWTClaimsSet claims = validateAndParseToken(jweToken);
            return claims.getLongClaim("user_id");
        } catch (Exception e) {
            logger.error("Error extracting user ID from token", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String jweToken) {
        try {
            JWTClaimsSet claims = validateAndParseToken(jweToken);
            return (List<String>) claims.getClaim("roles");
        } catch (Exception e) {
            logger.error("Error extracting roles from token", e);
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> extractScopes(String jweToken) {
        try {
            JWTClaimsSet claims = validateAndParseToken(jweToken);
            return (List<String>) claims.getClaim("scopes");
        } catch (Exception e) {
            logger.error("Error extracting scopes from token", e);
            return List.of();
        }
    }

    public String extractClientId(String jweToken) {
        try {
            JWTClaimsSet claims = validateAndParseToken(jweToken);
            return claims.getStringClaim("client_id");
        } catch (Exception e) {
            logger.error("Error extracting client ID from token", e);
            return null;
        }
    }

    public Date extractExpirationDate(String jweToken) {
        try {
            JWTClaimsSet claims = validateAndParseToken(jweToken);
            return claims.getExpirationTime();
        } catch (Exception e) {
            logger.error("Error extracting expiration date from token", e);
            return null;
        }
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}