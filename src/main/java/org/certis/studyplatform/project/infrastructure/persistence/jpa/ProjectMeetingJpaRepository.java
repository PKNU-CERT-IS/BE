package org.certis.studyplatform.project.infrastructure.persistence.jpa;

import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectMeetingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

/**
 * Project Meeting JPA Repository
 *
 * ProjectMeetingEntity에 대한 JPA Repository
 * Infrastructure Layer
 */
@Repository
public interface ProjectMeetingJpaRepository extends JpaRepository<ProjectMeetingEntity, Long> {
    /**
     * 회의록 벌크 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProjectMeetingEntity m SET " +
            "m.title = :title, " +
            "m.content = :content, " +
            "m.participants = :participants, " +
            "m.updatedAt = :updatedAt " +
            "WHERE m.id = :id AND m.deletedAt IS NULL")
    int bulkUpdateMeeting(@Param("id") Long id,
                          @Param("title") String title,
                          @Param("content") String content,
                          @Param("participants") Long[] participants,
                          @Param("updatedAt") OffsetDateTime updatedAt);

    /**
     * 회의록 벌크 소프트 삭제
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProjectMeetingEntity m SET " +
            "m.deletedAt = :deletedAt, " +
            "m.updatedAt = :deletedAt " +
            "WHERE m.id = :id AND m.deletedAt IS NULL")
    int bulkSoftDeleteById(@Param("id") Long id, @Param("deletedAt") OffsetDateTime deletedAt);
}
