package org.certis.studyplatform.study.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.repository.StudyMeetingQueryRepository;
import org.certis.studyplatform.study.domain.vo.StudyMeetingSummaryVo;
import org.certis.studyplatform.study.domain.vo.StudyMeetingVo;
import org.certis.studyplatform.study.infrastructure.mapper.StudyMeetingInfrastructureMapper;
import org.jooq.DSLContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static org.certis.generated.jooq.Tables.*;

/**
 * Study Meeting Query Repository Implementation using Generated jOOQ Tables
 *
 * ✅ jOOQ 생성 코드 활용:
 * - 타입 안전한 테이블/필드 참조
 * - 컴파일 타임 검증
 * - IDE 완벽 지원 (자동완성, 리팩토링)
 * - 스키마 변경 시 자동 감지
 *
 * ✅ CQRS 패턴에서 Query 전용 Repository
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class StudyMeetingQueryRepositoryImpl implements StudyMeetingQueryRepository {

    private final DSLContext dsl;
    private final StudyMeetingInfrastructureMapper mapper;

    @Override
    public Optional<StudyMeetingVo> findById(Long meetingId) {
        log.info("jOOQ: Finding study meeting by ID - {}", meetingId);

        Optional<StudyMeetingVo> result = dsl.select(
                        STUDY_MEETING.ID,
                        STUDY_MEETING.STUDY_ID,
                        STUDY_MEETING.TITLE,
                        STUDY_MEETING.CONTENT,
                        STUDY_MEETING.PARTICIPANTS,
                        STUDY_MEETING.MEMBER_ID,
                        STUDY_MEETING.CREATED_AT,
                        STUDY_MEETING.UPDATED_AT
                )
                .from(STUDY_MEETING)
                .where(STUDY_MEETING.ID.eq(meetingId))
                .and(STUDY_MEETING.DELETED_AT.isNull())
                .fetchOptional(record -> mapper.toVo(record));

        log.info("jOOQ: Study meeting found - ID: {}", meetingId);
        return result;
    }

    @Override
    public Page<StudyMeetingSummaryVo> findByStudyId(Long studyId, Pageable pageable) {
        log.info("jOOQ: Clean conversion - studyId: {}", studyId);

        validateStudyExists(studyId);

        var sm = STUDY_MEETING.as("sm");
        var m = MEMBER.as("m");

        // 전체 개수
        int totalCount = dsl.selectCount()
                .from(sm)
                .where(sm.STUDY_ID.eq(studyId))
                .and(sm.DELETED_AT.isNull())
                .fetchOne(0, int.class);

        List<StudyMeetingSummaryVo> summaryVos = dsl.select(
                        sm.ID,
                        sm.TITLE,
                        sm.PARTICIPANTS,
                        sm.MEMBER_ID.as("writer_id"),
                        sm.CREATED_AT,
                        m.NAME.as("writer_name"),
                        // 최근 생성된 링크 1건의 URL/Title 서브쿼리로 조회
                        dsl.select(STUDY_MEETING_LINK.ATTACHED_URL)
                                .from(STUDY_MEETING_LINK)
                                .where(STUDY_MEETING_LINK.STUDY_ID.eq(sm.STUDY_ID))
                                .and(STUDY_MEETING_LINK.DELETED_AT.isNull())
                                .orderBy(STUDY_MEETING_LINK.CREATED_AT.desc())
                                .limit(1)
                                .asField("attached_url"),
                        dsl.select(STUDY_MEETING_LINK.NAME)
                                .from(STUDY_MEETING_LINK)
                                .where(STUDY_MEETING_LINK.STUDY_ID.eq(sm.STUDY_ID))
                                .and(STUDY_MEETING_LINK.DELETED_AT.isNull())
                                .orderBy(STUDY_MEETING_LINK.CREATED_AT.desc())
                                .limit(1)
                                .asField("attached_title")
                )
                .from(sm)
                .leftJoin(m).on(sm.MEMBER_ID.eq(m.ID))
                .where(sm.STUDY_ID.eq(studyId))
                .and(sm.DELETED_AT.isNull())
                .orderBy(sm.CREATED_AT.desc())
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetch(mapper::recordToSummaryVo);

        return new PageImpl<>(summaryVos, pageable, totalCount);
    }

    @Override
    public boolean existsById(Long meetingId) {
        log.info("jOOQ: Checking if study meeting exists - ID: {}", meetingId);

        // ✅ jOOQ 생성 테이블로 존재 여부 확인
        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(STUDY_MEETING)
                        .where(STUDY_MEETING.ID.eq(meetingId))
                        .and(STUDY_MEETING.DELETED_AT.isNull())
        );

        log.info("jOOQ: Study meeting exists: {} - ID: {}", exists, meetingId);
        return exists;
    }

    @Override
    public boolean hasEditPermission(Long meetingId, Long requesterId) {
        log.info("jOOQ: Checking edit permission - meetingId: {}, requesterId: {}", meetingId, requesterId);

        // ✅ jOOQ 생성 테이블로 권한 확인
        boolean hasPermission = dsl.fetchExists(
                dsl.selectOne()
                        .from(STUDY_MEETING)
                        .where(STUDY_MEETING.ID.eq(meetingId))
                        .and(STUDY_MEETING.MEMBER_ID.eq(requesterId))
                        .and(STUDY_MEETING.DELETED_AT.isNull())
        );

        log.info("jOOQ: Edit permission: {} - meetingId: {}, requesterId: {}", hasPermission, meetingId, requesterId);
        return hasPermission;
    }
    // ================= Private Helper Methods =================

    /**
     * ✅ jOOQ 생성 테이블로 프로젝트 존재 여부 검증
     */
    private boolean validateStudyExists(Long studyId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(STUDY)
                        .where(STUDY.ID.eq(studyId))
                        .and(STUDY.DELETED_AT.isNull())
        );
    }

}