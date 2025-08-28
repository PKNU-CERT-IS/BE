package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record BoardCategoryVo(String value) {

    public static BoardCategoryVo of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_CATEGORY);
        }
        return new BoardCategoryVo(value.toUpperCase());
    }
}
