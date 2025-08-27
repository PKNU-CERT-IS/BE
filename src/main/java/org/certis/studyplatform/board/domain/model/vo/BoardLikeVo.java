package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record BoardLikeVo(
        Long boardId,
        Long memberId,
        boolean isLiked,
        Long likeCount
) {

    public static BoardLikeVo of(Long boardId, Long memberId, boolean isLiked, Long likeCount) {
        BoardIdVo.of(boardId);
        if (memberId == null || memberId <= 0) {
            throw new DomainException(ExceptionStatus.MEMBER_DOMAIN_NOT_FOUND);
        }

        return new BoardLikeVo(boardId, memberId, isLiked, likeCount != null ? likeCount : 0L);
    }
}
