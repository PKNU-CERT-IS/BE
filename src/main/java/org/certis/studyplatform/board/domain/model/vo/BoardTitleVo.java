package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record BoardTitleVo(String value) {

    public static BoardTitleVo of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_TITLE);
        }
        return new BoardTitleVo(value);
    }
}