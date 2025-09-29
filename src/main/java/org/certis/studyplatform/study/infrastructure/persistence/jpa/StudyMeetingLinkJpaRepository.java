package org.certis.studyplatform.study.infrastructure.persistence.jpa;

import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyMeetingLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

/**
 * Study Meeting Link JPA Repository
 *
 * StudyMeetingLinkEntity에 대한 JPA Repository
 * Infrastructure Layer
 */
@Repository
public interface StudyMeetingLinkJpaRepository extends JpaRepository<StudyMeetingLinkEntity, Long> {

    /**
     * 프로젝트별 링크 소프트 삭제 (쓰기 작업) - 개선된 버전
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StudyMeetingLinkEntity e SET " +
            "e.deletedAt = :deletedAt, " +
            "e.updatedAt = :deletedAt " +
            "WHERE e.meetingId = :meetingId AND e.deletedAt IS NULL")
    int bulkSoftDeleteByStudyId(@Param("meetingId") Long meetingId,
                                  @Param("deletedAt") OffsetDateTime deletedAt);

    /**
     * 회원별 링크 소프트 삭제 (쓰기 작업) - 개선된 버전
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StudyMeetingLinkEntity e SET " +
            "e.deletedAt = :deletedAt, " +
            "e.updatedAt = :deletedAt " +
            "WHERE e.memberId = :memberId AND e.deletedAt IS NULL")
    int bulkSoftDeleteByMemberId(@Param("memberId") Long memberId,
                                 @Param("deletedAt") OffsetDateTime deletedAt);

    /**
     * 특정 링크 소프트 삭제 (쓰기 작업) - 개선된 버전
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StudyMeetingLinkEntity e SET " +
            "e.deletedAt = :deletedAt, " +
            "e.updatedAt = :deletedAt " +
            "WHERE e.id = :id AND e.deletedAt IS NULL")
    int bulkSoftDeleteById(@Param("id") Long id,
                           @Param("deletedAt") OffsetDateTime deletedAt);
}