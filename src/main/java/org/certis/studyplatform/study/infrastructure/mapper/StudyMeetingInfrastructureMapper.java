package org.certis.studyplatform.study.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.study.domain.vo.StudyMeetingLinkVo;
import org.certis.studyplatform.study.domain.vo.StudyMeetingSummaryVo;
import org.certis.studyplatform.study.domain.vo.StudyMeetingVo;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyMeetingEntity;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyMeetingLinkEntity;
import org.jooq.Record;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.certis.generated.jooq.Tables.STUDY_MEETING;

/**
 * Study Meeting Infrastructure Mapper
 *
 * StudyMeetingEntity와 VO/DTO 간의 변환을 담당하는 매퍼
 * Infrastructure Layer
 */
@Component
@RequiredArgsConstructor
public class StudyMeetingInfrastructureMapper {

    /**
     * StudyMeetingEntity를 StudyMeetingVo로 변환 (Command Repository용)
     */
    public StudyMeetingVo toVo(StudyMeetingEntity entity, Long currentUserId) {
        if (entity == null) {
            return null;
        }

        // 현재 사용자가 작성자인지 확인하여 isEditable 설정
        boolean isEditable = currentUserId != null && currentUserId.equals(entity.getMemberId());

        return StudyMeetingVo.of(
                entity.getId(),
                entity.getStudyId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getParticipants() != null ? entity.getParticipants().length : 0,
                entity.getMemberId(),
                isEditable,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public StudyMeetingVo toVo(Record record) {
        if (record == null) {
            return null;
        }

        Long writerId = record.get(STUDY_MEETING.MEMBER_ID);
        boolean isEditable = STUDY_MEETING.MEMBER_ID != null && STUDY_MEETING.MEMBER_ID.equals(writerId);

        return StudyMeetingVo.of(
                record.get(STUDY_MEETING.ID),
                record.get(STUDY_MEETING.STUDY_ID),
                record.get(STUDY_MEETING.TITLE),
                record.get(STUDY_MEETING.CONTENT),
                record.get(STUDY_MEETING.PARTICIPANTS) != null ? record.get(STUDY_MEETING.PARTICIPANTS).length : 0,
                writerId,
                isEditable,
                record.get(STUDY_MEETING.CREATED_AT),
                record.get(STUDY_MEETING.UPDATED_AT)
        );
    }


    // ================================================================
    // STUDY MEETING LINK MAPPING - 링크 변환
    // ================================================================

    /**
     * StudyMeetingLinkEntity를 StudyMeetingLinkVo로 변환
     */
    public StudyMeetingLinkVo toLinkVo(StudyMeetingLinkEntity entity) {
        if (entity == null) {
            return null;
        }

        return StudyMeetingLinkVo.of(
                entity.getId(),
                entity.getMeetingId(),
                entity.getMemberId(),
                entity.getName(),
                entity.getAttachedUrl(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * StudyMeetingLinkVo를 StudyMeetingLinkEntity로 변환
     */
    public StudyMeetingLinkEntity toLinkEntity(StudyMeetingLinkVo vo) {
        if (vo == null) {
            return null;
        }

        return StudyMeetingLinkEntity.builder()
                .id(vo.id())
                .meetingId(vo.meetingId())
                .memberId(vo.memberId())
                .name(vo.name())
                .attachedUrl(vo.attachedUrl())
                .build();
    }

    /**
     * StudyMeetingLinkEntity 리스트를 StudyMeetingLinkVo 리스트로 변환
     */
    public List<StudyMeetingLinkVo> toLinkVoList(List<StudyMeetingLinkEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(this::toLinkVo)
                .toList();
    }

    /**
     * StudyMeetingEntity를 StudyMeetingSummaryVo로 변환 (Query Service용)
     */
    public StudyMeetingSummaryVo toSummaryVo(StudyMeetingEntity entity, String creatorName, Long currentUserId) {
        if (entity == null) {
            return null;
        }

        // participants 배열의 길이를 participantNumber로 사용
        int participantNumber = entity.getParticipants() != null ? entity.getParticipants().length : 0;
        
        // 현재 사용자가 작성자인지 확인하여 isEditable 설정
        boolean isEditable = currentUserId != null && currentUserId.equals(entity.getMemberId());

        return StudyMeetingSummaryVo.of(
                entity.getId(),
                entity.getTitle(),
                participantNumber,
                creatorName,
                isEditable,
                entity.getCreatedAt(),
                null,
                null
        );
    }

    /**
     * StudyMeetingEntity 리스트를 StudyMeetingSummaryVo 리스트로 변환 (Query Service용)
     */
    public List<StudyMeetingSummaryVo> toSummaryVoList(List<StudyMeetingEntity> entities, String creatorName, Long currentUserId) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(entity -> toSummaryVo(entity, creatorName, currentUserId))
                .toList();
    }


    /**
     * ✅ jOOQ Record를 StudyMeetingSummaryVo로 변환
     */
    public StudyMeetingSummaryVo recordToSummaryVo(Record record) {
        return recordToSummaryVo(record, null);
    }

    /**
     * ✅ jOOQ Record를 StudyMeetingSummaryVo로 변환 (현재 사용자 ID 포함)
     */
    public StudyMeetingSummaryVo recordToSummaryVo(Record record, Long currentUserId) {
        if (record == null) {
            return null;
        }

        // alias된 테이블에서 데이터 가져오기
        String[] participants = record.get("participants", String[].class);
        int participantCount = participants != null ? participants.length : 0;

        String writerName = record.get("writer_name", String.class);
        if (writerName == null || writerName.trim().isEmpty()) {
            writerName = "알 수 없음";
        }

        // 현재 사용자와 작성자 비교하여 편집 가능 여부 결정
        Long writerId = record.get("writer_id", Long.class);
        boolean isEditable = currentUserId != null && writerId != null && currentUserId.equals(writerId);

        OffsetDateTime createdAt = record.get("created_at", OffsetDateTime.class);

        // 서브쿼리에서 제공하는 첨부 링크 정보 (없으면 null)
        String meetingAttachedUrl = record.get("attached_url", String.class);
        String meetingAttachedTitle = record.get("attached_title", String.class);

        return StudyMeetingSummaryVo.of(
                record.get("id", Long.class),
                record.get("title", String.class),
                participantCount,
                writerName,
                isEditable,
                createdAt,
                meetingAttachedUrl,
                meetingAttachedTitle
        );
    }
} 