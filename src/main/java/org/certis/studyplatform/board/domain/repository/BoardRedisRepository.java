package org.certis.studyplatform.board.domain.repository;

import org.certis.studyplatform.board.domain.model.vo.BoardIdVo;

import java.util.List;

public interface BoardRedisRepository {

    // 초기 설정 ( 조회수, 좋아요 수 )
    void initializeStats(BoardIdVo boardIdVo);

    // 설정 삭제 ( 조회수, 좋아요 수 )
    void deleteStats(BoardIdVo boardIdVo);

    // 좋아요 추가
    void addLike(BoardIdVo boardId, Long memberId);

    // 좋아요 삭제
    void removeLike(BoardIdVo boardId, Long memberId);

    // 좋아요 여부
    boolean isLikedByMember(BoardIdVo boardId, Long memberId);

    // 좋아요 수 조회
    Long getLikeCount(BoardIdVo boardId);

    // 조회수 추가
    void addView(BoardIdVo boardId, Long viewerId);

    // 조회 여부
    boolean isViewedByMember(BoardIdVo boardId, Long viewerId);

    // 조회수 조회
    Long getViewCount(BoardIdVo boardId);

    // 조회수 증가
    void incrementViewCount(BoardIdVo boardId);

    // 테스트용: 특정 값으로 좋아요 수 설정
    void setLikeCount(BoardIdVo boardId, Long count);

    // 테스트용: 특정 값으로 조회수 설정
    void setViewCount(BoardIdVo boardId, Long count);

}
