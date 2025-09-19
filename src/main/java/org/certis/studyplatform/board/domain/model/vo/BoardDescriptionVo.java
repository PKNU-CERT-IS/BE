package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record BoardDescriptionVo(String value) {

    public static BoardDescriptionVo of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_DESCRIPTION);
        }
        if (value.trim().length() > 100) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_DESCRIPTION);
        }
        return new BoardDescriptionVo(value);
    }
}
