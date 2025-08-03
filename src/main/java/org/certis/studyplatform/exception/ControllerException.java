package org.certis.studyplatform.exception;

public class ControllerException extends RuntimeException implements BaseException {

    private final ExceptionStatus status;

    /**
     * @param status exception에 대한 정보에 대한 enum
     */
    public ControllerException(ExceptionStatus status) {
        this.status = status;
    }

    public ExceptionStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return status.getMessage();
    }
}