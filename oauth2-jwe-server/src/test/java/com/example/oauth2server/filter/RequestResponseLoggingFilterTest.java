package com.example.oauth2server.filter;

import com.example.oauth2.filter.RequestResponseLoggingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestResponseLoggingFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RequestResponseLoggingFilter loggingFilter;

    @BeforeEach
    void setUp() {
        loggingFilter = new RequestResponseLoggingFilter();
    }

    @Test
    void doFilterInternal_ShouldLogRequestAndResponse() throws ServletException, IOException {
        // Given
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/auth/oauth/v2/token-jew");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(response.getStatus()).thenReturn(200);

        // When
        loggingFilter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain, times(1)).doFilter(request, response);
        verify(request, atLeastOnce()).getMethod();
        verify(request, atLeastOnce()).getRequestURI();
        verify(request, atLeastOnce()).getRemoteAddr();
        verify(response, atLeastOnce()).getStatus();
    }

    @Test
    void doFilterInternal_ShouldHandleExceptions() throws ServletException, IOException {
        // Given
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/auth/oauth/v2/token-jew");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }
}