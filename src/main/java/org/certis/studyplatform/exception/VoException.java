package org.certis.studyplatform.exception;

public class VoException extends BaseException {

    /**
     * @param status exception에 대한 정보에 대한 enum
     */
    public VoException(ExceptionStatus status) {
        super(status);
    }
}