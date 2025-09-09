package org.certis.studyplatform.study.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.study.domain.vo.*;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyEntity;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyParticipantEntity;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.certis.generated.jooq.Tables.*;

/**
 * Study Infrastructure Mapper
 *
 * Clean Architecture Infrastructure Layer
 * Entity ↔ VO 변환 및 jOOQ Record → VO 변환
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StudyInfrastructureMapper {

    @Qualifier("jooqDataSource")
    private final DSLContext dsl;

    // ================================================================
    // COMMAND REPOSITORY 매핑 (Entity ↔ VO)
    // ================================================================

    /**
     * StudyVo를 StudyEntity로 변환 (저장용)
     */
    public StudyEntity toEntity(StudyVo vo) {
        if (vo == null) {
            return null;
        }

        return StudyEntity.builder()
                .id(vo.id())
                .memberId(vo.creatorId())
                .title(vo.title())
                .description(vo.description())
                .content(vo.content())
                .category(vo.category())
                .subcategory(vo.subCategory())
                .maxParticipantsNumber(vo.maxParticipants())
                .startedAt(vo.startDate())
                .endedAt(vo.endDate())
                .build();
    }

    /**
     * StudyEntity를 StudyVo로 변환 (조회용)
     * 연관관계(참여자, 회의록 등)는 포함되지 않음.
     */
    public StudyVo toVo(StudyEntity entity) {
        if (entity == null) {
            return null;
        }

        return StudyVo.of(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getContent(),
                entity.getCategory(),
                entity.getSubcategory(),
                entity.getStartedAt(),
                entity.getEndedAt(),
                entity.getMemberId(),
                null, // creatorName은 별도 조회 필요
                null, // creatorGrade는 별도 조회 필요
                entity.getMaxParticipantsNumber(),
                0 // currentParticipants는 별도 계산 필요
        );
    }

    // ================================================================
    // QUERY REPOSITORY 매핑 (jOOQ Record → VO)
    // ================================================================

    /**
     * jOOQ 조회 결과(Result<Record>)를 완전한 StudyVo로 변환합니다.
     * 스터디 상세 정보, 생성자 정보, 첨부파일, 참여자, 회의록 요약 목록을 모두 포함합니다.
     * 여러 테이블을 JOIN한 결과로 생성된 중복 행들을 그룹핑하여 하나의 StudyVo 객체를 생성합니다.
     */
    public StudyVo toFullStudyVoFromRecords(List<Record> records) {
        if (records == null || records.isEmpty()) {
            return null;
        }

        Record firstRecord = records.get(0);

        // 참여자 목록 그룹핑 및 생성
        Map<Long, StudyParticipantVo> participants = new LinkedHashMap<>();
        records.stream()
                .filter(r -> r.get("participant_id") != null)
                .forEach(r -> {
                    Long participantId = r.get("participant_id", Long.class);
                    participants.putIfAbsent(participantId, mapRecordToParticipantVo(r));
                });
        List<StudyParticipantVo> participantVos = new ArrayList<>(participants.values());

        // 회의록 요약 목록 그룹핑 및 생성
        Map<Long, StudyMeetingSummaryVo> summaries = new LinkedHashMap<>();
        records.stream()
                .filter(r -> r.get("meeting_id") != null)
                .forEach(r -> {
                    Long meetingId = r.get("meeting_id", Long.class);
                    summaries.putIfAbsent(meetingId, mapRecordToMeetingSummaryVo(r));
                });
        List<StudyMeetingSummaryVo> summaryVos = new ArrayList<>(summaries.values());

        // 첨부파일 목록 그룹핑 및 생성
        List<StudyAttachedVo> attachedVos = records.stream()
                .filter(r -> r.get("attached_id") != null)
                .map(this::mapRecordToStudyAttachedVo)
                .distinct()
                .collect(Collectors.toList());


        // ✅ [MODIFICATION] StudyVo 생성자 인자 순서 변경 (attached, summaryVoList, participantVoList)
        return new StudyVo(
                firstRecord.get("id", Long.class),
                firstRecord.get("title", String.class),
                firstRecord.get("description", String.class),
                firstRecord.get("content", String.class),
                firstRecord.get("category", String.class),
                firstRecord.get("subcategory", String.class),
                firstRecord.get("started_at", OffsetDateTime.class),
                firstRecord.get("ended_at", OffsetDateTime.class),
                firstRecord.get("member_id", Long.class),
                firstRecord.get("creator_name", String.class),
                safeParseMemberGrade(firstRecord.get("creator_grade", String.class)),
                firstRecord.get("max_participants_number", Integer.class),
                firstRecord.get("current_participants", Integer.class),
                attachedVos,
                summaryVos,
                participantVos
        );
    }

    /**
     * jOOQ 조회 결과(Result<Record>)를 StudyVo (첨부파일 포함)로 변환합니다.
     * 스터디 상세 정보와 첨부파일 목록을 포함합니다. 참여자나 회의록은 포함하지 않습니다.
     * 여러 테이블을 JOIN한 결과로 생성된 중복 행들을 그룹핑하여 하나의 StudyVo 객체를 생성합니다.
     */
    public StudyVo toStudyVoFromRecordsWithAttachments(List<Record> records) {
        if (records == null || records.isEmpty()) {
            return null;
        }

        Record firstRecord = records.get(0);

        // 첨부파일 목록 그룹핑 및 생성
        List<StudyAttachedVo> attachedVos = records.stream()
                .filter(r -> r.get("attached_id") != null)
                .map(this::mapRecordToStudyAttachedVo)
                .distinct() // 중복 제거
                .collect(Collectors.toList());

        // ✅ [MODIFICATION] StudyVo 생성자 인자 순서 변경 (attached, summaryVoList, participantVoList)
        return new StudyVo(
                firstRecord.get("id", Long.class),
                firstRecord.get("title", String.class),
                firstRecord.get("description", String.class),
                firstRecord.get("content", String.class),
                firstRecord.get("category", String.class),
                firstRecord.get("subcategory", String.class),
                firstRecord.get("started_at", OffsetDateTime.class),
                firstRecord.get("ended_at", OffsetDateTime.class),
                firstRecord.get("member_id", Long.class),
                firstRecord.get("creator_name", String.class),
                safeParseMemberGrade(firstRecord.get("creator_grade", String.class)),
                firstRecord.get("max_participants_number", Integer.class),
                firstRecord.get("current_participants", Integer.class),
                attachedVos,               // attached
                Collections.emptyList(),   // summaryVoList
                Collections.emptyList()    // participantVoList
        );
    }

    /**
     * Result<Record>를 StudySummaryVo 리스트로 변환 (Study ID별 그룹핑)
     * 리팩토링된 Repository의 핵심 변환 메서드
     */
    public List<StudySummaryVo> groupRecordsByStudyIdToSummaryVos(List<Record> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        // Study ID별로 Record 그룹핑
        Map<Long, List<Record>> recordsByStudyId = records.stream()
                .collect(Collectors.groupingBy(record -> record.get("id", Long.class)));

        return recordsByStudyId.entrySet().stream()
                .map(entry -> convertRecordGroupToStudySummaryVo(entry.getKey(), entry.getValue()))
                .filter(Objects::nonNull) // null 제거
                .collect(Collectors.toList());
    }

    /**
     * 그룹핑된 Record들을 StudySummaryVo로 변환
     */
    private StudySummaryVo convertRecordGroupToStudySummaryVo(Long studyId, List<Record> records) {
        if (records.isEmpty()) {
            return null;
        }

        Record firstRecord = records.get(0);

        // StudyAttached 정보 추출 및 변환
        List<StudyAttachedVo> attachedVos = records.stream()
                .filter(record -> record.get("attached_id") != null)
                .map(this::mapRecordToStudyAttachedVo)
                .distinct()
                .collect(Collectors.toList());

        boolean isParticipantable = determineParticipantable(
                firstRecord.get(STUDY.STARTED_AT),
                firstRecord.get(STUDY.ENDED_AT),
                firstRecord.get(STUDY.MAX_PARTICIPANTS_NUMBER),
                firstRecord.get("current_participants", Integer.class)
        );

        MemberGrade memberGrade = safeParseMemberGrade(firstRecord.get("creator_grade", String.class));

        return StudySummaryVo.of(
                studyId,
                firstRecord.get("title", String.class),
                firstRecord.get("description", String.class),
                firstRecord.get("category", String.class),
                firstRecord.get("subcategory", String.class),
                firstRecord.get("started_at", OffsetDateTime.class),
                firstRecord.get("ended_at", OffsetDateTime.class),
                firstRecord.get("creator_name", String.class),
                memberGrade,
                isParticipantable,
                attachedVos
        );
    }

    /**
     * Record를 StudyAttachedVo로 변환 (헬퍼 메서드)
     */
    private StudyAttachedVo mapRecordToStudyAttachedVo(Record record) {
        return StudyAttachedVo.of(
                record.get("attached_id", Long.class),
                record.get("attached_name", String.class),
                record.get("attached_type", String.class),
                record.get("attached_size", String.class),
                record.get("attached_url", String.class)
        );
    }

    /**
     * 참여 가능 여부 판단 로직
     */
    private boolean determineParticipantable(OffsetDateTime startedAt, OffsetDateTime endedAt,
                                             Integer maxParticipants, Integer currentParticipants) {
        OffsetDateTime now = OffsetDateTime.now();

        if (endedAt != null && endedAt.isBefore(now)) {
            return false;
        }

        if (maxParticipants != null && currentParticipants != null) {
            return currentParticipants < maxParticipants;
        }
        return true;
    }

    // ================================================================
    // STUDY PARTICIPANT 매핑 메서드들
    // ================================================================

    public StudyParticipantEntity toEntity(StudyParticipantVo vo) {
        return StudyParticipantEntity.builder()
                .id(vo.id())
                .studyId(vo.studyId())
                .memberId(vo.memberId())
                .status(vo.status())
                .createdAt(vo.createdAt())
                .updatedAt(vo.updatedAt())
                .build();
    }

    public StudyParticipantVo toVo(StudyParticipantEntity entity) {
        return new StudyParticipantVo(
                entity.getId(),
                entity.getStudyId(),
                entity.getMemberId(),
                null, // memberName은 별도 조회 필요
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * StudyParticipantEntity -> StudyParticipantCreatedVo 변환
     */
    public StudyParticipantCreatedVo toCreatedVo(StudyParticipantEntity entity) {
        if (entity == null) {
            return null;
        }
        return StudyParticipantCreatedVo.of(
                entity.getId(),
                entity.getStudyId(),
                entity.getMemberId(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    // ... Other existing participant mapping methods ...

    /**
     * jOOQ Record -> StudyParticipantVo 매핑
     * Repository에서 단건 조회 시 사용 (별칭: member_name)
     */
    public StudyParticipantVo toVoFromRecord(Record record) {
        if (record == null) {
            return null;
        }

        return new StudyParticipantVo(
                record.get("id", Long.class),
                record.get("study_id", Long.class),
                record.get("member_id", Long.class),
                record.get("member_name", String.class),
                record.get("status", org.certis.studyplatform.study.domain.StudyParticipantStatus.class),
                record.get("created_at", OffsetDateTime.class),
                record.get("updated_at", OffsetDateTime.class)
        );
    }

    /**
     * jOOQ Record -> StudyParticipantSummaryVo 매핑
     * Repository에서 목록 조회 시 사용 (별칭: member_name)
     */
    public StudyParticipantSummaryVo toSummaryVoFromRecord(Record record) {
        if (record == null) {
            return null;
        }

        return new StudyParticipantSummaryVo(
                record.get("id", Long.class),
                record.get("member_id", Long.class),
                record.get("member_name", String.class),
                record.get("status", org.certis.studyplatform.study.domain.StudyParticipantStatus.class),
                record.get("created_at", OffsetDateTime.class)
        );
    }

    // ================================================================
    // PRIVATE HELPER METHODS
    // ================================================================

    private StudyParticipantVo mapRecordToParticipantVo(Record record) {
        return new StudyParticipantVo(
                record.get("participant_id", Long.class),
                record.get("id", Long.class),
                record.get("participant_member_id", Long.class),
                record.get("participant_name", String.class),
                record.get("participant_status", org.certis.studyplatform.study.domain.StudyParticipantStatus.class),
                record.get("participant_created_at", OffsetDateTime.class),
                record.get("participant_updated_at", OffsetDateTime.class)
        );
    }

    private StudyMeetingSummaryVo mapRecordToMeetingSummaryVo(Record record) {
        // study_meeting_id가 null이면 회의록 정보가 없는 것이므로 null 반환
        if (record.get("meeting_id") == null) {
            return null;
        }

        return new StudyMeetingSummaryVo(
                record.get("meeting_id", Long.class),
                record.get("meeting_title", String.class),
                record.get("meeting_participant_number", Integer.class),
                record.get("meeting_creator_name", String.class),
                record.get("meeting_is_editable", boolean.class),
                record.get("meeting_created_at", OffsetDateTime.class),
                record.get("meeting_attached_url", String.class),
                record.get("meeting_attached_title", String.class)
        );
    }

    private MemberGrade safeParseMemberGrade(String grade) {
        if (grade == null) {
            return null;
        }
        try {
            return MemberGrade.valueOf(grade.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid member grade string: {}", grade);
            return null;
        }
    }
}