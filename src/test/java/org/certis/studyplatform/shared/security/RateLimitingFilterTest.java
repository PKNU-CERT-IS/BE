package org.certis.studyplatform.shared.security;

import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import jakarta.servlet.FilterChain;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private ProxyManager<String> proxyManager;

    @Mock
    private RemoteBucketBuilder<String> bucketBuilder;

    @Mock
    private BucketProxy bucket;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RateLimitingFilter rateLimitingFilter;

    @Test
    @DisplayName("RefreshToken 엔드포인트가 아니면 Rate Limiting을 건너뛴다")
    void shouldSkipRateLimitingForNonRefreshEndpoint() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        rateLimitingFilter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(proxyManager, never()).builder();
    }

    @Test
    @DisplayName("Rate Limit 내의 요청은 성공적으로 통과한다")
    void shouldAllowRequestWithinRateLimit() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/token/refresh");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(proxyManager.builder()).willReturn(bucketBuilder);
        // any(Supplier.class)를 사용하여 모호성 해결
        given(bucketBuilder.build(anyString(), any(Supplier.class))).willReturn(bucket);
        given(bucket.tryConsume(1)).willReturn(true);

        // when
        rateLimitingFilter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(bucket).tryConsume(1);
    }

    @Test
    @DisplayName("Rate Limit 초과 시 429 에러를 반환한다")
    void shouldRejectRequestExceedingRateLimit() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/token/refresh");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(proxyManager.builder()).willReturn(bucketBuilder);
        given(bucketBuilder.build(anyString(), any(Supplier.class))).willReturn(bucket);
        given(bucket.tryConsume(1)).willReturn(false);

        // when
        rateLimitingFilter.doFilter(request, response, filterChain);

        // then
        verify(filterChain, never()).doFilter(request, response);
        
        assertThat(response.getStatus()).isEqualTo(ExceptionStatus.AUTH_PRESENTATION_RATE_LIMIT_EXCEEDED.getStatusCode());
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더가 있을 경우 이를 사용하여 IP를 식별한다")
    void shouldUseXForwardedForHeader() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/token/refresh");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(proxyManager.builder()).willReturn(bucketBuilder);
        given(bucketBuilder.build(eq("rate_limit:refresh_token:10.0.0.1"), any(Supplier.class))).willReturn(bucket);
        given(bucket.tryConsume(1)).willReturn(true);

        // when
        rateLimitingFilter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(bucket).tryConsume(1);
    }
}
