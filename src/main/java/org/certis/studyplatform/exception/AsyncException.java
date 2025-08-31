package org.certis.studyplatform.exception;

public class AsyncException extends BaseException {
    
    private static final long serialVersionUID = 1L;

    /**
     * @param status exception에 대한 정보에 대한 enum
     */
    public AsyncException(ExceptionStatus status) {
        super(status);
    }
}