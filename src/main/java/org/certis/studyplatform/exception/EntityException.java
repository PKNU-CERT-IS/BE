package org.certis.studyplatform.exception;

public class EntityException extends BaseException {
    
    private static final long serialVersionUID = 1L;

    /**
     * @param status exception에 대한 정보에 대한 enum
     */
    public EntityException(ExceptionStatus status) {
        super(status);
    }
}