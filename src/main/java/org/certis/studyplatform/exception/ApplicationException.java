package org.certis.studyplatform.exception;

/**
 * Application Layer Exception
 * 
 * 애플리케이션 서비스, CQRS, Facade 관련 예외 처리
 * - 비즈니스 규칙 위반
 * - 명령/조회 실행 실패
 * - Facade 작업 실패
 */
public class ApplicationException extends BaseException {
    
    public ApplicationException(ExceptionStatus status) {
        super(status);
    }
    
    public ApplicationException(ExceptionStatus status, Throwable cause) {
        super(status, cause);
    }
    
    public ApplicationException(ExceptionStatus status, String customMessage) {
        super(status, customMessage);
    }
    
    public ApplicationException(ExceptionStatus status, String customMessage, Throwable cause) {
        super(status, customMessage, cause);
    }
} 