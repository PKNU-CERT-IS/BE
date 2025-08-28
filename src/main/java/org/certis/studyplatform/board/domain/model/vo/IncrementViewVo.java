package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

/**
 * Increment View Value Object
 * 조회수 증가용 VO
 */
public record IncrementViewVo(
        Long boardId,
        Long viewerId
) {

    public static IncrementViewVo of(Long boardId, Long viewerId) {
        BoardIdVo.of(boardId);   // 검증
        if (viewerId == null || viewerId <= 0) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_ID);
        }

        return new IncrementViewVo(boardId, viewerId);
    }
}

