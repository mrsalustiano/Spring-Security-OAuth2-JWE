package com.example.oauth2.task;

import com.example.oauth2.repository.AccessTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TokenCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupTask.class);

    @Autowired
    private AccessTokenRepository accessTokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredTokens() {
        try {
            logger.info("Starting cleanup of expired tokens");

            int deletedTokens = jdbcTemplate.update(
                    "DELETE FROM access_tokens WHERE expires_at < ? OR revoked = TRUE",
                    LocalDateTime.now()
            );

            logger.info("Cleaned up {} expired/revoked tokens", deletedTokens);

        } catch (Exception e) {
            logger.error("Error during token cleanup", e);
        }
    }

    @Scheduled(fixedRate = 1800000) // Run every 30 minutes
    public void cleanupOldRateLimitBuckets() {
        try {
            logger.info("Starting cleanup of old rate limit buckets");

            int deletedBuckets = jdbcTemplate.update(
                    "DELETE FROM rate_limit_buckets WHERE last_refill < ?",
                    LocalDateTime.now().minusHours(24)
            );

            logger.info("Cleaned up {} old rate limit buckets", deletedBuckets);

        } catch (Exception e) {
            logger.error("Error during rate limit bucket cleanup", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    public void dailyMaintenanceTask() {
        try {
            logger.info("Starting daily maintenance tasks");

            // Clean up old refresh tokens
            int deletedRefreshTokens = jdbcTemplate.update(
                    "DELETE FROM refresh_tokens WHERE expires_at < ? OR revoked = TRUE",
                    LocalDateTime.now()
            );

            logger.info("Daily maintenance completed. Deleted {} refresh tokens", deletedRefreshTokens);

        } catch (Exception e) {
            logger.error("Error during daily maintenance", e);
        }
    }
}