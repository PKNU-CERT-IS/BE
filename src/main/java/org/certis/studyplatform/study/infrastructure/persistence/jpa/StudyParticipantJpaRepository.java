package org.certis.studyplatform.study.infrastructure.persistence.jpa;

import org.certis.studyplatform.study.domain.StudyParticipantStatus;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

/**
 * Study Participant JPA Repository
 *
 * JPA 기반 프로젝트 참가자 데이터 접근
 * Command Repository 구현체에서 사용
 */
@Repository
public interface StudyParticipantJpaRepository extends JpaRepository<StudyParticipantEntity, Long> {

    /**
     * 참가자 상태 벌크 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StudyParticipantEntity p SET " +
            "p.status = :status, " +
            "p.updatedAt = :updatedAt " +
            "WHERE p.id = :id AND p.deletedAt IS NULL")
    int bulkUpdateStatus(@Param("id") Long id,
                         @Param("status") StudyParticipantStatus status,
                         @Param("updatedAt") OffsetDateTime updatedAt);

    /**
     * 거절 처리: 상태를 REJECTED로 변경하고 소프트 삭제 표시(deletedAt)까지 함께 설정
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StudyParticipantEntity p SET " +
            "p.status = org.certis.studyplatform.study.domain.StudyParticipantStatus.REJECTED, " +
            "p.updatedAt = :deletedAt, " +
            "p.deletedAt = :deletedAt " +
            "WHERE p.id = :id AND p.deletedAt IS NULL")
    int bulkRejectWithSoftDelete(@Param("id") Long id,
                                 @Param("deletedAt") OffsetDateTime deletedAt);

    /**
     * 프로젝트 + 멤버별 참가자 벌크 소프트 삭제
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StudyParticipantEntity p SET " +
            "p.deletedAt = :deletedAt, " +
            "p.updatedAt = :deletedAt " +
            "WHERE p.studyId = :studyId AND p.memberId = :memberId AND p.deletedAt IS NULL")
    int bulkSoftDeleteByStudyIdAndMemberId(@Param("studyId") Long studyId,
                                             @Param("memberId") Long memberId,
                                             @Param("deletedAt") OffsetDateTime deletedAt);

    /**
     * 프로젝트별 모든 참가자 벌크 소프트 삭제
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StudyParticipantEntity p SET " +
            "p.deletedAt = :deletedAt, " +
            "p.updatedAt = :deletedAt " +
            "WHERE p.studyId = :studyId AND p.deletedAt IS NULL")
    int bulkSoftDeleteByStudyId(@Param("studyId") Long studyId,
                                  @Param("deletedAt") OffsetDateTime deletedAt);

    /**
     * 단건 하드 삭제 (승인 취소 등)
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM StudyParticipantEntity p WHERE p.id = :id")
    int hardDeleteById(@Param("id") Long id);

    /**
     * 단건 소프트 삭제 (거절 등)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StudyParticipantEntity p SET p.deletedAt = :deletedAt, p.updatedAt = :deletedAt WHERE p.id = :id AND p.deletedAt IS NULL")
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") OffsetDateTime deletedAt);

    /**
     * 소프트 삭제된 참가 신청 중 최신 1건만 복원 (deleted_at IS NOT NULL 중 updated_at DESC LIMIT 1)
     * JPA JPQL은 LIMIT를 지원하지 않으므로 nativeQuery 사용
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE study_participant p SET deleted_at = NULL, updated_at = :updatedAt " +
            "WHERE p.id = (SELECT id FROM study_participant WHERE study_id = :studyId AND member_id = :memberId " +
            "AND deleted_at IS NOT NULL ORDER BY updated_at DESC LIMIT 1)", nativeQuery = true)
    int restoreLatestByStudyIdAndMemberId(@Param("studyId") Long studyId,
                                          @Param("memberId") Long memberId,
                                          @Param("updatedAt") OffsetDateTime updatedAt);

    /**
     * 복원 직후 최신 활성 레코드 조회 (deletedAt IS NULL)
     */
    java.util.Optional<StudyParticipantEntity> findTopByStudyIdAndMemberIdAndDeletedAtIsNullOrderByUpdatedAtDesc(
            Long studyId,
            Long memberId
    );
}

