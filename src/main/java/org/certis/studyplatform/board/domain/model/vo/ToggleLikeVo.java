package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

/**
 * Toggle Like Value Object
 * 좋아요 토글용 VO
 */
public record ToggleLikeVo(
        Long boardId,
        Long memberId
) {

    public static ToggleLikeVo of(Long boardId, Long memberId) {
        BoardIdVo.of(boardId);   // 검증
        if (memberId == null || memberId <= 0) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_ID);
        }

        return new ToggleLikeVo(boardId, memberId);
    }
}
