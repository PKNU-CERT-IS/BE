package org.certis.studyplatform.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GlobalExceptionHandler JWT 예외 처리 테스트
 * 
 * JWT 관련 예외들이 적절한 HTTP 응답으로 변환되는지 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("🔐 GlobalExceptionHandler JWT 예외 처리 테스트")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    private MockHttpServletRequest request;
    private ServletWebRequest webRequest;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/token/refresh");
        request.setMethod("POST");
        webRequest = new ServletWebRequest(request);
    }

    // =================================================================
    // JWT 예외 처리 테스트
    // =================================================================

    @Test
    @DisplayName("JWT 토큰 만료 예외 처리 테스트")
    void handleExpiredJwtException_ShouldReturnUnauthorized() {
        // Given
        ExpiredJwtException exception = new ExpiredJwtException(null, null, "JWT expired");

        // When
        ResponseEntity<GlobalResponseHandler<Object>> response = 
                globalExceptionHandler.handleExpiredJwtException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("JWT 토큰이 만료되었습니다");
        assertThat(response.getBody().getStatusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("JWT 토큰 형식 오류 예외 처리 테스트")
    void handleMalformedJwtException_ShouldReturnUnauthorized() {
        // Given
        MalformedJwtException exception = new MalformedJwtException("Malformed JWT token");

        // When
        ResponseEntity<GlobalResponseHandler<Object>> response = 
                globalExceptionHandler.handleMalformedJwtException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("JWT 토큰 형식이 올바르지 않습니다");
        assertThat(response.getBody().getStatusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("JWT 토큰 서명 오류 예외 처리 테스트")
    void handleSignatureException_ShouldReturnUnauthorized() {
        // Given
        SignatureException exception = new SignatureException("JWT signature does not match");

        // When
        ResponseEntity<GlobalResponseHandler<Object>> response = 
                globalExceptionHandler.handleSignatureException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("JWT 토큰 서명이 유효하지 않습니다");
        assertThat(response.getBody().getStatusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("JWT 토큰 지원하지 않는 형식 예외 처리 테스트")
    void handleUnsupportedJwtException_ShouldReturnUnauthorized() {
        // Given
        UnsupportedJwtException exception = new UnsupportedJwtException("Unsupported JWT token");

        // When
        ResponseEntity<GlobalResponseHandler<Object>> response = 
                globalExceptionHandler.handleUnsupportedJwtException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("지원하지 않는 JWT 토큰입니다");
        assertThat(response.getBody().getStatusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("JWT 토큰 일반 예외 처리 테스트")
    void handleJwtException_ShouldReturnUnauthorized() {
        // Given
        JwtException exception = new JwtException("Invalid JWT token");

        // When
        ResponseEntity<GlobalResponseHandler<Object>> response = 
                globalExceptionHandler.handleJwtException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("유효하지 않은 엑세스 토큰입니다");
        assertThat(response.getBody().getStatusCode()).isEqualTo(401);
    }

    // =================================================================
    // ExceptionStatus 검증 테스트
    // =================================================================

    @Test
    @DisplayName("ExceptionStatus를 사용한 JWT 예외 처리 검증")
    void jwtExceptionHandlers_ShouldUseExceptionStatus() {
        // Given
        ExpiredJwtException expiredException = new ExpiredJwtException(null, null, "JWT expired");
        MalformedJwtException malformedException = new MalformedJwtException("Malformed JWT token");
        SignatureException signatureException = new SignatureException("JWT signature does not match");
        UnsupportedJwtException unsupportedException = new UnsupportedJwtException("Unsupported JWT token");
        JwtException generalException = new JwtException("Invalid JWT token");

        // When & Then - 각 예외가 적절한 ExceptionStatus를 사용하는지 확인
        ResponseEntity<GlobalResponseHandler<Object>> expiredResponse = 
                globalExceptionHandler.handleExpiredJwtException(expiredException, webRequest);
        assertThat(expiredResponse.getBody().getStatusCode()).isEqualTo(ExceptionStatus.AUTH_INFRASTRUCTURE_JWT_TOKEN_EXPIRED.getStatusCode());

        ResponseEntity<GlobalResponseHandler<Object>> malformedResponse = 
                globalExceptionHandler.handleMalformedJwtException(malformedException, webRequest);
        assertThat(malformedResponse.getBody().getStatusCode()).isEqualTo(ExceptionStatus.AUTH_INFRASTRUCTURE_JWT_TOKEN_INVALID_FORMAT.getStatusCode());

        ResponseEntity<GlobalResponseHandler<Object>> signatureResponse = 
                globalExceptionHandler.handleSignatureException(signatureException, webRequest);
        assertThat(signatureResponse.getBody().getStatusCode()).isEqualTo(ExceptionStatus.AUTH_INFRASTRUCTURE_JWT_TOKEN_INVALID_SIGNATURE.getStatusCode());

        ResponseEntity<GlobalResponseHandler<Object>> unsupportedResponse = 
                globalExceptionHandler.handleUnsupportedJwtException(unsupportedException, webRequest);
        assertThat(unsupportedResponse.getBody().getStatusCode()).isEqualTo(ExceptionStatus.AUTH_INFRASTRUCTURE_JWT_TOKEN_UNSUPPORTED.getStatusCode());

        ResponseEntity<GlobalResponseHandler<Object>> generalResponse = 
                globalExceptionHandler.handleJwtException(generalException, webRequest);
        assertThat(generalResponse.getBody().getStatusCode()).isEqualTo(ExceptionStatus.AUTH_INFRASTRUCTURE_INVALID_ACCESS_TOKEN.getStatusCode());
    }

    // =================================================================
    // 에러 응답 구조 검증 테스트
    // =================================================================

    @Test
    @DisplayName("JWT 예외 응답 구조 검증 테스트")
    void jwtExceptionResponse_ShouldHaveCorrectStructure() {
        // Given
        ExpiredJwtException exception = new ExpiredJwtException(null, null, "JWT expired");

        // When
        ResponseEntity<GlobalResponseHandler<Object>> response = 
                globalExceptionHandler.handleExpiredJwtException(exception, webRequest);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getStatusCode()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("JWT 토큰이 만료되었습니다");
        assertThat(response.getBody().getData()).isNotNull();
        
        // 에러 상세 정보 검증
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> errorDetails = (java.util.Map<String, Object>) response.getBody().getData();
        assertThat(errorDetails.get("type")).isEqualTo("JWT_EXPIRED");
        assertThat(errorDetails.get("path")).isEqualTo("/api/v1/auth/token/refresh");
    }
}