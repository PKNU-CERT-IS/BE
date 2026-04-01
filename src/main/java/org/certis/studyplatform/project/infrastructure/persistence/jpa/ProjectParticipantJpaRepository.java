package org.certis.studyplatform.project.infrastructure.persistence.jpa;

import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Project Participant JPA Repository
 *
 * JPA 기반 프로젝트 참가자 데이터 접근
 * Command Repository 구현체에서 사용
 */
@Repository
public interface ProjectParticipantJpaRepository extends JpaRepository<ProjectParticipantEntity, Long> {

    /**
     * 기존 단일 조회 메서드 (Query Repository에서만 사용)
     */
    Optional<ProjectParticipantEntity> findByIdAndDeletedAtIsNull(Long id);

    /**
     * 참가자 상태 벌크 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProjectParticipantEntity p SET " +
            "p.status = :status, " +
            "p.updatedAt = :updatedAt " +
            "WHERE p.id = :id AND p.deletedAt IS NULL AND p.status = org.certis.studyplatform.project.domain.ProjectParticipantStatus.PENDING")
    int bulkUpdateStatus(@Param("id") Long id,
                         @Param("status") ProjectParticipantStatus status,
                         @Param("updatedAt") OffsetDateTime updatedAt);

    /**
     * 거절 처리: 상태를 REJECTED로 변경하고 소프트 삭제 표시(deletedAt)까지 함께 설정
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProjectParticipantEntity p SET " +
            "p.status = org.certis.studyplatform.project.domain.ProjectParticipantStatus.REJECTED, " +
            "p.updatedAt = :deletedAt, " +
            "p.deletedAt = :deletedAt " +
            "WHERE p.id = :id AND p.deletedAt IS NULL AND p.status = org.certis.studyplatform.project.domain.ProjectParticipantStatus.PENDING")
    int bulkRejectWithSoftDelete(@Param("id") Long id,
                                 @Param("deletedAt") OffsetDateTime deletedAt);

    /**
     * 프로젝트 + 멤버별 참가자 벌크 소프트 삭제
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProjectParticipantEntity p SET " +
            "p.deletedAt = :deletedAt, " +
            "p.updatedAt = :deletedAt " +
            "WHERE p.projectId = :projectId AND p.memberId = :memberId AND p.deletedAt IS NULL")
    int bulkSoftDeleteByProjectIdAndMemberId(@Param("projectId") Long projectId,
                                             @Param("memberId") Long memberId,
                                             @Param("deletedAt") OffsetDateTime deletedAt);

    /**
     * 프로젝트별 모든 참가자 벌크 소프트 삭제
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProjectParticipantEntity p SET " +
            "p.deletedAt = :deletedAt, " +
            "p.updatedAt = :deletedAt " +
            "WHERE p.projectId = :projectId AND p.deletedAt IS NULL")
    int bulkSoftDeleteByProjectId(@Param("projectId") Long projectId,
                                  @Param("deletedAt") OffsetDateTime deletedAt);

    /**
     * 단건 하드 삭제 (승인 취소 등)
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ProjectParticipantEntity p WHERE p.id = :id")
    int hardDeleteById(@Param("id") Long id);

    /**
     * 단건 소프트 삭제 (거절 등)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProjectParticipantEntity p SET p.deletedAt = :deletedAt, p.updatedAt = :deletedAt WHERE p.id = :id AND p.deletedAt IS NULL")
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") OffsetDateTime deletedAt);

    /**
     * 소프트 삭제된 참가 신청 중 최신 1건만 복원 (deleted_at IS NOT NULL 중 updated_at DESC LIMIT 1)
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE project_participant p SET deleted_at = NULL, updated_at = :updatedAt, status = 'PENDING' " +
            "WHERE p.id = (SELECT id FROM project_participant WHERE project_id = :projectId AND member_id = :memberId " +
            "AND deleted_at IS NOT NULL ORDER BY updated_at DESC LIMIT 1)", nativeQuery = true)
    int restoreLatestByProjectIdAndMemberId(@Param("projectId") Long projectId,
                                            @Param("memberId") Long memberId,
                                            @Param("updatedAt") OffsetDateTime updatedAt);
    /**
     * 소프트 삭제된 참가 신청 복원 (deletedAt = NULL, updatedAt = NOW())
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProjectParticipantEntity p SET p.deletedAt = NULL, p.updatedAt = :updatedAt, p.status = org.certis.studyplatform.project.domain.ProjectParticipantStatus.PENDING " +
            "WHERE p.projectId = :projectId AND p.memberId = :memberId AND p.deletedAt IS NOT NULL")
    int restoreByProjectIdAndMemberId(@Param("projectId") Long projectId,
                                      @Param("memberId") Long memberId,
                                      @Param("updatedAt") OffsetDateTime updatedAt);

}
