package org.certis.studyplatform.shared.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Bucket4j 기반 Rate Limiting 필터
 * - RefreshToken 엔드포인트에 대한 요청 속도를 제한합니다.
 * - IP 주소당 1분에 10회로 제한됩니다.
 * - 제한 초과 시 HTTP 429 (Too Many Requests) 응답을 반환합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;

    // Rate Limiting 설정: 1분당 10회 요청 허용
    private static final int REQUEST_LIMIT = 10;
    private static final Duration REFILL_DURATION = Duration.ofMinutes(1);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // /api/v1/auth/token/refresh 경로만 rate limiting 적용
        String requestURI = request.getRequestURI();
        if (!requestURI.equals("/api/v1/auth/token/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }

        // IP 주소 기반으로 버킷 키 생성
        String clientIp = getClientIp(request);
        String bucketKey = "rate_limit:refresh_token:" + clientIp;

        // Bucket 가져오기 또는 생성
        Bucket bucket = proxyManager.builder().build(bucketKey, getConfigurationSupplier());

        // 토큰 소비 시도
        if (bucket.tryConsume(1)) {
            // 요청 허용
            filterChain.doFilter(request, response);
        } else {
            // Rate limit 초과 - 429 응답
            log.warn("Rate limit exceeded for IP: {}, endpoint: {}", clientIp, requestURI);
            response.setStatus(ExceptionStatus.AUTH_PRESENTATION_RATE_LIMIT_EXCEEDED.getStatusCode());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(String.format(
                "{\"statusCode\":%d,\"message\":\"%s\"}",
                ExceptionStatus.AUTH_PRESENTATION_RATE_LIMIT_EXCEEDED.getStatusCode(),
                ExceptionStatus.AUTH_PRESENTATION_RATE_LIMIT_EXCEEDED.getMessage()
            ));
        }
    }

    /**
     * Bucket4j 설정을 반환하는 Supplier
     * - 1분당 10회 요청 허용 (Token Bucket 알고리즘)
     */
    private Supplier<BucketConfiguration> getConfigurationSupplier() {
        return () -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(REQUEST_LIMIT)
                    .refillIntervally(REQUEST_LIMIT, REFILL_DURATION)
                    .build();
            return BucketConfiguration.builder()
                    .addLimit(limit)
                    .build();
        };
    }

    /**
     * 클라이언트 IP 주소 추출
     * - X-Forwarded-For 헤더를 우선 확인 (프록시 환경 대응)
     * - 없으면 RemoteAddr 사용
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For는 쉼표로 구분된 IP 목록일 수 있으므로 첫 번째 IP 사용
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
