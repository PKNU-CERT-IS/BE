package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record BoardContentVo(String value) {

    public static BoardContentVo of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_CONTENT);
        }
        return new BoardContentVo(value);
    }
}