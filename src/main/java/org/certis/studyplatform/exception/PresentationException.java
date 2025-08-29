package org.certis.studyplatform.exception;

/**
 * Presentation Layer Exception
 *
 * Controller, 요청 검증, 인증/인가 관련 예외 처리
 * - 요청 데이터 검증 실패
 * - 인증/인가 실패
 * - HTTP 관련 오류
 */
public class PresentationException extends BaseException {
    
    private static final long serialVersionUID = 1L;

    public PresentationException(ExceptionStatus status) {
        super(status);
    }

    public PresentationException(ExceptionStatus status, Throwable cause) {
        super(status, cause);
    }

    public PresentationException(ExceptionStatus status, String customMessage) {
        super(status, customMessage);
    }

    public PresentationException(ExceptionStatus status, String customMessage, Throwable cause) {
        super(status, customMessage, cause);
    }
}