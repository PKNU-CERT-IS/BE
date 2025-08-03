package org.certis.studyplatform.exception;

public class MiddlewareException extends BaseException {

    /**
     * @param status exception에 대한 정보에 대한 enum
     */
    public MiddlewareException(ExceptionStatus status) {
        super(status);
    }
}