package org.certis.studyplatform.blog.domain.repository;

public interface BlogViewCommandRepository {

    /**
     * 블로그 조회수 증가 (비동기)
     */
    void incrementViewCount(Long id);

    /**
     * 블로그 조회수 업데이트 (Redis → RDB 동기화용)
     */
    void updateViewCount(Long id, Integer viewCount);

    /**
     * 블로그 조회수 통계 저장/업데이트 (BlogViewEntity 사용)
     * Redis → RDB 동기화 시 사용
     */
    void saveOrUpdateBlogViewStats(Long blogId, Integer viewCount);
}
