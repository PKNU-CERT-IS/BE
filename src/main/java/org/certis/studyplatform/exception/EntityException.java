package org.certis.studyplatform.exception;

public class EntityException extends BaseException {

    /**
     * @param status exception에 대한 정보에 대한 enum
     */
    public EntityException(ExceptionStatus status) {
        super(status);
    }
}