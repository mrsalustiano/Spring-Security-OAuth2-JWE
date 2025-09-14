package com.example.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
class OAuth2ClientApplicationTest {

    @Test
    void contextLoads() {
        // Test that the Spring context loads successfully
    }
}