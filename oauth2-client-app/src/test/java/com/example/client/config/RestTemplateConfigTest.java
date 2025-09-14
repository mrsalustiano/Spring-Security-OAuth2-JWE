package com.example.client.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "rest-template.connection-timeout=1000",
        "rest-template.read-timeout=2000"
})
class RestTemplateConfigTest {

    @Test
    void restTemplate_ShouldBeConfigured() {
        // Given
        RestTemplateConfig config = new RestTemplateConfig();

        // When
        RestTemplate restTemplate = config.restTemplate();

        // Then
        assertThat(restTemplate).isNotNull();
        assertThat(restTemplate.getInterceptors()).hasSize(1);
        assertThat(restTemplate.getInterceptors().get(0)).isInstanceOf(LoggingInterceptor.class);
    }
}