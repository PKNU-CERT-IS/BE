package org.certis.studyplatform.blog.infrastructure.persistence.jpa;

import org.certis.studyplatform.blog.infrastructure.persistence.entity.BlogViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Blog View JPA Repository
 *
 * Spring Data JPA 기반 블로그 조회수 데이터 액세스 인터페이스
 * CQRS Command 측면에서 사용되는 JPA Repository
 * 벌크 연산을 통한 효율적인 조회수 관리
 */
@Repository
public interface BlogViewJpaRepository extends JpaRepository<BlogViewEntity, Long> {

    // === 벌크 연산 메서드 ===

    /**
     * 블로그 조회수 증가 - 벌크 연산
     * 특정 blogId의 조회수를 1 증가시킴
     *
     * @param blogId 블로그 ID
     * @return 영향받은 행의 수 (0이면 해당 블로그가 존재하지 않음)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BlogViewEntity bv SET bv.viewNumber = bv.viewNumber + 1 " +
            "WHERE bv.blogId = :blogId")
    int incrementViewCount(@Param("blogId") Long blogId);

    /**
     * 블로그 조회수 업데이트 - 벌크 연산
     * 특정 blogId의 조회수를 지정된 값으로 설정
     * Redis → RDB 동기화 시 사용
     *
     * @param blogId 블로그 ID
     * @param viewCount 설정할 조회수
     * @return 영향받은 행의 수 (0이면 해당 블로그가 존재하지 않음)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BlogViewEntity bv SET bv.viewNumber = :viewCount " +
            "WHERE bv.blogId = :blogId")
    int updateViewCount(@Param("blogId") Long blogId, @Param("viewCount") Integer viewCount);

    /**
     * 블로그 조회수 UPSERT - 벌크 연산 (MySQL 전용)
     * 존재하면 업데이트, 없으면 삽입
     * 네이티브 쿼리를 사용한 UPSERT 구현
     *
     * @param blogId 블로그 ID
     * @param viewCount 조회수
     * @return 영향받은 행의 수
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO blog_view (blog_id, view_number, created_at) " +
            "VALUES (:blogId, :viewCount, NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "view_number = :viewCount",
            nativeQuery = true)
    int upsertViewCount(@Param("blogId") Long blogId, @Param("viewCount") Integer viewCount);

    /**
     * 블로그 조회수 UPSERT - 벌크 연산 (PostgreSQL 전용)
     * 존재하면 업데이트, 없으면 삽입
     *
     * @param blogId 블로그 ID
     * @param viewCount 조회수
     * @return 영향받은 행의 수
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO blog_view (blog_id, view_number, created_at) " +
            "VALUES (:blogId, :viewCount, NOW()) " +
            "ON CONFLICT (blog_id) DO UPDATE SET " +
            "view_number = :viewCount",
            nativeQuery = true)
    int upsertViewCountPostgreSQL(@Param("blogId") Long blogId, @Param("viewCount") Integer viewCount);


    /**
     * 블로그 ID로 조회수 통계 삭제 - 벌크 연산
     *
     * @param blogId 블로그 ID
     * @return 삭제된 행의 수
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM BlogViewEntity bv WHERE bv.blogId = :blogId")
    int deleteByBlogId(@Param("blogId") Long blogId);
}