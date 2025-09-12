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

        var sml = STUDY_MEETING_LINK.as("sml");

        Optional<StudyMeetingLinkVo> result = dsl.select(
                        sml.ID,
                        sml.STUDY_ID,
                        sml.MEMBER_ID,
                        sml.NAME,
                        sml.ATTACHED_URL,
                        sml.CREATED_AT,
                        sml.UPDATED_AT
                )
                .from(sml)
                .where(sml.ID.eq(linkId))
                .and(sml.DELETED_AT.isNull())
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

        var sml = STUDY_MEETING_LINK.as("sml");

        List<StudyMeetingLinkVo> result = dsl.select(
                        sml.ID,
                        sml.STUDY_ID,
                        sml.MEMBER_ID,
                        sml.NAME,
                        sml.ATTACHED_URL,
                        sml.CREATED_AT,
                        sml.UPDATED_AT
                )
                .from(sml)
                .where(sml.STUDY_ID.eq(studyId))
                .and(sml.DELETED_AT.isNull())
                .orderBy(sml.CREATED_AT.desc())
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

        var sml = STUDY_MEETING_LINK.as("sml");

        // 총 개수 조회
        int total = dsl.selectCount()
                .from(sml)
                .where(sml.STUDY_ID.eq(studyId))
                .and(sml.DELETED_AT.isNull())
                .fetchOne(0, int.class);

        if (total == 0) {
            log.debug("jOOQ: No study meeting links found for studyId: {}", studyId);
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 페이징된 데이터 조회
        List<StudyMeetingLinkVo> links = dsl.select(
                        sml.ID,
                        sml.STUDY_ID,
                        sml.MEMBER_ID,
                        sml.NAME,
                        sml.ATTACHED_URL,
                        sml.CREATED_AT,
                        sml.UPDATED_AT
                )
                .from(sml)
                .where(sml.STUDY_ID.eq(studyId))
                .and(sml.DELETED_AT.isNull())
                .orderBy(sml.CREATED_AT.desc())
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

        var sml = STUDY_MEETING_LINK.as("sml");

        List<StudyMeetingLinkVo> result = dsl.select(
                        sml.ID,
                        sml.STUDY_ID,
                        sml.MEMBER_ID,
                        sml.NAME,
                        sml.ATTACHED_URL,
                        sml.CREATED_AT,
                        sml.UPDATED_AT
                )
                .from(sml)
                .where(sml.MEMBER_ID.eq(memberId))
                .and(sml.DELETED_AT.isNull())
                .orderBy(sml.CREATED_AT.desc())
                .fetch(mapper::toVoFromRecord);

        log.info("jOOQ: Found {} study meeting links for memberId: {}", result.size(), memberId);
        return result;
    }

    /**
     * 링크 존재 여부 확인 (ID 기반)
     */
    public boolean existsById(Long linkId) {
        log.info("jOOQ: Checking if study meeting link exists by ID - {}", linkId);

        var sml = STUDY_MEETING_LINK.as("sml");

        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(sml)
                        .where(sml.ID.eq(linkId))
                        .and(sml.DELETED_AT.isNull())
        );

        log.info("jOOQ: Study meeting link exists: {} - ID: {}", exists, linkId);
        return exists;
    }

    /**
     * 프로젝트별 링크 존재 여부 확인
     */
    public boolean existsByStudyId(Long studyId) {
        log.info("jOOQ: Checking if study meeting links exist by studyId - {}", studyId);

        var sml = STUDY_MEETING_LINK.as("sml");

        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(sml)
                        .where(sml.STUDY_ID.eq(studyId))
                        .and(sml.DELETED_AT.isNull())
        );

        log.info("jOOQ: Study meeting links exist: {} - studyId: {}", exists, studyId);
        return exists;
    }

    /**
     * 프로젝트별 링크 개수 조회
     */
    public int countByStudyId(Long studyId) {
        log.info("jOOQ: Counting study meeting links by studyId - {}", studyId);

        var sml = STUDY_MEETING_LINK.as("sml");

        int count = dsl.selectCount()
                .from(sml)
                .where(sml.STUDY_ID.eq(studyId))
                .and(sml.DELETED_AT.isNull())
                .fetchOne(0, int.class);

        log.info("jOOQ: Found {} study meeting links for studyId: {}", count, studyId);
        return count;
    }
}