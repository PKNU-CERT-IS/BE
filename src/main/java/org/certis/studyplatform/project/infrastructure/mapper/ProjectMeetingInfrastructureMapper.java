package org.certis.studyplatform.project.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingSummaryVo;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingVo;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingLinkVo;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectMeetingEntity;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectMeetingLinkEntity;
import org.certis.studyplatform.project.presentation.dto.response.ProjectMeetingSummaryResponseDto;
import org.certis.studyplatform.shared.util.DataConverter;
import org.jooq.Record;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static org.certis.generated.jooq.Tables.*;

/**
 * Project Meeting Infrastructure Mapper
 *
 * ProjectMeetingEntity와 VO/DTO 간의 변환을 담당하는 매퍼
 * Infrastructure Layer
 */
@Component
@RequiredArgsConstructor
public class ProjectMeetingInfrastructureMapper {

    private final DataConverter dataConverter;

    /**
     * ProjectMeetingEntity를 ProjectMeetingVo로 변환 (Command Repository용)
     */
    public ProjectMeetingVo toVo(ProjectMeetingEntity entity, Long currentUserId) {
        if (entity == null) {
            return null;
        }

        // 현재 사용자가 작성자인지 확인하여 isEditable 설정
        boolean isEditable = currentUserId != null && currentUserId.equals(entity.getMemberId());

        return ProjectMeetingVo.of(
                entity.getId(),
                entity.getProjectId(),
                entity.getTitle(),
                entity.getContent(),
                List.of(entity.getParticipants()),
                entity.getMemberId(),
                isEditable,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ProjectMeetingVo toVo(Record record) {
        if (record == null) {
            return null;
        }

        Long writerId = record.get(PROJECT_MEETING.MEMBER_ID);
        boolean isEditable = PROJECT_MEETING.MEMBER_ID != null && PROJECT_MEETING.MEMBER_ID.equals(writerId);

        return ProjectMeetingVo.of(
                record.get(PROJECT_MEETING.ID),
                record.get(PROJECT_MEETING.PROJECT_ID),
                record.get(PROJECT_MEETING.TITLE),
                record.get(PROJECT_MEETING.CONTENT),
                dataConverter.convertToLongList(record.get(PROJECT_MEETING.PARTICIPANTS)),
                writerId,
                isEditable,
                record.get(PROJECT_MEETING.CREATED_AT),
                record.get(PROJECT_MEETING.UPDATED_AT)
        );
    }


    // ================================================================
    // PROJECT MEETING LINK MAPPING - 링크 변환
    // ================================================================

    /**
     * ProjectMeetingLinkEntity를 ProjectMeetingLinkVo로 변환
     */
    public ProjectMeetingLinkVo toLinkVo(ProjectMeetingLinkEntity entity) {
        if (entity == null) {
            return null;
        }

        return ProjectMeetingLinkVo.of(
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
     * ProjectMeetingLinkVo를 ProjectMeetingLinkEntity로 변환
     */
    public ProjectMeetingLinkEntity toLinkEntity(ProjectMeetingLinkVo vo) {
        if (vo == null) {
            return null;
        }

        return ProjectMeetingLinkEntity.builder()
                .id(vo.id())
                .projectId(vo.projectId())
                .memberId(vo.memberId())
                .name(vo.name())
                .attachedUrl(vo.attachedUrl())
                .build();
    }

    /**
     * ProjectMeetingLinkEntity 리스트를 ProjectMeetingLinkVo 리스트로 변환
     */
    public List<ProjectMeetingLinkVo> toLinkVoList(List<ProjectMeetingLinkEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(this::toLinkVo)
                .toList();
    }

    /**
     * ProjectMeetingEntity를 ProjectMeetingSummaryVo로 변환 (Query Service용)
     */
    public ProjectMeetingSummaryVo toSummaryVo(ProjectMeetingEntity entity, String creatorName, Long currentUserId) {
        if (entity == null) {
            return null;
        }

        // participants 배열의 길이를 participantNumber로 사용
        int participantNumber = entity.getParticipants() != null ? entity.getParticipants().length : 0;
        
        // 현재 사용자가 작성자인지 확인하여 isEditable 설정
        boolean isEditable = currentUserId != null && currentUserId.equals(entity.getMemberId());

        return ProjectMeetingSummaryVo.of(
                entity.getId(),
                entity.getTitle(),
                participantNumber,
                creatorName,
                isEditable
        );
    }

    /**
     * ProjectMeetingEntity 리스트를 ProjectMeetingSummaryVo 리스트로 변환 (Query Service용)
     */
    public List<ProjectMeetingSummaryVo> toSummaryVoList(List<ProjectMeetingEntity> entities, String creatorName, Long currentUserId) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(entity -> toSummaryVo(entity, creatorName, currentUserId))
                .toList();
    }

    /**
     * ProjectMeetingEntity를 ProjectMeetingSummaryResponseDto로 변환 (Legacy 호환용)
     */
    public ProjectMeetingSummaryResponseDto toSummaryResponseDto(ProjectMeetingEntity entity, String creatorName, Long currentUserId) {
        if (entity == null) {
            return null;
        }

        // participants 배열의 길이를 participantNumber로 사용
        int participantNumber = entity.getParticipants() != null ? entity.getParticipants().length : 0;
        
        // 현재 사용자가 작성자인지 확인하여 isEditable 설정
        boolean isEditable = currentUserId != null && currentUserId.equals(entity.getMemberId());

        return ProjectMeetingSummaryResponseDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .participantNumber(participantNumber)
                .creatorName(creatorName)
                .isEditable(isEditable)
                .build();
    }


    /**
     * ✅ jOOQ Record를 ProjectMeetingSummaryVo로 변환
     */
    public ProjectMeetingSummaryVo recordToSummaryVo(Record record) {
        // alias된 테이블에서 데이터 가져오기
        String[] participants = record.get("participants", String[].class);
        int participantCount = participants != null ? participants.length : 0;

        String writerName = record.get("writer_name", String.class);
        if (writerName == null || writerName.trim().isEmpty()) {
            writerName = "알 수 없음";
        }

        return ProjectMeetingSummaryVo.of(
                record.get("id", Long.class),
                record.get("title", String.class),
                participantCount,
                writerName,
                true // TODO: 실제로는 현재 사용자와 작성자 비교하여 편집 가능 여부 결정
        );
    }
} 