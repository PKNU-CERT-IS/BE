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

        var pml = PROJECT_MEETING_LINK.as("pml");

        Optional<ProjectMeetingLinkVo> result = dsl.select(
                        pml.ID,
                        pml.PROJECT_ID,
                        pml.MEMBER_ID,
                        pml.NAME,
                        pml.ATTACHED_URL,
                        pml.CREATED_AT,
                        pml.UPDATED_AT
                )
                .from(pml)
                .where(pml.ID.eq(linkId))
                .and(pml.DELETED_AT.isNull())
                .fetchOptional(mapper::toVoFromRecord);

        log.info("jOOQ: Project meeting link found - ID: {}", linkId);
        return result;
    }

    /**
     * 프로젝트별 링크 목록 조회
     */
    @Override
    public List<ProjectMeetingLinkVo> findByProjectId(Long projectId) {
        log.info("jOOQ: Finding project meeting links by projectId - {}", projectId);

        var pml = PROJECT_MEETING_LINK.as("pml");

        List<ProjectMeetingLinkVo> result = dsl.select(
                        pml.ID,
                        pml.PROJECT_ID,
                        pml.MEMBER_ID,
                        pml.NAME,
                        pml.ATTACHED_URL,
                        pml.CREATED_AT,
                        pml.UPDATED_AT
                )
                .from(pml)
                .where(pml.PROJECT_ID.eq(projectId))
                .and(pml.DELETED_AT.isNull())
                .orderBy(pml.CREATED_AT.desc())
                .fetch(mapper::toVoFromRecord);

        log.info("jOOQ: Found {} project meeting links for projectId: {}", result.size(), projectId);
        return result;
    }

    /**
     * 프로젝트별 링크 목록 페이징 조회
     */
    @Override
    public Page<ProjectMeetingLinkVo> findByProjectId(Long projectId, Pageable pageable) {
        log.info("jOOQ: Finding project meeting links by projectId with pagination - projectId: {}", projectId);

        var pml = PROJECT_MEETING_LINK.as("pml");

        // 총 개수 조회
        int total = dsl.selectCount()
                .from(pml)
                .where(pml.PROJECT_ID.eq(projectId))
                .and(pml.DELETED_AT.isNull())
                .fetchOne(0, int.class);

        if (total == 0) {
            log.debug("jOOQ: No project meeting links found for projectId: {}", projectId);
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 페이징된 데이터 조회
        List<ProjectMeetingLinkVo> links = dsl.select(
                        pml.ID,
                        pml.PROJECT_ID,
                        pml.MEMBER_ID,
                        pml.NAME,
                        pml.ATTACHED_URL,
                        pml.CREATED_AT,
                        pml.UPDATED_AT
                )
                .from(pml)
                .where(pml.PROJECT_ID.eq(projectId))
                .and(pml.DELETED_AT.isNull())
                .orderBy(pml.CREATED_AT.desc())
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetch(mapper::toVoFromRecord);

        log.info("jOOQ: Found {} project meeting links for projectId: {}", total, projectId);
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
                        pml.PROJECT_ID,
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
    public boolean existsByProjectId(Long projectId) {
        log.info("jOOQ: Checking if project meeting links exist by projectId - {}", projectId);

        var pml = PROJECT_MEETING_LINK.as("pml");

        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(pml)
                        .where(pml.PROJECT_ID.eq(projectId))
                        .and(pml.DELETED_AT.isNull())
        );

        log.info("jOOQ: Project meeting links exist: {} - projectId: {}", exists, projectId);
        return exists;
    }

    /**
     * 프로젝트별 링크 개수 조회
     */
    public int countByProjectId(Long projectId) {
        log.info("jOOQ: Counting project meeting links by projectId - {}", projectId);

        var pml = PROJECT_MEETING_LINK.as("pml");

        int count = dsl.selectCount()
                .from(pml)
                .where(pml.PROJECT_ID.eq(projectId))
                .and(pml.DELETED_AT.isNull())
                .fetchOne(0, int.class);

        log.info("jOOQ: Found {} project meeting links for projectId: {}", count, projectId);
        return count;
    }
}