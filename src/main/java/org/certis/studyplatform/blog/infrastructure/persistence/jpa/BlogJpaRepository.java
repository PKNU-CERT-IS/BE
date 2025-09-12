package org.certis.studyplatform.blog.infrastructure.persistence.jpa;

import io.lettuce.core.dynamic.annotation.Param;
import org.certis.studyplatform.blog.infrastructure.persistence.entity.BlogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

/**
 * Blog JPA Repository
 *
 * Spring Data JPA 기반 데이터 액세스 인터페이스
 * CQRS Command 측면에서 사용되는 JPA Repository
 * Write 작업과 Command 검증에 필요한 메서드만 포함
 */
@Repository
public interface BlogJpaRepository extends JpaRepository<BlogEntity, Long> {

    // === Command Repository 전용 메서드 (Soft Delete 지원) ===

    /**
     * 소프트 삭제 - 벌크 연산
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BlogEntity b SET b.deletedAt = :deletedAt, b.updatedAt = :deletedAt " +
            "WHERE b.id = :id AND b.deletedAt IS NULL")
    int bulkSoftDeleteById(@Param("id") Long id, @Param("deletedAt") OffsetDateTime deletedAt);

}