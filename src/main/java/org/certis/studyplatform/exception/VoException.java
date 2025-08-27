package org.certis.studyplatform.exception;

public class VoException extends BaseException {
    
    private static final long serialVersionUID = 1L;

    /**
     * @param status exception에 대한 정보에 대한 enum
     */
    public VoException(ExceptionStatus status) {
        super(status);
    }
}