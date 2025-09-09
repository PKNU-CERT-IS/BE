package org.certis.studyplatform.study.infrastructure.mapper;

import org.certis.studyplatform.study.domain.vo.StudyMeetingLinkVo;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyMeetingLinkEntity;
import org.jooq.Record;
import org.springframework.stereotype.Component;

import static org.certis.generated.jooq.Tables.STUDY_MEETING_LINK;

/**
 * StudyMeetingLink Infrastructure Mapper
 *
 * Infrastructure Layer
 * VO ↔ Entity 변환 및 jOOQ Record → VO 변환 담당
 */
@Component
public class StudyMeetingLinkInfrastructureMapper {

    /**
     * VO → Entity 변환 (생성용)
     */
    public StudyMeetingLinkEntity toEntity(StudyMeetingLinkVo vo) {
        return StudyMeetingLinkEntity.builder()
                .id(vo.id())
                .studyId(vo.studyId())
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
    public StudyMeetingLinkVo toVo(StudyMeetingLinkEntity entity) {
        return new StudyMeetingLinkVo(
                entity.getId(),
                entity.getStudyId(),
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
    public StudyMeetingLinkVo toVoFromRecord(Record record) {
        var pml = STUDY_MEETING_LINK;

        return new StudyMeetingLinkVo(
                record.get(pml.ID),
                record.get(pml.STUDY_ID),
                record.get(pml.MEMBER_ID),
                record.get(pml.NAME),
                record.get(pml.ATTACHED_URL),
                record.get(pml.CREATED_AT),
                record.get(pml.UPDATED_AT)
        );
    }
}