package org.certis.studyplatform.study.infrastructure.persistence.jpa;

import io.lettuce.core.dynamic.annotation.Param;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;

/**
 * Study JPA Repository
 *
 * Spring Data JPA 기반 데이터 액세스 인터페이스
 * CQRS Command 측면에서 사용되는 JPA Repository
 * Write 작업과 Command 검증에 필요한 메서드만 포함
 */
@Repository
public interface StudyJpaRepository extends JpaRepository<StudyEntity, Long> {

    // === Command Repository 전용 메서드 (Soft Delete 지원) ===

    /**
     * 소프트 삭제 - 벌크 연산
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StudyEntity p SET p.deletedAt = :deletedAt, p.updatedAt = :deletedAt " +
            "WHERE p.id = :id AND p.deletedAt IS NULL")
    int bulkSoftDeleteById(@Param("id") Long id, @Param("deletedAt") OffsetDateTime deletedAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE StudyEntity s SET s.resultSubmittedAt = :submittedAt, s.resultSubmitStatus = :status, s.resultAttachmentUrl = :attachmentUrl, s.updatedAt = :submittedAt WHERE s.id = :id AND s.deletedAt IS NULL")
    int updateResultSubmission(@Param("id") Long id,
                               @Param("submittedAt") OffsetDateTime submittedAt,
                               @Param("status") ResultSubmitStatus status,
                               @Param("attachmentUrl") String attachmentUrl);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE StudyEntity s SET s.resultSubmitStatus = :status, s.endedAt = :endedAt, s.updatedAt = :endedAt WHERE s.id = :id AND s.deletedAt IS NULL")
    int approveEnd(@Param("id") Long id,
                   @Param("endedAt") OffsetDateTime endedAt,
                   @Param("status") ResultSubmitStatus status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE StudyEntity s SET s.resultSubmitStatus = :status, s.resultAttachmentUrl = NULL, s.resultSubmittedAt = NULL, s.updatedAt = :now WHERE s.id = :id AND s.deletedAt IS NULL")
    int rejectEnd(@Param("id") Long id,
                  @Param("status") ResultSubmitStatus status,
                  @Param("now") OffsetDateTime now);

    /**
     * 스터디 생성 승인 - status를 APPROVED로 변경
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StudyEntity s SET s.status = 'APPROVED', s.startedAt = CASE WHEN s.startedAt > :now THEN :now ELSE s.startedAt END, s.updatedAt = :now WHERE s.id = :id AND s.deletedAt IS NULL")
    int approveCreation(@Param("id") Long id, @Param("now") OffsetDateTime now);
}