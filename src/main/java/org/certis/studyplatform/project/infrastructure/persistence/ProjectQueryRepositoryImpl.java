package org.certis.studyplatform.project.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.project.domain.ProjectStatus;
import org.certis.studyplatform.project.domain.repository.ProjectQueryRepository;
import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.certis.studyplatform.project.domain.vo.ProjectSummaryVo;
import org.certis.studyplatform.project.domain.vo.ProjectSearchCriteriaVo;
import org.certis.studyplatform.project.domain.vo.ProjectSearchResultVo;
import org.certis.studyplatform.project.domain.vo.ExternalUrlVo;
import org.certis.studyplatform.project.infrastructure.mapper.ProjectInfrastructureMapper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.OrderField;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.certis.studyplatform.project.domain.vo.ProjectEndSubmissionInfoVo;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;

import static org.certis.generated.jooq.Tables.*;
import static org.jooq.impl.DSL.*;

/**
 * Project Query Repository Implementation using Generated jOOQ Tables
 *
 * ✅ jOOQ 생성 코드 활용:
 * - 타입 안전한 테이블/필드 참조
 * - 컴파일 타임 검증
 * - IDE 완벽 지원 (자동완성, 리팩토링)
 * - 스키마 변경 시 자동 감지
 *
 * ✅ CQRS 패턴에서 Query 전용 Repository
 * Infrastructure Layer
 * PostgreSQL 배열 지원
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProjectQueryRepositoryImpl implements ProjectQueryRepository {

    @Qualifier("jooqDataSource")
    private final DSLContext dsl;

    private final ProjectInfrastructureMapper mapper;

    @Override
    public Optional<ProjectEndSubmissionInfoVo> getEndSubmissionInfo(Long projectId) {
        var p = PROJECT.as("p");
        var m = MEMBER.as("m");
        return java.util.Optional.ofNullable(
                dsl.select(
                                p.ID,
                                p.STATUS.as("status"),
                                p.RESULT_SUBMIT_STATUS,
                                p.RESULT_SUBMITTED_AT,
                                p.RESULT_ATTACHED_URL,
                                p.CATEGORY,
                                p.SUBCATEGORY,
                                p.TITLE,
                                p.DESCRIPTION,
                                p.MEMBER_ID,
                                m.NAME,
                                m.GRADE,
                                p.STARTED_AT,
                                p.ENDED_AT,
                                // current participants
                                org.jooq.impl.DSL.select(org.jooq.impl.DSL.count())
                                        .from(org.certis.generated.jooq.tables.ProjectParticipant.PROJECT_PARTICIPANT)
                                        .where(org.certis.generated.jooq.tables.ProjectParticipant.PROJECT_PARTICIPANT.PROJECT_ID.eq(p.ID))
                                        .and(org.certis.generated.jooq.tables.ProjectParticipant.PROJECT_PARTICIPANT.DELETED_AT.isNull())
                                        .asField("current_participants"),
                                p.MAX_PARTICIPANTS_NUMBER
                        )
                        .from(p)
                        .leftJoin(m).on(p.MEMBER_ID.eq(m.ID))
                        .where(p.ID.eq(projectId))
                        .and(p.DELETED_AT.isNull())
                        .fetchOne()
        ).map(r -> {
            String resultSubmitStatusString = r.get(p.RESULT_SUBMIT_STATUS);
            org.certis.studyplatform.shared.domain.ResultSubmitStatus resultSubmitStatus = resultSubmitStatusString != null ? org.certis.studyplatform.shared.domain.ResultSubmitStatus.valueOf(resultSubmitStatusString) : null;
            org.certis.studyplatform.project.domain.ProjectStatus projectStatus = r.get("status", org.certis.studyplatform.project.domain.ProjectStatus.class);
            return new org.certis.studyplatform.project.domain.vo.ProjectEndSubmissionInfoVo(
                    r.get(p.ID),
                    projectStatus,
                    resultSubmitStatus,
                    r.get(p.RESULT_SUBMITTED_AT),
                    r.get(p.RESULT_ATTACHED_URL),
                    r.get(p.CATEGORY),
                    r.get(p.SUBCATEGORY),
                    r.get(p.TITLE),
                    r.get(p.DESCRIPTION),
                    r.get(p.MEMBER_ID),
                    r.get(m.NAME),
                    r.get(m.GRADE, org.certis.studyplatform.member.domain.MemberGrade.class),
                    r.get(p.STARTED_AT),
                    r.get(p.ENDED_AT),
                    r.get("current_participants", Integer.class),
                    r.get(p.MAX_PARTICIPANTS_NUMBER)
            );
        });
    }

    @Override
    public java.util.List<ProjectEndSubmissionInfoVo> findEndSubmissionsInProgress() {
        var p = PROJECT.as("p");
        var m = MEMBER.as("m");
        return dsl.select(
                        p.ID,
                        p.STATUS.as("status"),
                        p.RESULT_SUBMIT_STATUS,
                        p.RESULT_SUBMITTED_AT,
                        p.RESULT_ATTACHED_URL,
                        p.CATEGORY,
                        p.SUBCATEGORY,
                        p.TITLE,
                        p.DESCRIPTION,
                        p.MEMBER_ID,
                        m.NAME,
                        m.GRADE,
                        p.STARTED_AT,
                        p.ENDED_AT,
                        org.jooq.impl.DSL.select(org.jooq.impl.DSL.count())
                                .from(org.certis.generated.jooq.tables.ProjectParticipant.PROJECT_PARTICIPANT)
                                .where(org.certis.generated.jooq.tables.ProjectParticipant.PROJECT_PARTICIPANT.PROJECT_ID.eq(p.ID))
                                .and(org.certis.generated.jooq.tables.ProjectParticipant.PROJECT_PARTICIPANT.DELETED_AT.isNull())
                                .asField("current_participants"),
                        p.MAX_PARTICIPANTS_NUMBER
                )
                .from(p)
                .leftJoin(m).on(p.MEMBER_ID.eq(m.ID))
                .where(p.RESULT_SUBMIT_STATUS.eq(org.certis.studyplatform.shared.domain.ResultSubmitStatus.INPROGRESS.name()))
                .and(p.DELETED_AT.isNull())
                .orderBy(p.RESULT_SUBMITTED_AT.desc())
                .fetch(r -> new ProjectEndSubmissionInfoVo(
                        r.get(p.ID),
                        r.get("status", org.certis.studyplatform.project.domain.ProjectStatus.class),
                        org.certis.studyplatform.shared.domain.ResultSubmitStatus.valueOf(r.get(p.RESULT_SUBMIT_STATUS)),
                        r.get(p.RESULT_SUBMITTED_AT),
                        r.get(p.RESULT_ATTACHED_URL),
                        r.get(p.CATEGORY),
                        r.get(p.SUBCATEGORY),
                        r.get(p.TITLE),
                        r.get(p.DESCRIPTION),
                        r.get(p.MEMBER_ID),
                        r.get(m.NAME),
                        r.get(m.GRADE, org.certis.studyplatform.member.domain.MemberGrade.class),
                        r.get(p.STARTED_AT),
                        r.get(p.ENDED_AT),
                        r.get("current_participants", Integer.class),
                        r.get(p.MAX_PARTICIPANTS_NUMBER)
                ));
    }

    @Override
    public Optional<ProjectVo> findProjectDetailById(Long projectId) {
        log.info("jOOQ: Finding project detail by ID - {}", projectId);

        var p = PROJECT.as("p");
        var m = MEMBER.as("m");
        var pa = PROJECT_ATTACHED.as("pa");

        Optional<ProjectVo> result = Optional.of(
                        dsl.select(
                                        p.ID,
                                        p.MEMBER_ID,
                                        m.NAME,
                                        m.GRADE,
                                        m.PROFILE_IMAGE.as("creator_profile_image"),
                                        p.TITLE,
                                        p.DESCRIPTION,
                                        p.CONTENT,
                                        p.CATEGORY,
                                        p.SUBCATEGORY,
                                        p.STATUS.as("status"),
                                        p.THUMBNAIL_URL,
                                        p.GITHUB_URL,
                                        p.EXTERNAL_URL,
                                        p.DEMO_URL,
                                        p.MAX_PARTICIPANTS_NUMBER,
                                        p.STARTED_AT,
                        p.ENDED_AT,
                        p.DELETED_AT,
                        p.RESULT_SUBMIT_STATUS.as("result_submit_status"),
                                        // 현재 참여자 수 서브쿼리
                                        select(count())
                                                .from(PROJECT_PARTICIPANT)
                                                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(p.ID))
                                                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                                                .asField("current_participants"),
                                        // ProjectAttached 정보
                                        pa.ID.as("attached_id"),
                                        pa.NAME.as("attached_name"),
                                        pa.TYPE.as("attached_type"),
                                        pa.SIZE.as("attached_size"),
                                        pa.ATTACHED_URL
                                )
                                .from(p)
                                .leftJoin(m).on(p.MEMBER_ID.eq(m.ID))
                                .leftJoin(pa).on(p.ID.eq(pa.PROJECT_ID).and(pa.DELETED_AT.isNull()))
                                .where(p.ID.eq(projectId))
                                .and(p.DELETED_AT.isNull())
                                .fetch()
                ).filter(records -> !records.isEmpty())
                .map(records -> mapper.toProjectVoFromRecordsWithAttachments(records));

        log.info("jOOQ: Project detail found - ID: {}", projectId);
        return result;
    }

    @Override
    public ProjectSearchResultVo findProjects(ProjectSearchCriteriaVo criteria, Pageable pageable) {
        log.info("jOOQ: Finding projects with criteria - {}", criteria);
        log.debug("Pageable info - page: {}, size: {}, sort: {}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        var p = PROJECT.as("p");
        var m = MEMBER.as("m");

        // 동적 조건 구성
        Condition conditions = buildSearchConditions(criteria);
        log.debug("jOOQ: Search conditions built successfully");

        // 총 개수 조회
        int total = dsl.selectCount()
                .from(p)
                .leftJoin(m).on(p.MEMBER_ID.eq(m.ID))
                .where(conditions.and(p.DELETED_AT.isNull()))
                .fetchOne(0, int.class);

        log.debug("jOOQ: Count query result: {} total projects found", total);

        if (total == 0) {
            log.debug("jOOQ: No projects found matching criteria, returning empty result");
            return ProjectSearchResultVo.empty(pageable.getPageSize());
        }

        // 페이징된 데이터 조회
        List<ProjectSummaryVo> projectSummaries = dsl.select(
                        p.ID,
                        p.MEMBER_ID,
                        m.NAME,
                        m.GRADE,
                        m.PROFILE_IMAGE.as("creator_profile_image"),
                        p.TITLE,
                        p.DESCRIPTION,
                        p.CATEGORY,
                        p.SUBCATEGORY,
                        p.STATUS.as("status"),
                        p.THUMBNAIL_URL,
                        p.GITHUB_URL,
                        p.EXTERNAL_URL,
                        p.DEMO_URL,
                        p.MAX_PARTICIPANTS_NUMBER,
                        p.STARTED_AT,
                        p.ENDED_AT,
                        p.DELETED_AT,
                        p.RESULT_SUBMIT_STATUS.as("result_submit_status"),
                        // 현재 참여자 수 서브쿼리
                        select(count())
                                .from(PROJECT_PARTICIPANT)
                                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(p.ID))
                                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                                .asField("current_participants")
                )
                .from(p)
                .leftJoin(m).on(p.MEMBER_ID.eq(m.ID))
                .where(conditions.and(p.DELETED_AT.isNull()))
                .orderBy(buildOrderBy(pageable))
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetch(mapper::toProjectSummaryVoFromRecord);

        // 첨부파일을 각 프로젝트에 채워 넣기 (S3 URL은 상위 계층에서 normalize)
        projectSummaries = projectSummaries.stream().map(vo -> {
            List<org.certis.studyplatform.project.domain.vo.ProjectAttachedVo> attachments = findAttachmentsByProjectId(vo.id());
            return org.certis.studyplatform.project.domain.vo.ProjectSummaryVo.of(
                    vo.id(),
                    vo.title(),
                    vo.description(),
                    vo.category(),
                    vo.subcategory(),
                    vo.startDate(),
                    vo.endDate(),
                    vo.projectCreatorName(),
                    vo.projectCreatorGrade(),
                    vo.semester(),
                    vo.status(),
                    vo.isParticipantable(),
                    vo.githubUrl(),
                    vo.externalUrl(),
                    vo.thumbnailUrl(),
                    vo.demoUrl(),
                    vo.maxParticipantNumber(),
                    vo.currentParticipantNumber(),
                    vo.resultSubmitStatus(),
                    attachments
            );
        }).collect(java.util.stream.Collectors.toList());

        log.debug("jOOQ: Data query executed successfully, found {} project summaries",
                projectSummaries.size());

        int totalPages = (int) Math.ceil((double) total / pageable.getPageSize());

        log.info("jOOQ: Found {} projects", total);

        return ProjectSearchResultVo.of(
                projectSummaries,
                (long) total,
                totalPages,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
    }

    @Override
    public ProjectSearchResultVo findProjectsByMemberId(Long memberId, Pageable pageable) {
        log.info("jOOQ: Finding projects by memberId - {}", memberId);

        var p = PROJECT.as("p");
        var m = MEMBER.as("m");

        Condition condition = p.MEMBER_ID.eq(memberId);

        int total = dsl.selectCount()
                .from(p)
                .where(condition.and(p.DELETED_AT.isNull()))
                .fetchOne(0, int.class);

        List<ProjectSummaryVo> projectSummaries = dsl.select(
                        p.ID,
                        p.MEMBER_ID,
                        m.NAME,
                        m.GRADE,
                        m.GRADE,
                        p.TITLE,
                        p.DESCRIPTION,
                        p.CATEGORY,
                        p.SUBCATEGORY,
                        p.THUMBNAIL_URL,
                        p.GITHUB_URL,
                        p.EXTERNAL_URL,
                        p.DEMO_URL,
                        p.MAX_PARTICIPANTS_NUMBER,
                        p.STARTED_AT,
                        p.ENDED_AT,
                        p.DELETED_AT,
                        p.RESULT_SUBMIT_STATUS.as("result_submit_status"),
                        select(count())
                                .from(PROJECT_PARTICIPANT)
                                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(p.ID))
                                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                                .asField("current_participants")
                )
                .from(p)
                .leftJoin(m).on(p.MEMBER_ID.eq(m.ID))
                .where(condition.and(p.DELETED_AT.isNull()))
                .orderBy(p.CREATED_AT.desc())
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetch(mapper::toProjectSummaryVoFromRecord);

        projectSummaries = projectSummaries.stream().map(vo -> {
            List<org.certis.studyplatform.project.domain.vo.ProjectAttachedVo> attachments = findAttachmentsByProjectId(vo.id());
            return org.certis.studyplatform.project.domain.vo.ProjectSummaryVo.of(
                    vo.id(), vo.title(), vo.description(), vo.category(), vo.subcategory(),
                    vo.startDate(), vo.endDate(), vo.projectCreatorName(), vo.projectCreatorGrade(),
                    vo.semester(), vo.status(), vo.isParticipantable(), vo.githubUrl(), vo.externalUrl(),
                    vo.thumbnailUrl(), vo.demoUrl(), vo.maxParticipantNumber(), vo.currentParticipantNumber(),
                    vo.resultSubmitStatus(), attachments
            );
        }).collect(java.util.stream.Collectors.toList());

        return createSearchResult(projectSummaries, total, pageable);
    }

    @Override
    public ProjectSearchResultVo findProjectsByCategory(String category, Pageable pageable) {
        log.info("jOOQ: Finding projects by category - {}", category);

        var p = PROJECT.as("p");
        var m = MEMBER.as("m");

        Condition condition = p.CATEGORY.eq(category);

        int total = dsl.selectCount()
                .from(p)
                .where(condition.and(p.DELETED_AT.isNull()))
                .fetchOne(0, int.class);

        List<ProjectSummaryVo> projectSummaries = dsl.select(
                        p.ID,
                        p.MEMBER_ID,
                        m.NAME,
                        m.GRADE,
                        p.TITLE,
                        p.DESCRIPTION,
                        p.CATEGORY,
                        p.SUBCATEGORY,
                        p.THUMBNAIL_URL,
                        p.GITHUB_URL,
                        p.EXTERNAL_URL,
                        p.DEMO_URL,
                        p.MAX_PARTICIPANTS_NUMBER,
                        p.STARTED_AT,
                        p.ENDED_AT,
                        p.DELETED_AT,
                        p.RESULT_SUBMIT_STATUS.as("result_submit_status"),
                        select(count())
                                .from(PROJECT_PARTICIPANT)
                                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(p.ID))
                                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                                .asField("current_participants")
                )
                .from(p)
                .leftJoin(m).on(p.MEMBER_ID.eq(m.ID))
                .where(condition.and(p.DELETED_AT.isNull()))
                .orderBy(p.CREATED_AT.desc())
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetch(mapper::toProjectSummaryVoFromRecord);

        projectSummaries = projectSummaries.stream().map(vo -> {
            List<org.certis.studyplatform.project.domain.vo.ProjectAttachedVo> attachments = findAttachmentsByProjectId(vo.id());
            return org.certis.studyplatform.project.domain.vo.ProjectSummaryVo.of(
                    vo.id(), vo.title(), vo.description(), vo.category(), vo.subcategory(),
                    vo.startDate(), vo.endDate(), vo.projectCreatorName(), vo.projectCreatorGrade(),
                    vo.semester(), vo.status(), vo.isParticipantable(), vo.githubUrl(), vo.externalUrl(),
                    vo.thumbnailUrl(), vo.demoUrl(), vo.maxParticipantNumber(), vo.currentParticipantNumber(),
                    vo.resultSubmitStatus(), attachments
            );
        }).collect(java.util.stream.Collectors.toList());

        return createSearchResult(projectSummaries, total, pageable);
    }

    @Override
    public ProjectSearchResultVo findActiveProjects(Pageable pageable) {
        log.info("jOOQ: Finding active projects");

        var p = PROJECT.as("p");
        var m = MEMBER.as("m");

        OffsetDateTime now = OffsetDateTime.now();
        Condition condition = p.STARTED_AT.lessOrEqual(now)
                .and(p.ENDED_AT.greaterOrEqual(now));

        int total = dsl.selectCount()
                .from(p)
                .where(condition.and(p.DELETED_AT.isNull()))
                .fetchOne(0, int.class);

        List<ProjectSummaryVo> projectSummaries = dsl.select(
                        p.ID,
                        p.MEMBER_ID,
                        m.NAME,
                        m.GRADE,
                        p.TITLE,
                        p.DESCRIPTION,
                        p.CATEGORY,
                        p.SUBCATEGORY,
                        p.STATUS.as("status"),
                        p.THUMBNAIL_URL,
                        p.GITHUB_URL,
                        p.EXTERNAL_URL,
                        p.DEMO_URL,
                        p.MAX_PARTICIPANTS_NUMBER,
                        p.STARTED_AT,
                        p.ENDED_AT,
                        p.RESULT_SUBMIT_STATUS.as("result_submit_status"),
                        select(count())
                                .from(PROJECT_PARTICIPANT)
                                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(p.ID))
                                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                                .asField("current_participants")
                )
                .from(p)
                .leftJoin(m).on(p.MEMBER_ID.eq(m.ID))
                .where(condition.and(p.DELETED_AT.isNull()))
                .orderBy(p.CREATED_AT.desc())
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetch(mapper::toProjectSummaryVoFromRecord);

        projectSummaries = projectSummaries.stream().map(vo -> {
            List<org.certis.studyplatform.project.domain.vo.ProjectAttachedVo> attachments = findAttachmentsByProjectId(vo.id());
            return org.certis.studyplatform.project.domain.vo.ProjectSummaryVo.of(
                    vo.id(), vo.title(), vo.description(), vo.category(), vo.subcategory(),
                    vo.startDate(), vo.endDate(), vo.projectCreatorName(), vo.projectCreatorGrade(),
                    vo.semester(), vo.status(), vo.isParticipantable(), vo.githubUrl(), vo.externalUrl(),
                    vo.thumbnailUrl(), vo.demoUrl(), vo.maxParticipantNumber(), vo.currentParticipantNumber(),
                    vo.resultSubmitStatus(), attachments
            );
        }).collect(java.util.stream.Collectors.toList());

        return createSearchResult(projectSummaries, total, pageable);
    }

    @Override
    public long countActiveProjectsCreatedByMemberId(Long memberId) {
        log.info("jOOQ: Counting active projects created by member - memberId: {}", memberId);

        var p = PROJECT.as("p");

        OffsetDateTime now = OffsetDateTime.now();
        Condition condition = p.STARTED_AT.lessOrEqual(now)
                .and(p.ENDED_AT.greaterOrEqual(now))
                .and(p.MEMBER_ID.eq(memberId))
                .and(p.DELETED_AT.isNull());

        return dsl.selectCount()
                .from(p)
                .where(condition)
                .fetchOne(0, long.class);
    }

    @Override
    public ProjectSearchResultVo findProjectsByKeyword(String keyword, Pageable pageable) {
        log.info("jOOQ: Finding projects by keyword - {}", keyword);

        var p = PROJECT.as("p");
        var m = MEMBER.as("m");

        String likeKeyword = "%" + keyword + "%";
        Condition condition = p.TITLE.likeIgnoreCase(likeKeyword)
                .or(p.DESCRIPTION.likeIgnoreCase(likeKeyword))
                .or(p.CATEGORY.likeIgnoreCase(likeKeyword))
                .or(p.SUBCATEGORY.likeIgnoreCase(likeKeyword));

        int total = dsl.selectCount()
                .from(p)
                .where(condition.and(p.DELETED_AT.isNull()))
                .fetchOne(0, int.class);

        List<ProjectSummaryVo> projectSummaries = dsl.select(
                        p.ID,
                        p.MEMBER_ID,
                        m.NAME,
                        m.GRADE,
                        p.TITLE,
                        p.DESCRIPTION,
                        p.CATEGORY,
                        p.SUBCATEGORY,
                        p.STATUS.as("status"),
                        p.THUMBNAIL_URL,
                        p.GITHUB_URL,
                        p.EXTERNAL_URL,
                        p.DEMO_URL,
                        p.MAX_PARTICIPANTS_NUMBER,
                        p.STARTED_AT,
                        p.ENDED_AT,
                        p.RESULT_SUBMIT_STATUS.as("result_submit_status"),
                        select(count())
                                .from(PROJECT_PARTICIPANT)
                                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(p.ID))
                                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                                .asField("current_participants")
                )
                .from(p)
                .leftJoin(m).on(p.MEMBER_ID.eq(m.ID))
                .where(condition.and(p.DELETED_AT.isNull()))
                .orderBy(p.CREATED_AT.desc())
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetch(mapper::toProjectSummaryVoFromRecord);

        return createSearchResult(projectSummaries, total, pageable);
    }

    @Override
    public ProjectSearchResultVo findProjectsBySkills(List<String> skills, Pageable pageable) {
        log.info("jOOQ: Finding projects by skills - {}", skills);

        var p = PROJECT.as("p");
        var m = MEMBER.as("m");

        String[] skillsArray = convertSkillsToArray(skills);

        // ✅ jOOQ로 PostgreSQL 배열 연산자 사용 (타입 안전)
        Condition condition = condition("p.skills && CAST(? AS text[])", (Object) skillsArray);

        int total = dsl.selectCount()
                .from(p)
                .where(condition.and(p.DELETED_AT.isNull()))
                .fetchOne(0, int.class);

        List<ProjectSummaryVo> projectSummaries = dsl.select(
                        p.ID,
                        p.MEMBER_ID,
                        m.NAME,
                        m.GRADE,
                        m.PROFILE_IMAGE.as("creator_profile_image"),
                        p.TITLE,
                        p.DESCRIPTION,
                        p.CATEGORY,
                        p.SUBCATEGORY,
                        p.THUMBNAIL_URL,
                        p.GITHUB_URL,
                        p.EXTERNAL_URL,
                        p.DEMO_URL,
                        p.MAX_PARTICIPANTS_NUMBER,
                        p.STARTED_AT,
                        p.ENDED_AT,
                        p.RESULT_SUBMIT_STATUS.as("result_submit_status"),
                        select(count())
                                .from(PROJECT_PARTICIPANT)
                                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(p.ID))
                                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                                .asField("current_participants")
                )
                .from(p)
                .leftJoin(m).on(p.MEMBER_ID.eq(m.ID))
                .where(condition.and(p.DELETED_AT.isNull()))
                .orderBy(p.CREATED_AT.desc())
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetch(mapper::toProjectSummaryVoFromRecord);

        return createSearchResult(projectSummaries, total, pageable);
    }

    // ================= Additional Helper Methods for Domain Service =================

    /**
     * ✅ jOOQ 생성 테이블로 프로젝트 존재 여부 확인
     */
    public boolean existsByTitle(String title) {
        log.info("jOOQ: Checking if project exists by title - {}", title);

        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(PROJECT)
                        .where(PROJECT.TITLE.eq(title))
                        .and(PROJECT.DELETED_AT.isNull())
        );

        log.info("jOOQ: Project exists by title: {} - title: {}", exists, title);
        return exists;
    }

    /**
     * ✅ jOOQ 생성 테이블로 프로젝트 제목 중복 확인 (자신 제외)
     */
    public boolean existsByTitleAndIdNot(String title, Long projectId) {
        log.info("jOOQ: Checking if project exists by title excluding ID - title: {}, excludeId: {}",
                title, projectId);

        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(PROJECT)
                        .where(PROJECT.TITLE.eq(title))
                        .and(PROJECT.ID.ne(projectId))
                        .and(PROJECT.DELETED_AT.isNull())
        );

        log.info("jOOQ: Project exists by title (excluding ID): {} - title: {}, excludeId: {}",
                exists, title, projectId);
        return exists;
    }

    /**
     * ✅ jOOQ 생성 테이블로 ProjectVo 조회 (Domain Service용)
     */
    public Optional<ProjectVo> findById(Long projectId) {
        log.info("jOOQ: Finding project VO by ID - {}", projectId);

        var p = PROJECT.as("p");
        var m = MEMBER.as("m");
        var pa = PROJECT_ATTACHED.as("pa");

        List<org.jooq.Record> records = dsl.select(
                        p.ID,
                        p.STATUS.as("status"),
                        p.TITLE,
                        p.DESCRIPTION,
                        p.CONTENT,
                        p.CATEGORY,
                        p.SUBCATEGORY,
                        p.STARTED_AT,
                        p.ENDED_AT,
                        p.DELETED_AT,
                        p.MEMBER_ID,
                        m.NAME,
                        m.GRADE,
                        m.PROFILE_IMAGE.as("creator_profile_image"),
                        p.MAX_PARTICIPANTS_NUMBER,
                        p.THUMBNAIL_URL,
                        p.GITHUB_URL,
                        p.EXTERNAL_URL,
                        p.DEMO_URL,
                        p.CREATED_AT,
                        p.UPDATED_AT,
                        p.RESULT_SUBMIT_STATUS.as("result_submit_status"),
                        // 현재 참여자 수 서브쿼리
                        select(count())
                                .from(PROJECT_PARTICIPANT)
                                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(p.ID))
                                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                                .asField("current_participants"),
                        // ProjectAttached 정보
                        pa.ID.as("attached_id"),
                        pa.NAME.as("attached_name"),
                        pa.TYPE.as("attached_type"),
                        pa.SIZE.as("attached_size"),
                        pa.ATTACHED_URL
                )
                .from(p)
                .leftJoin(m).on(p.MEMBER_ID.eq(m.ID))
                .leftJoin(pa).on(p.ID.eq(pa.PROJECT_ID).and(pa.DELETED_AT.isNull()))
                .where(p.ID.eq(projectId))
                .and(p.DELETED_AT.isNull())
                .fetch();

        if (records.isEmpty()) {
            log.warn("jOOQ: Project not found - ID: {}", projectId);
            return Optional.empty();
        }

        ProjectVo result = mapper.toProjectVoFromRecordsWithAttachments(records);
        log.info("jOOQ: Project VO found - ID: {}", projectId);
        return Optional.of(result);
    }

    public Optional<ProjectVo> findByIdAndDeletedAtIsNull(Long projectId) {
        log.info("jOOQ: Finding project VO by ID - {}", projectId);

        var p = PROJECT.as("p");
        var m = MEMBER.as("m");
        var pa = PROJECT_ATTACHED.as("pa");

        Optional<ProjectVo> result = Optional.of(
                dsl.select(
                        p.ID,
                        p.STATUS.as("status"),
                        p.TITLE,
                        p.DESCRIPTION,
                        p.CONTENT,
                        p.CATEGORY,
                        p.SUBCATEGORY,
                        p.STARTED_AT,
                        p.ENDED_AT,
                        p.DELETED_AT,
                        p.MEMBER_ID,
                        m.NAME,
                        m.GRADE,
                        p.THUMBNAIL_URL,
                        p.GITHUB_URL,
                        p.EXTERNAL_URL,
                        p.DEMO_URL,
                        p.MAX_PARTICIPANTS_NUMBER,
                        p.CREATED_AT,
                        p.UPDATED_AT,
                        p.RESULT_SUBMIT_STATUS.as("result_submit_status"),
                        // 현재 참여자 수 서브쿼리
                        select(count())
                                .from(PROJECT_PARTICIPANT)
                                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(p.ID))
                                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                                .asField("current_participants"),
                        // ProjectAttached 정보
                        pa.ID.as("attached_id"),
                        pa.NAME.as("attached_name"),
                        pa.TYPE.as("attached_type"),
                        pa.SIZE.as("attached_size"),
                        pa.ATTACHED_URL
                )
                .from(p)
                .leftJoin(m).on(p.MEMBER_ID.eq(m.ID))
                .leftJoin(pa).on(p.ID.eq(pa.PROJECT_ID).and(pa.DELETED_AT.isNull()))
                .where(p.ID.eq(projectId))
                .and(p.DELETED_AT.isNull())
                .fetch()
        ).filter(records -> !records.isEmpty())
                .map(records -> mapper.toProjectVoFromRecordsWithAttachments(records));

        log.info("jOOQ: Project VO found - ID: {}", projectId);
        return result;
    }


    @Override
    public Page<ProjectSummaryVo> findCompletedProjectsByMember(Long memberId, Pageable pageable) {
        log.info("Repository: Finding completed projects by member - memberId: {}", memberId);

        var p = PROJECT.as("p");
        var m = MEMBER.as("m");

        // 전체 개수 조회
        int totalCount = dsl.selectCount()
                .from(p)
                .where(p.MEMBER_ID.eq(memberId))
                .and(p.DELETED_AT.isNull())
                .and(p.ENDED_AT.lessThan(OffsetDateTime.now())) // 완료 조건
                .fetchOne(0, int.class);

        if (totalCount == 0) {
            return Page.empty(pageable);
        }

        // 데이터 조회
        List<ProjectSummaryVo> projects = dsl.select(
                        p.ID,
                        p.TITLE,
                        p.DESCRIPTION,
                        p.CATEGORY,
                        p.SUBCATEGORY,
                        p.STARTED_AT,
                        p.ENDED_AT,
                        p.DELETED_AT,
                        m.NAME.as("creator_name"),
                        m.GRADE,
                        p.MAX_PARTICIPANTS_NUMBER,
                        p.GITHUB_URL,
                        p.EXTERNAL_URL
                )
                .from(p)
                .leftJoin(m).on(p.MEMBER_ID.eq(m.ID))
                .where(p.MEMBER_ID.eq(memberId))
                .and(p.DELETED_AT.isNull())
                .and(p.ENDED_AT.lessThan(OffsetDateTime.now()))
                .orderBy(p.ENDED_AT.desc()) // 최근 완료된 순
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetch(record -> {
                    // ExternalUrl 문자열을 ExternalUrlVo로 변환
                    ExternalUrlVo externalUrlVo = null;
                    String externalUrlStr = record.get(p.EXTERNAL_URL);
                    if (externalUrlStr != null && !externalUrlStr.trim().isEmpty()) {
                        try {
                            if (externalUrlStr.startsWith("{") && externalUrlStr.endsWith("}")) {
                                String title = extractJsonValue(externalUrlStr, "title");
                                String url = extractJsonValue(externalUrlStr, "url");
                                if (title != null && url != null) {
                                    externalUrlVo = new ExternalUrlVo(title, url);
                                }
                            }
                        } catch (Exception e) {
                            externalUrlVo = null;
                        }
                    }

                    return ProjectSummaryVo.of(
                            record.get(p.ID),
                            record.get(p.TITLE),
                            record.get(p.DESCRIPTION),
                            record.get(p.CATEGORY),
                            record.get(p.SUBCATEGORY),
                            record.get(p.STARTED_AT),
                            record.get(p.ENDED_AT),
                            record.get("creator_name", String.class),
                            record.get(m.GRADE, MemberGrade.class),
                            calculateSemester(record.get(p.ENDED_AT)), // semester 계산
                            ProjectStatus.COMPLETED.name(), // 완료된 프로젝트는 status = COMPLETED
                            false, // 완료된 프로젝트는 참가 불가
                            record.get(p.GITHUB_URL),
                            externalUrlVo,
                            record.get(p.THUMBNAIL_URL),
                            record.get(p.DEMO_URL), // demoUrl
                            record.get(p.MAX_PARTICIPANTS_NUMBER, Integer.class), // maxParticipantNumber
                            record.get("current_participants", Integer.class), // currentParticipantNumber
                            record.get(p.RESULT_SUBMIT_STATUS, ResultSubmitStatus.class),
                            java.util.Collections.emptyList()
                    );
                });

        return new PageImpl<>(projects, pageable, totalCount);
    }

    @Override
    public List<ProjectSummaryVo> findCompletedProjectsListByMember(Long memberId) {
        log.info("Repository: Finding completed projects list by member - memberId: {}", memberId);

        var p = PROJECT.as("p");
        var m = MEMBER.as("m");

        return dsl.select(
                        p.ID,
                        p.TITLE,
                        p.DESCRIPTION,
                        p.CATEGORY,
                        p.SUBCATEGORY,
                        p.STARTED_AT,
                        p.ENDED_AT,
                        p.DELETED_AT,
                        m.NAME.as("creator_name"),
                        m.GRADE,
                        p.MAX_PARTICIPANTS_NUMBER,
                        p.GITHUB_URL,
                        p.EXTERNAL_URL,
                        p.THUMBNAIL_URL,
                        p.DEMO_URL,
                        // 현재 참여자 수 서브쿼리 (다른 메서드들과 일관성 유지)
                        select(count())
                                .from(PROJECT_PARTICIPANT)
                                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(p.ID))
                                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                                .asField("current_participants")
                )
                .from(p)
                .leftJoin(m).on(p.MEMBER_ID.eq(m.ID))
                .where(p.MEMBER_ID.eq(memberId))
                .and(p.DELETED_AT.isNull())
                .and(p.ENDED_AT.lessThan(OffsetDateTime.now()))
                .orderBy(p.ENDED_AT.desc())
                .fetch(record -> {
                    // ExternalUrl 문자열을 ExternalUrlVo로 변환
                    ExternalUrlVo externalUrlVo = null;
                    String externalUrlStr = record.get(p.EXTERNAL_URL);
                    if (externalUrlStr != null && !externalUrlStr.trim().isEmpty()) {
                        try {
                            if (externalUrlStr.startsWith("{") && externalUrlStr.endsWith("}")) {
                                String title = extractJsonValue(externalUrlStr, "title");
                                String url = extractJsonValue(externalUrlStr, "url");
                                if (title != null && url != null) {
                                    externalUrlVo = new ExternalUrlVo(title, url);
                                }
                            }
                        } catch (Exception e) {
                            externalUrlVo = null;
                        }
                    }

                    return ProjectSummaryVo.of(
                            record.get(p.ID),
                            record.get(p.TITLE),
                            record.get(p.DESCRIPTION),
                            record.get(p.CATEGORY),
                            record.get(p.SUBCATEGORY),
                            record.get(p.STARTED_AT),
                            record.get(p.ENDED_AT),
                            record.get("creator_name", String.class),
                            record.get(m.GRADE, MemberGrade.class),
                            calculateSemester(record.get(p.ENDED_AT)), // semester 계산
                            ProjectStatus.COMPLETED.name(), // 완료된 프로젝트는 status = COMPLETED
                            false, // 완료된 프로젝트는 참가 불가
                            record.get(p.GITHUB_URL),
                            externalUrlVo,
                            record.get(p.THUMBNAIL_URL),
                            record.get(p.DEMO_URL), // demoUrl
                            record.get(p.MAX_PARTICIPANTS_NUMBER, Integer.class), // maxParticipantNumber
                            record.get("current_participants", Integer.class), // currentParticipantNumber
                            null,
                            java.util.Collections.emptyList()
                    );
                });
    }


    // ================================================================
    // PRIVATE HELPER METHODS
    // ================================================================

    /**
     * 학기 계산 (종료일 기준)
     */
    private String calculateSemester(OffsetDateTime endedAt) {
        if (endedAt == null) return null;
        java.time.LocalDate endDate = endedAt.toLocalDate();
        int year = endDate.getYear();
        int month = endDate.getMonthValue();
        if (month >= 3 && month <= 8) return year + "-1";
        else return year + "-2";
    }

    /**
     * ✅ jOOQ 생성 테이블로 검색 조건 구성
     */
    private Condition buildSearchConditions(ProjectSearchCriteriaVo criteria) {
        log.debug("jOOQ: Building search conditions with criteria: {}", criteria);

        var p = PROJECT.as("p");
        var m = MEMBER.as("m");

        Condition conditions = noCondition();

        // 키워드 검색 (title, description, creatorName 포함)
        if (criteria.keyword() != null && !criteria.keyword().trim().isEmpty()) {
            String likeKeyword = "%" + criteria.keyword() + "%";
            conditions = conditions.and(
                    p.TITLE.likeIgnoreCase(likeKeyword)
                            .or(p.DESCRIPTION.likeIgnoreCase(likeKeyword))
                            .or(m.NAME.likeIgnoreCase(likeKeyword))
            );
            log.debug("jOOQ: Added keyword condition: {}", likeKeyword);
        }

        // 학기 필터 (semester 기간 검색)
        if (criteria.semester() != null && !criteria.semester().trim().isEmpty()) {
            try {
                // DateTimeUtils를 사용하여 semester 기간 파싱
                var semesterPeriod = org.certis.studyplatform.shared.util.DateTimeUtils.parseSemesterPeriod(criteria.semester());

                // 프로젝트 기간이 학기 기간과 겹치는 경우를 검색
                conditions = conditions.and(
                        p.STARTED_AT.lessOrEqual(semesterPeriod.endDate())
                                .and(p.ENDED_AT.greaterOrEqual(semesterPeriod.startDate()))
                );
                log.debug("jOOQ: Added semester condition: {} to {}",
                        semesterPeriod.startDate(), semesterPeriod.endDate());
            } catch (IllegalArgumentException e) {
                log.warn("jOOQ: Invalid semester format: {}", criteria.semester());
                // 잘못된 형식의 경우 조건 무시
            }
        }

        if (criteria.category() != null) {
            conditions = conditions.and(p.CATEGORY.eq(criteria.category()));
            log.debug("jOOQ: Added category condition: {}", criteria.category());
        }

        if (criteria.subCategory() != null) {
            conditions = conditions.and(p.SUBCATEGORY.eq(criteria.subCategory()));
            log.debug("jOOQ: Added subcategory condition: {}", criteria.subCategory());
        }

        // 프로젝트 상태 필터 (상태 매핑 로직 적용)
        if (criteria.status() != null && !criteria.status().trim().isEmpty()) {
            String upperStatus = criteria.status().toUpperCase();
            Condition statusCondition = buildProjectStatusCondition(upperStatus, p);
            if (statusCondition != null) {
                conditions = conditions.and(statusCondition);
                log.debug("jOOQ: Added status condition for: {}", criteria.status());
            }
        }

        log.debug("jOOQ: Final conditions built: {}", conditions);
        return conditions;
    }

    /**
     * 프로젝트 상태 조건 구성 (상태 매핑 로직 적용)
     * READY → READY, APPROVED
     * INPROGRESS → INPROGRESS
     * COMPLETED → COMPLETED
     */
    private Condition buildProjectStatusCondition(String status, org.jooq.Table<?> p) {
        var pTable = PROJECT.as("p");
        return switch (status) {
            case "READY" -> pTable.STATUS.in("READY", "APPROVED");
            case "INPROGRESS" -> pTable.STATUS.eq("INPROGRESS");
            case "COMPLETED" -> pTable.STATUS.eq("COMPLETED");
            default -> {
                log.warn("jOOQ: Unknown project status: {}", status);
                yield null;
            }
        };
    }


    /**
     * ✅ jOOQ 생성 테이블로 정렬 조건 구성
     */
    private OrderField<?>[] buildOrderBy(Pageable pageable) {
        var p = PROJECT.as("p");

        if (pageable.getSort().isUnsorted()) {
            return new OrderField<?>[]{p.CREATED_AT.desc()};
        }

        return pageable.getSort().stream()
                .map(order -> {
                    Field<?> sortField; // OrderField → Field로 변경
                    switch (order.getProperty()) {
                        case "createdAt":
                            sortField = p.CREATED_AT;
                            break;
                        case "updatedAt":
                            sortField = p.UPDATED_AT;
                            break;
                        case "startedAt":
                            sortField = p.STARTED_AT;
                            break;
                        case "endedAt":
                            sortField = p.ENDED_AT;
                            break;
                        case "title":
                            sortField = p.TITLE;
                            break;
                        case "memberId":
                            sortField = p.MEMBER_ID;
                            break;
                        case "maxParticipantsNumber":
                            sortField = p.MAX_PARTICIPANTS_NUMBER;
                            break;
                        default:
                            sortField = p.CREATED_AT; // 기본값
                    }
                    return order.isAscending() ? sortField.asc() : sortField.desc();
                })
                .toArray(OrderField[]::new);
    }

    /**
     * 공통 검색 결과 생성
     */
    private ProjectSearchResultVo createSearchResult(List<ProjectSummaryVo> projectSummaries,
                                                   Integer totalElements, Pageable pageable) {
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

        log.debug("jOOQ: Found {} projects", totalElements);

        return new ProjectSearchResultVo(
                projectSummaries,
                (long) totalElements,
                totalPages,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
    }

    /**
     * List<String>을 PostgreSQL 배열용 String[]로 변환
     */
    private String[] convertSkillsToArray(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return new String[0];
        }
        return skills.toArray(new String[0]);
    }

    /**
     * 프로젝트 첨부파일 조회 (요약 조회용)
     */
    private List<org.certis.studyplatform.project.domain.vo.ProjectAttachedVo> findAttachmentsByProjectId(Long projectId) {
        var pa = PROJECT_ATTACHED.as("pa");
        return dsl.select(
                        pa.ID,
                        pa.NAME,
                        pa.TYPE,
                        pa.SIZE,
                        pa.ATTACHED_URL
                )
                .from(pa)
                .where(pa.PROJECT_ID.eq(projectId))
                .and(pa.DELETED_AT.isNull())
                .orderBy(pa.CREATED_AT.asc())
                .fetch()
                .stream()
                .map(record -> org.certis.studyplatform.project.domain.vo.ProjectAttachedVo.of(
                        record.get(pa.ID),
                        record.get(pa.NAME),
                        record.get(pa.TYPE),
                        record.get(pa.SIZE),
                        record.get(pa.ATTACHED_URL)
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * JSON 문자열에서 특정 키의 값을 추출하는 헬퍼 메서드
     */
    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            // 파싱 실패 시 null 반환
        }
        return null;
    }

    @Override
    public List<Long> findApprovedProjectsStartedBefore(OffsetDateTime currentTime) {
        log.info("jOOQ: Finding approved projects started before {}", currentTime);

        List<Long> projectIds = dsl.select(PROJECT.ID)
                .from(PROJECT)
                .where(PROJECT.STATUS.eq("APPROVED"))
                .and(PROJECT.STARTED_AT.le(currentTime))
                .and(PROJECT.DELETED_AT.isNull())
                .fetch()
                .stream()
                .map(record -> record.get(PROJECT.ID))
                .collect(java.util.stream.Collectors.toList());

        log.info("jOOQ: Found {} approved projects started before {}", projectIds.size(), currentTime);
        return projectIds;
    }

    @Override
    public Optional<ProjectVo> findVoByIdForStatusCheck(Long projectId) {
        log.info("jOOQ: Finding project VO (for status check) by ID - {}", projectId);

        return Optional.ofNullable(
                dsl.selectFrom(PROJECT)
                        .where(PROJECT.ID.eq(projectId))
                        .and(PROJECT.DELETED_AT.isNull())
                        .fetchOne()
        ).map(record -> {
            var entity = org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectEntity.builder()
                    .id(record.get(PROJECT.ID))
                    .memberId(record.get(PROJECT.MEMBER_ID))
                    .title(record.get(PROJECT.TITLE))
                    .description(record.get(PROJECT.DESCRIPTION))
                    .content(record.get(PROJECT.CONTENT))
                    .category(record.get(PROJECT.CATEGORY))
                    .subcategory(record.get(PROJECT.SUBCATEGORY))
                    .maxParticipantsNumber(record.get(PROJECT.MAX_PARTICIPANTS_NUMBER))
                    .githubUrl(record.get(PROJECT.GITHUB_URL))
                    .externalUrl(record.get(PROJECT.EXTERNAL_URL))
                    .demoUrl(record.get(PROJECT.DEMO_URL))
                    .thumbnailUrl(record.get(PROJECT.THUMBNAIL_URL))
                    .startedAt(record.get(PROJECT.STARTED_AT))
                    .endedAt(record.get(PROJECT.ENDED_AT))
                    .createdAt(record.get(PROJECT.CREATED_AT))
                    .updatedAt(record.get(PROJECT.UPDATED_AT))
                    .deletedAt(record.get(PROJECT.DELETED_AT))
                    .resultSubmittedAt(record.get(PROJECT.RESULT_SUBMITTED_AT))
                    .resultSubmitStatus(record.get(PROJECT.RESULT_SUBMIT_STATUS) != null ?
                            ResultSubmitStatus.valueOf(record.get(PROJECT.RESULT_SUBMIT_STATUS)) : ResultSubmitStatus.READY)
                    .status(record.get(PROJECT.STATUS) != null ?
                            ProjectStatus.valueOf(record.get(PROJECT.STATUS)) : ProjectStatus.READY)
                    .build();
            return mapper.toVo(entity);
        });
    }
}