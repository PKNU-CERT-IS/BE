package org.certis.studyplatform.blog.domain.repository;

import org.certis.studyplatform.blog.domain.vo.BlogIdVo;

/**
 * Blog Redis Repository Interface
 *
 * Redis 기반 블로그 통계 관리
 * - 조회수 관리 (실시간)
 * - 매일 00시 RDB 동기화
 */
public interface BlogRedisRepository {

    // 초기 설정 (조회수 0으로 설정)
    void initializeStats(BlogIdVo blogIdVo);

    // 설정 삭제 (블로그 삭제 시)
    void deleteStats(BlogIdVo blogIdVo);

    // 조회수 추가 (중복 방지)
    void addView(BlogIdVo blogId, Long viewerId);

    // 비로그인 유저 조회수 추가 (중복 방지 없음)
    void addViewForAnonymous(BlogIdVo blogId);

    // 조회 여부 확인
    boolean isViewedByMember(BlogIdVo blogId, Long viewerId);

    // 조회수 조회
    Long getViewCount(BlogIdVo blogId);
}