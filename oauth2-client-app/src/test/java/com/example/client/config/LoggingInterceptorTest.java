package com.example.client.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingInterceptorTest {

    @Mock
    private HttpRequest request;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private ClientHttpResponse response;

    private LoggingInterceptor loggingInterceptor;

    @BeforeEach
    void setUp() {
        loggingInterceptor = new LoggingInterceptor();
    }

    @Test
    void intercept_ShouldLogRequestAndResponse() throws IOException {
        // Given
        byte[] body = "test body".getBytes();

        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/test"));
        when(request.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());

        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(response.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());
        when(response.getBody()).thenReturn(new ByteArrayInputStream("response body".getBytes()));

        when(execution.execute(request, body)).thenReturn(response);

        // When
        ClientHttpResponse result = loggingInterceptor.intercept(request, body, execution);

        // Then
        assertThat(result).isEqualTo(response);
        verify(execution, times(1)).execute(request, body);
        verify(request, atLeastOnce()).getMethod();
        verify(request, atLeastOnce()).getURI();
        verify(response, atLeastOnce()).getStatusCode();
    }

    @Test
    void intercept_ShouldHandleEmptyBody() throws IOException {
        // Given
        byte[] emptyBody = new byte[0];

        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/test"));
        when(request.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());

        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(response.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());

        when(execution.execute(request, emptyBody)).thenReturn(response);

        // When
        ClientHttpResponse result = loggingInterceptor.intercept(request, emptyBody, execution);

        // Then
        assertThat(result).isEqualTo(response);
        verify(execution, times(1)).execute(request, emptyBody);
    }
}