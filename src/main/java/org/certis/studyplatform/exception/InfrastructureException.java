package org.certis.studyplatform.exception;

/**
 * Infrastructure Layer Exception
 *
 * 데이터베이스, 외부 서비스, 메시징 관련 예외 처리
 * - 데이터베이스 오류
 * - 외부 서비스 연동 실패
 * - 메시징 처리 오류
 * - 리소스 관련 오류
 */
public class InfrastructureException extends BaseException {
    
    private static final long serialVersionUID = 1L;

    public InfrastructureException(ExceptionStatus status) {
        super(status);
    }

    public InfrastructureException(ExceptionStatus status, Throwable cause) {
        super(status, cause);
    }

    public InfrastructureException(ExceptionStatus status, String customMessage) {
        super(status, customMessage);
    }

    public InfrastructureException(ExceptionStatus status, String customMessage, Throwable cause) {
        super(status, customMessage, cause);
    }
}