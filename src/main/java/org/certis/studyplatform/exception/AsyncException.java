package org.certis.studyplatform.exception;

public class AsyncException extends BaseException {

    /**
     * @param status exception에 대한 정보에 대한 enum
     */
    public AsyncException(ExceptionStatus status) {
        super(status);
    }
}