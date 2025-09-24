package org.certis.studyplatform.study.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.repository.StudyMeetingLinkQueryRepository;
import org.certis.studyplatform.study.domain.vo.StudyMeetingLinkVo;
import org.certis.studyplatform.study.infrastructure.mapper.StudyMeetingLinkInfrastructureMapper;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.certis.generated.jooq.Tables.STUDY_MEETING_LINK;
import static org.certis.generated.jooq.Tables.STUDY_MEETING;

/**
 * StudyMeetingLink Query Repository Implementation using Generated jOOQ Tables
 *
 * ✅ CQRS 엄격 적용: Query는 조회 작업만 담당 (Read Only)
 * ✅ jOOQ 생성 코드 활용으로 타입 안전성 보장
 * Infrastructure Layer
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudyMeetingLinkQueryRepositoryImpl implements StudyMeetingLinkQueryRepository {

    @Qualifier("jooqDataSource")
    private final DSLContext dsl;

    private final StudyMeetingLinkInfrastructureMapper mapper;

    /**
     * ID로 링크 조회
     */
    @Override
    public Optional<StudyMeetingLinkVo> findById(Long linkId) {
        log.info("jOOQ: Finding study meeting link by ID - {}", linkId);

        Optional<StudyMeetingLinkVo> result = dsl.select(
                        STUDY_MEETING_LINK.ID,
                        STUDY_MEETING_LINK.MEETING_ID,
                        STUDY_MEETING_LINK.MEMBER_ID,
                        STUDY_MEETING_LINK.NAME,
                        STUDY_MEETING_LINK.ATTACHED_URL,
                        STUDY_MEETING_LINK.CREATED_AT,
                        STUDY_MEETING_LINK.UPDATED_AT
                )
                .from(STUDY_MEETING_LINK)
                .where(STUDY_MEETING_LINK.ID.eq(linkId))
                .and(STUDY_MEETING_LINK.DELETED_AT.isNull())
                .fetchOptional(mapper::toVoFromRecord);

        log.info("jOOQ: Study meeting link found - ID: {}", linkId);
        return result;
    }

    /**
     * 프로젝트별 링크 목록 조회
     */
    @Override
    public List<StudyMeetingLinkVo> findByStudyId(Long studyId) {
        log.info("jOOQ: Finding study meeting links by studyId - {}", studyId);

        List<StudyMeetingLinkVo> result = dsl.select(
                        STUDY_MEETING_LINK.ID,
                        STUDY_MEETING_LINK.MEETING_ID,
                        STUDY_MEETING_LINK.MEMBER_ID,
                        STUDY_MEETING_LINK.NAME,
                        STUDY_MEETING_LINK.ATTACHED_URL,
                        STUDY_MEETING_LINK.CREATED_AT,
                        STUDY_MEETING_LINK.UPDATED_AT
                )
                .from(STUDY_MEETING_LINK)
                .where(STUDY_MEETING_LINK.MEETING_ID.in(
                        dsl.select(STUDY_MEETING.ID)
                                .from(STUDY_MEETING)
                                .where(STUDY_MEETING.STUDY_ID.eq(studyId))
                                .and(STUDY_MEETING.DELETED_AT.isNull())
                ))
                .and(STUDY_MEETING_LINK.DELETED_AT.isNull())
                .orderBy(STUDY_MEETING_LINK.CREATED_AT.desc())
                .fetch(mapper::toVoFromRecord);

        log.info("jOOQ: Found {} study meeting links for studyId: {}", result.size(), studyId);
        return result;
    }

    /**
     * 프로젝트별 링크 목록 페이징 조회
     */
    @Override
    public Page<StudyMeetingLinkVo> findByStudyId(Long studyId, Pageable pageable) {
        log.info("jOOQ: Finding study meeting links by studyId with pagination - studyId: {}", studyId);

        // 총 개수 조회
        int total = dsl.selectCount()
                .from(STUDY_MEETING_LINK)
                .where(STUDY_MEETING_LINK.MEETING_ID.in(
                        dsl.select(STUDY_MEETING.ID)
                                .from(STUDY_MEETING)
                                .where(STUDY_MEETING.STUDY_ID.eq(studyId))
                                .and(STUDY_MEETING.DELETED_AT.isNull())
                ))
                .and(STUDY_MEETING_LINK.DELETED_AT.isNull())
                .fetchOne(0, int.class);

        if (total == 0) {
            log.debug("jOOQ: No study meeting links found for studyId: {}", studyId);
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 페이징된 데이터 조회
        List<StudyMeetingLinkVo> links = dsl.select(
                        STUDY_MEETING_LINK.ID,
                        STUDY_MEETING_LINK.MEETING_ID,
                        STUDY_MEETING_LINK.MEMBER_ID,
                        STUDY_MEETING_LINK.NAME,
                        STUDY_MEETING_LINK.ATTACHED_URL,
                        STUDY_MEETING_LINK.CREATED_AT,
                        STUDY_MEETING_LINK.UPDATED_AT
                )
                .from(STUDY_MEETING_LINK)
                .where(STUDY_MEETING_LINK.MEETING_ID.in(
                        dsl.select(STUDY_MEETING.ID)
                                .from(STUDY_MEETING)
                                .where(STUDY_MEETING.STUDY_ID.eq(studyId))
                                .and(STUDY_MEETING.DELETED_AT.isNull())
                ))
                .and(STUDY_MEETING_LINK.DELETED_AT.isNull())
                .orderBy(STUDY_MEETING_LINK.CREATED_AT.desc())
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetch(mapper::toVoFromRecord);

        log.info("jOOQ: Found {} study meeting links for studyId: {}", total, studyId);
        return new PageImpl<>(links, pageable, total);
    }

    /**
     * 회원별 링크 목록 조회
     */
    @Override
    public List<StudyMeetingLinkVo> findByMemberId(Long memberId) {
        log.info("jOOQ: Finding study meeting links by memberId - {}", memberId);

        List<StudyMeetingLinkVo> result = dsl.select(
                        STUDY_MEETING_LINK.ID,
                        STUDY_MEETING_LINK.MEETING_ID,
                        STUDY_MEETING_LINK.MEMBER_ID,
                        STUDY_MEETING_LINK.NAME,
                        STUDY_MEETING_LINK.ATTACHED_URL,
                        STUDY_MEETING_LINK.CREATED_AT,
                        STUDY_MEETING_LINK.UPDATED_AT
                )
                .from(STUDY_MEETING_LINK)
                .where(STUDY_MEETING_LINK.MEMBER_ID.eq(memberId))
                .and(STUDY_MEETING_LINK.DELETED_AT.isNull())
                .orderBy(STUDY_MEETING_LINK.CREATED_AT.desc())
                .fetch(mapper::toVoFromRecord);

        log.info("jOOQ: Found {} study meeting links for memberId: {}", result.size(), memberId);
        return result;
    }

    /**
     * 미팅별 링크 목록 조회
     */
    public List<StudyMeetingLinkVo> findByMeetingId(Long meetingId) {
        log.info("jOOQ: Finding study meeting links by meetingId - {}", meetingId);

        List<StudyMeetingLinkVo> result = dsl.select(
                        STUDY_MEETING_LINK.ID,
                        STUDY_MEETING_LINK.MEETING_ID.as("meeting_id"),
                        STUDY_MEETING_LINK.MEMBER_ID,
                        STUDY_MEETING_LINK.NAME,
                        STUDY_MEETING_LINK.ATTACHED_URL,
                        STUDY_MEETING_LINK.CREATED_AT,
                        STUDY_MEETING_LINK.UPDATED_AT
                )
                .from(STUDY_MEETING_LINK)
                .where(STUDY_MEETING_LINK.MEETING_ID.eq(meetingId))
                .and(STUDY_MEETING_LINK.DELETED_AT.isNull())
                .orderBy(STUDY_MEETING_LINK.CREATED_AT.desc())
                .fetch(mapper::toVoFromRecord);

        log.info("jOOQ: Found {} study meeting links for meetingId: {}", result.size(), meetingId);
        return result;
    }

    /**
     * 링크 존재 여부 확인 (ID 기반)
     */
    public boolean existsById(Long linkId) {
        log.info("jOOQ: Checking if study meeting link exists by ID - {}", linkId);

        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(STUDY_MEETING_LINK)
                        .where(STUDY_MEETING_LINK.ID.eq(linkId))
                        .and(STUDY_MEETING_LINK.DELETED_AT.isNull())
        );

        log.info("jOOQ: Study meeting link exists: {} - ID: {}", exists, linkId);
        return exists;
    }

    /**
     * 프로젝트별 링크 존재 여부 확인
     */
    public boolean existsByStudyId(Long studyId) {
        log.info("jOOQ: Checking if study meeting links exist by studyId - {}", studyId);

        boolean existsMeeting = dsl.fetchExists(
                dsl.selectOne()
                        .from(STUDY_MEETING_LINK)
                        .where(STUDY_MEETING_LINK.MEETING_ID.in(
                                dsl.select(STUDY_MEETING.ID)
                                        .from(STUDY_MEETING)
                                        .where(STUDY_MEETING.STUDY_ID.eq(studyId))
                                        .and(STUDY_MEETING.DELETED_AT.isNull())
                        ))
                        .and(STUDY_MEETING_LINK.DELETED_AT.isNull())
        );

        log.info("jOOQ: Study meeting links exist: {} - studyId: {}", existsMeeting, studyId);
        return existsMeeting;
    }

    /**
     * 프로젝트별 링크 개수 조회
     */
    public int countByStudyId(Long studyId) {
        log.info("jOOQ: Counting study meeting links by studyId - {}", studyId);

        int count = dsl.selectCount()
                .from(STUDY_MEETING_LINK)
                .where(STUDY_MEETING_LINK.MEETING_ID.in(
                        dsl.select(STUDY_MEETING.ID)
                                .from(STUDY_MEETING)
                                .where(STUDY_MEETING.STUDY_ID.eq(studyId))
                                .and(STUDY_MEETING.DELETED_AT.isNull())
                ))
                .and(STUDY_MEETING_LINK.DELETED_AT.isNull())
                .fetchOne(0, int.class);

        log.info("jOOQ: Found {} study meeting links for studyId: {}", count, studyId);
        return count;
    }
}