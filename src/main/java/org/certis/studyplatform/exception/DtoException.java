package org.certis.studyplatform.exception;

public class DtoException extends BaseException {

    /**
     * @param status exception에 대한 정보에 대한 enum
     */
    public DtoException(ExceptionStatus status) {
        super(status);
    }
}