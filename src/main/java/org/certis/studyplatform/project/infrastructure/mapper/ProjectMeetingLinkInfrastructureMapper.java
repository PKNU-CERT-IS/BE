package org.certis.studyplatform.project.infrastructure.mapper;

import org.certis.studyplatform.project.domain.vo.ProjectMeetingLinkVo;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectMeetingLinkEntity;
import org.jooq.Record;
import org.springframework.stereotype.Component;

import static org.certis.generated.jooq.Tables.*;

/**
 * ProjectMeetingLink Infrastructure Mapper
 *
 * Infrastructure Layer
 * VO ↔ Entity 변환 및 jOOQ Record → VO 변환 담당
 */
@Component
public class ProjectMeetingLinkInfrastructureMapper {

    /**
     * VO → Entity 변환 (생성용)
     */
    public ProjectMeetingLinkEntity toEntity(ProjectMeetingLinkVo vo) {
        return ProjectMeetingLinkEntity.builder()
                .id(vo.id())
                .projectId(vo.projectId())
                .memberId(vo.memberId())
                .name(vo.name())
                .attachedUrl(vo.attachedUrl())
                .createdAt(vo.createdAt())
                .updatedAt(vo.updatedAt())
                .build();
    }

    /**
     * Entity → VO 변환
     */
    public ProjectMeetingLinkVo toVo(ProjectMeetingLinkEntity entity) {
        return new ProjectMeetingLinkVo(
                entity.getId(),
                entity.getProjectId(),
                entity.getMemberId(),
                entity.getName(),
                entity.getAttachedUrl(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * jOOQ Record → VO 변환
     */
    public ProjectMeetingLinkVo toVoFromRecord(Record record) {
        var pml = PROJECT_MEETING_LINK;

        return new ProjectMeetingLinkVo(
                record.get(pml.ID),
                record.get(pml.PROJECT_ID),
                record.get(pml.MEMBER_ID),
                record.get(pml.NAME),
                record.get(pml.ATTACHED_URL),
                record.get(pml.CREATED_AT),
                record.get(pml.UPDATED_AT)
        );
    }
}