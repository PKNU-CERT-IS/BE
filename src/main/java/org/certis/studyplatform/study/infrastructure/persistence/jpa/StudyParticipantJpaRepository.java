package org.certis.studyplatform.study.infrastructure.persistence.jpa;

import org.certis.studyplatform.study.domain.StudyParticipantStatus;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Study Participant JPA Repository
 *
 * JPA 기반 프로젝트 참가자 데이터 접근
 * Command Repository 구현체에서 사용
 */
@Repository
public interface StudyParticipantJpaRepository extends JpaRepository<StudyParticipantEntity, Long> {

    /**
     * 기존 단일 조회 메서드 (Query Repository에서만 사용)
     */
    Optional<StudyParticipantEntity> findByIdAndDeletedAtIsNull(Long id);

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
}

