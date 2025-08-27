package org.certis.studyplatform.exception;

/**
 * Domain Layer Exception
 *
 * 도메인 모델, 비즈니스 규칙, 애그리게이트 관련 예외 처리
 * - 도메인 규칙 위반
 * - 애그리게이트 일관성 위반
 * - 도메인 이벤트 처리 실패
 * - 도메인별 특화 예외
 */
public class DomainException extends BaseException {
    
    private static final long serialVersionUID = 1L;

    public DomainException(ExceptionStatus status) {
        super(status);
    }

    public DomainException(ExceptionStatus status, Throwable cause) {
        super(status, cause);
    }

    public DomainException(ExceptionStatus status, String customMessage) {
        super(status, customMessage);
    }

    public DomainException(ExceptionStatus status, String customMessage, Throwable cause) {
        super(status, customMessage, cause);
    }
}