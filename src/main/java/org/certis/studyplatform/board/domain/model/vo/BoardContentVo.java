package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record BoardContentVo(String value) {

    public static BoardContentVo of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_CONTENT);
        }
        // 글자 수 상한 제한 제거 (무제한 허용)
        
        return new BoardContentVo(value);
    }
}