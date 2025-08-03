package org.certis.studyplatform.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

import java.time.ZonedDateTime;

/**
 * 전역 응답 처리기
 * 
 * 모든 API 응답을 일관된 형태로 제공
 */
@Getter
@Builder
public class GlobalResponseHandler<T> {
    private final ZonedDateTime timestamp;
    private final int statusCode;
    private final String message;
    private final T data;

    /**
     * 성공 응답 생성 (데이터 포함)
     */
    public static <T> ResponseEntity<GlobalResponseHandler<T>> success(ResponseStatus status, T data) {
        return ResponseEntity.status(status.getStatusCode())
                .body(GlobalResponseHandler.<T>builder()
                        .timestamp(ZonedDateTime.now())
                        .statusCode(status.getStatusCode())
                        .message(status.getMessage())
                        .data(data)
                        .build());
    }

    /**
     * 성공 응답 생성 (데이터 없이)
     */
    public static ResponseEntity<GlobalResponseHandler<Void>> success(ResponseStatus status) {
        return ResponseEntity.status(status.getStatusCode())
                .body(GlobalResponseHandler.<Void>builder()
                        .timestamp(ZonedDateTime.now())
                        .statusCode(status.getStatusCode())
                        .message(status.getMessage())
                        .data(null)
                        .build());
    }

    /**
     * 성공 응답 생성 + 쿠키 세팅 (데이터 포함)
     */
    public static <T> ResponseEntity<GlobalResponseHandler<T>> successWithCookie(
            ResponseStatus status,
            T data,
            ResponseCookie cookie
    ) {
        return ResponseEntity.status(status.getStatusCode())
                .header("Set-Cookie", cookie.toString())
                .body(GlobalResponseHandler.<T>builder()
                        .timestamp(ZonedDateTime.now())
                        .statusCode(status.getStatusCode())
                        .message(status.getMessage())
                        .data(data)
                        .build());
    }

    /**
     * 성공 응답 생성 + 쿠키 세팅 (데이터 없음)
     */
    public static ResponseEntity<GlobalResponseHandler<Void>> successWithCookie(
            ResponseStatus status,
            ResponseCookie cookie
    ) {
        return ResponseEntity.status(status.getStatusCode())
                .header("Set-Cookie", cookie.toString())
                .body(GlobalResponseHandler.<Void>builder()
                        .timestamp(ZonedDateTime.now())
                        .statusCode(status.getStatusCode())
                        .message(status.getMessage())
                        .data(null)
                        .build());
    }

    /**
     * 에러 응답 생성 (Global Exception Handler에서 사용)
     */
    public static ResponseEntity<GlobalResponseHandler<Object>> error(
            int statusCode,
            String message,
            Object errorDetails
    ) {
        return ResponseEntity.status(statusCode)
                .body(GlobalResponseHandler.builder()
                        .timestamp(ZonedDateTime.now())
                        .statusCode(statusCode)
                        .message(message)
                        .data(errorDetails)
                        .build());
    }

    /**
     * 단순 에러 응답 생성
     */
    public static ResponseEntity<GlobalResponseHandler<Object>> error(
            int statusCode,
            String message
    ) {
        return error(statusCode, message, null);
    }
}