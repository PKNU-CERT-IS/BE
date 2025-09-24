package org.certis.studyplatform.project.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.domain.repository.ProjectMeetingLinkQueryRepository;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingLinkVo;
import org.certis.studyplatform.project.infrastructure.mapper.ProjectMeetingLinkInfrastructureMapper;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.certis.generated.jooq.Tables.*;

/**
 * ProjectMeetingLink Query Repository Implementation using Generated jOOQ Tables
 *
 * ✅ CQRS 엄격 적용: Query는 조회 작업만 담당 (Read Only)
 * ✅ jOOQ 생성 코드 활용으로 타입 안전성 보장
 * Infrastructure Layer
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProjectMeetingLinkQueryRepositoryImpl implements ProjectMeetingLinkQueryRepository {

    @Qualifier("jooqDataSource")
    private final DSLContext dsl;

    private final ProjectMeetingLinkInfrastructureMapper mapper;

    /**
     * ID로 링크 조회
     */
    @Override
    public Optional<ProjectMeetingLinkVo> findById(Long linkId) {
        log.info("jOOQ: Finding project meeting link by ID - {}", linkId);

        Optional<ProjectMeetingLinkVo> result = dsl.select(
                        PROJECT_MEETING_LINK.ID,
                        PROJECT_MEETING_LINK.MEETING_ID,
                        PROJECT_MEETING_LINK.MEMBER_ID,
                        PROJECT_MEETING_LINK.NAME,
                        PROJECT_MEETING_LINK.ATTACHED_URL,
                        PROJECT_MEETING_LINK.CREATED_AT,
                        PROJECT_MEETING_LINK.UPDATED_AT
                )
                .from(PROJECT_MEETING_LINK)
                .where(PROJECT_MEETING_LINK.ID.eq(linkId))
                .and(PROJECT_MEETING_LINK.DELETED_AT.isNull())
                .fetchOptional(mapper::toVoFromRecord);

        log.info("jOOQ: Project meeting link found - ID: {}", linkId);
        return result;
    }

    /**
     * 프로젝트별 링크 목록 조회
     */
    @Override
    public List<ProjectMeetingLinkVo> findByMeetingId(Long meetingId) {
        log.info("jOOQ: Finding project meeting links by meetingId - {}", meetingId);

        List<ProjectMeetingLinkVo> result = dsl.select(
                        PROJECT_MEETING_LINK.ID,
                        PROJECT_MEETING_LINK.MEETING_ID,
                        PROJECT_MEETING_LINK.MEMBER_ID,
                        PROJECT_MEETING_LINK.NAME,
                        PROJECT_MEETING_LINK.ATTACHED_URL,
                        PROJECT_MEETING_LINK.CREATED_AT,
                        PROJECT_MEETING_LINK.UPDATED_AT
                )
                .from(PROJECT_MEETING_LINK)
                .where(PROJECT_MEETING_LINK.MEETING_ID.eq(meetingId))
                .and(PROJECT_MEETING_LINK.DELETED_AT.isNull())
                .orderBy(PROJECT_MEETING_LINK.CREATED_AT.desc())
                .fetch(mapper::toVoFromRecord);

        log.info("jOOQ: Found {} project meeting links for meetingId: {}", result.size(), meetingId);
        return result;
    }

    /**
     * 프로젝트별 링크 목록 페이징 조회
     */
    @Override
    public Page<ProjectMeetingLinkVo> findByMeetingId(Long meetingId, Pageable pageable) {
        log.info("jOOQ: Finding project meeting links by meetingId with pagination - meetingId: {}", meetingId);

        // 총 개수 조회
        int total = dsl.selectCount()
                .from(PROJECT_MEETING_LINK)
                .where(PROJECT_MEETING_LINK.MEETING_ID.eq(meetingId))
                .and(PROJECT_MEETING_LINK.DELETED_AT.isNull())
                .fetchOne(0, int.class);

        if (total == 0) {
            log.debug("jOOQ: No project meeting links found for meetingId: {}", meetingId);
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 페이징된 데이터 조회
        List<ProjectMeetingLinkVo> links = dsl.select(
                        PROJECT_MEETING_LINK.ID,
                        PROJECT_MEETING_LINK.MEETING_ID,
                        PROJECT_MEETING_LINK.MEMBER_ID,
                        PROJECT_MEETING_LINK.NAME,
                        PROJECT_MEETING_LINK.ATTACHED_URL,
                        PROJECT_MEETING_LINK.CREATED_AT,
                        PROJECT_MEETING_LINK.UPDATED_AT
                )
                .from(PROJECT_MEETING_LINK)
                .where(PROJECT_MEETING_LINK.MEETING_ID.eq(meetingId))
                .and(PROJECT_MEETING_LINK.DELETED_AT.isNull())
                .orderBy(PROJECT_MEETING_LINK.CREATED_AT.desc())
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetch(mapper::toVoFromRecord);

        log.info("jOOQ: Found {} project meeting links for meetingId: {}", total, meetingId);
        return new PageImpl<>(links, pageable, total);
    }

    /**
     * 회원별 링크 목록 조회
     */
    @Override
    public List<ProjectMeetingLinkVo> findByMemberId(Long memberId) {
        log.info("jOOQ: Finding project meeting links by memberId - {}", memberId);

        var pml = PROJECT_MEETING_LINK.as("pml");

        List<ProjectMeetingLinkVo> result = dsl.select(
                        pml.ID,
                        pml.MEETING_ID,
                        pml.MEMBER_ID,
                        pml.NAME,
                        pml.ATTACHED_URL,
                        pml.CREATED_AT,
                        pml.UPDATED_AT
                )
                .from(pml)
                .where(pml.MEMBER_ID.eq(memberId))
                .and(pml.DELETED_AT.isNull())
                .orderBy(pml.CREATED_AT.desc())
                .fetch(mapper::toVoFromRecord);

        log.info("jOOQ: Found {} project meeting links for memberId: {}", result.size(), memberId);
        return result;
    }

    /**
     * 링크 존재 여부 확인 (ID 기반)
     */
    public boolean existsById(Long linkId) {
        log.info("jOOQ: Checking if project meeting link exists by ID - {}", linkId);

        var pml = PROJECT_MEETING_LINK.as("pml");

        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(pml)
                        .where(pml.ID.eq(linkId))
                        .and(pml.DELETED_AT.isNull())
        );

        log.info("jOOQ: Project meeting link exists: {} - ID: {}", exists, linkId);
        return exists;
    }

    /**
     * 프로젝트별 링크 존재 여부 확인
     */
    public boolean existsByMeetingId(Long meetingId) {
        log.info("jOOQ: Checking if project meeting links exist by meetingId - {}", meetingId);

        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(PROJECT_MEETING_LINK)
                        .where(PROJECT_MEETING_LINK.MEETING_ID.eq(meetingId))
                        .and(PROJECT_MEETING_LINK.DELETED_AT.isNull())
        );

        log.info("jOOQ: Project meeting links exist: {} - meetingId: {}", exists, meetingId);
        return exists;
    }

    /**
     * 프로젝트별 링크 개수 조회
     */
    public int countByMeetingId(Long meetingId) {
        log.info("jOOQ: Counting project meeting links by meetingId - {}", meetingId);

        int count = dsl.selectCount()
                .from(PROJECT_MEETING_LINK)
                .where(PROJECT_MEETING_LINK.MEETING_ID.eq(meetingId))
                .and(PROJECT_MEETING_LINK.DELETED_AT.isNull())
                .fetchOne(0, int.class);

        log.info("jOOQ: Found {} project meeting links for meetingId: {}", count, meetingId);
        return count;
    }
}