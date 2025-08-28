package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record BoardIdVo(Long value) {

    public static BoardIdVo of(Long value) {
        if (value == null || value <= 0) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_ID);
        }
        return new BoardIdVo(value);
    }
}
