package org.certis.studyplatform.exception;

import lombok.Getter;

/**
 * 모든 커스텀 예외의 기본 클래스
 * 
 * Clean Architecture 계층별 예외 처리의 기반 제공
 */
@Getter
public abstract class BaseException extends RuntimeException {
    
    private final ExceptionStatus status;
    
    protected BaseException(ExceptionStatus status) {
        super(status.getMessage());
        this.status = status;
    }
    
    protected BaseException(ExceptionStatus status, Throwable cause) {
        super(status.getMessage(), cause);
        this.status = status;
    }
    
    protected BaseException(ExceptionStatus status, String customMessage) {
        super(customMessage);
        this.status = status;
    }
    
    protected BaseException(ExceptionStatus status, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.status = status;
    }
}
