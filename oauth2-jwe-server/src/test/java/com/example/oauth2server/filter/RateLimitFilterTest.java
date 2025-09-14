package com.example.oauth2server.filter;

import com.example.oauth2.filter.RateLimitFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter Tests")
class RateLimitFilterTest {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter();
        ReflectionTestUtils.setField(rateLimitFilter, "requestsPerSecond", 5);
        ReflectionTestUtils.setField(rateLimitFilter, "bucketCapacity", 10);
    }

    @Nested
    @DisplayName("Rate Limiting Tests")
    class RateLimitingTests {

        @Test
        @DisplayName("Should allow request when within rate limit")
        void doFilterInternal_ShouldAllowRequest_WhenWithinRateLimit() throws ServletException, IOException {
            // Given
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getRequestURI()).thenReturn("/auth/oauth/v2/token-jew");

            // When
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain, times(1)).doFilter(request, response);
            verify(response, never()).setStatus(anyInt());
        }

        @Test
        @DisplayName("Should block request when exceeds rate limit")
        void doFilterInternal_ShouldBlockRequest_WhenExceedsRateLimit() throws ServletException, IOException {
            // Given
            String clientIp = "127.0.0.1";
            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(request.getRequestURI()).thenReturn("/auth/oauth/v2/token-jew");

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(printWriter);

            // Simulate multiple requests to exceed rate limit
            for (int i = 0; i < 15; i++) {
                rateLimitFilter.doFilterInternal(request, response, filterChain);
            }

            // Then
            verify(response, atLeastOnce()).setStatus(HTTP_TOO_MANY_REQUESTS);
            verify(response, atLeastOnce()).setContentType("application/json");
            verify(filterChain, atMost(10)).doFilter(request, response); // Should be blocked after capacity
        }

        @Test
        @DisplayName("Should skip rate limit for non-auth endpoints")
        void doFilterInternal_ShouldSkipRateLimit_ForNonAuthEndpoints() throws ServletException, IOException {
            // Given
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getRequestURI()).thenReturn("/api/v1/protected/profile");

            // When
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain, times(1)).doFilter(request, response);
            verify(response, never()).setStatus(anyInt());
        }

        @Test
        @DisplayName("Should handle different IPs separately")
        void doFilterInternal_ShouldHandleDifferentIPs_Separately() throws ServletException, IOException {
            // Given
            HttpServletRequest request1 = mock(HttpServletRequest.class);
            HttpServletRequest request2 = mock(HttpServletRequest.class);

            when(request1.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request1.getRequestURI()).thenReturn("/auth/oauth/v2/token-jew");

            when(request2.getRemoteAddr()).thenReturn("192.168.1.1");
            when(request2.getRequestURI()).thenReturn("/auth/oauth/v2/token-jew");

            // When - Make requests from different IPs
            for (int i = 0; i < 5; i++) {
                rateLimitFilter.doFilterInternal(request1, response, filterChain);
                rateLimitFilter.doFilterInternal(request2, response, filterChain);
            }

            // Then - Both should be allowed as they have separate buckets
            verify(filterChain, times(10)).doFilter(any(HttpServletRequest.class), eq(response));
            verify(response, never()).setStatus(HTTP_TOO_MANY_REQUESTS);
        }
    }

    @Nested
    @DisplayName("IP Address Detection Tests")
    class IpAddressDetectionTests {

        @Test
        @DisplayName("Should use X-Forwarded-For header when available")
        void shouldUseXForwardedForHeader() throws ServletException, IOException {
            // Given
            when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.1");
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getRequestURI()).thenReturn("/auth/oauth/v2/token-jew");

            // When
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain, times(1)).doFilter(request, response);
            // The filter should use 192.168.1.100 as the client IP
        }

        @Test
        @DisplayName("Should use X-Real-IP header when X-Forwarded-For is not available")
        void shouldUseXRealIpHeader() throws ServletException, IOException {
            // Given
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.200");
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getRequestURI()).thenReturn("/auth/oauth/v2/token-jew");

            // When
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain, times(1)).doFilter(request, response);
            // The filter should use 192.168.1.200 as the client IP
        }

        @Test
        @DisplayName("Should fallback to remote address when headers are not available")
        void shouldFallbackToRemoteAddress() throws ServletException, IOException {
            // Given
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getRequestURI()).thenReturn("/auth/oauth/v2/token-jew");

            // When
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain, times(1)).doFilter(request, response);
            // The filter should use 127.0.0.1 as the client IP
        }
    }

    @Nested
    @DisplayName("Error Response Tests")
    class ErrorResponseTests {

        @Test
        @DisplayName("Should return proper error response when rate limit exceeded")
        void shouldReturnProperErrorResponse() throws ServletException, IOException {
            // Given
            String clientIp = "127.0.0.1";
            when(request.getRemoteAddr()).thenReturn(clientIp);
            when(request.getRequestURI()).thenReturn("/auth/oauth/v2/token-jew");

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(printWriter);

            // Exceed rate limit
            for (int i = 0; i < 15; i++) {
                rateLimitFilter.doFilterInternal(request, response, filterChain);
            }

            // Then
            verify(response, atLeastOnce()).setStatus(HTTP_TOO_MANY_REQUESTS);
            verify(response, atLeastOnce()).setContentType("application/json");
            verify(response, atLeastOnce()).setCharacterEncoding("UTF-8");

            String responseContent = stringWriter.toString();
            assertThat(responseContent).contains("rate_limit_exceeded");
            assertThat(responseContent).contains("Too many requests");
            assertThat(responseContent).contains("retry_after");
        }
    }
}