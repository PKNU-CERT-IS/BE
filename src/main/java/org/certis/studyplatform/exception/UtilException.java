package org.certis.studyplatform.exception;

public class UtilException extends BaseException {

    /**
     * @param status exception에 대한 정보에 대한 enum
     */
    public UtilException(ExceptionStatus status) {
        super(status);
    }
}