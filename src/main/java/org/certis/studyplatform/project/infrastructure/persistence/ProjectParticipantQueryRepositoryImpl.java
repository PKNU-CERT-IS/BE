package org.certis.studyplatform.project.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository;
import org.certis.studyplatform.project.domain.vo.*;
import org.certis.studyplatform.project.infrastructure.mapper.ProjectInfrastructureMapper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import static org.jooq.impl.DSL.inline;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.certis.generated.jooq.Tables.*;
import static org.jooq.impl.DSL.currentOffsetDateTime;

/**
 * Project Participant Query Repository Implementation using Generated jOOQ Tables
 *
 * CQRS Query 측면의 Repository 구현체 (Read 작업)
 * Infrastructure Layer
 * jOOQ를 사용하여 복잡한 조회 쿼리 수행
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProjectParticipantQueryRepositoryImpl implements ProjectParticipantQueryRepository {

    @Qualifier("jooqDataSource")
    private final DSLContext dsl;

    private final ProjectInfrastructureMapper mapper;

    @Override
    public Optional<ProjectParticipantVo> findById(Long participantId) {
        log.info("jOOQ: Finding participant by ID - {}", participantId);

        var pp = PROJECT_PARTICIPANT.as("pp");
        var m = MEMBER.as("m");

        Optional<ProjectParticipantVo> result = dsl.select(
                        pp.ID,
                        pp.PROJECT_ID,
                        pp.MEMBER_ID,
                        m.NAME.as("member_name"),
                        pp.STATUS,
                        pp.CREATED_AT,
                        pp.UPDATED_AT
                )
                .from(pp)
                .leftJoin(m).on(pp.MEMBER_ID.eq(m.ID))
                .where(pp.ID.eq(participantId))
                .and(pp.DELETED_AT.isNull())
                .fetchOptional(mapper::toVoFromRecord);

        log.info("jOOQ: Participant found - ID: {}", participantId);
        return result;
    }

    @Override
    public Page<ProjectParticipantSummaryVo> findByProjectId(Long projectId, ProjectParticipantStatus status, Pageable pageable) {
        log.info("jOOQ: Finding participants by project - projectId: {}, status: {}", projectId, status);

        var pp = PROJECT_PARTICIPANT.as("pp");
        var m = MEMBER.as("m");
        var p = PROJECT.as("p");

        Condition condition = pp.PROJECT_ID.eq(projectId).and(pp.DELETED_AT.isNull());
        if (status != null) {
            condition = condition.and(pp.STATUS.eq(status.name()));
        }

        // 총 개수 조회
        int total = dsl.selectCount()
                .from(pp)
                .where(condition)
                .fetchOne(0, int.class);

        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 페이징된 데이터 조회
        List<ProjectParticipantSummaryVo> participants = dsl.select(
                        pp.ID,
                        pp.PROJECT_ID,
                        pp.MEMBER_ID,
                        m.NAME.as("member_name"),
                        m.GRADE.as("member_grade"),
                        m.PROFILE_IMAGE.as("member_profile_image_url"),
                        p.TITLE.as("project_title"),
                        pp.STATUS,
                        pp.CREATED_AT
                )
                .from(pp)
                .leftJoin(m).on(pp.MEMBER_ID.eq(m.ID))
                .leftJoin(p).on(pp.PROJECT_ID.eq(p.ID))
                .where(condition)
                .orderBy(pp.CREATED_AT.desc())
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetch(mapper::toSummaryVoFromRecord);

        log.info("jOOQ: Found {} participants", total);
        return new PageImpl<>(participants, pageable, total);
    }

    @Override
    public Page<ProjectParticipantSummaryVo> findByProjectId(Long projectId, Pageable pageable) {
        return findByProjectId(projectId, null, pageable);
    }

    @Override
    public Page<ProjectParticipantSummaryVo> findByMemberId(Long memberId, Pageable pageable) {
        log.info("jOOQ: Finding participants by member - memberId: {}", memberId);

        var pp = PROJECT_PARTICIPANT.as("pp");
        var m = MEMBER.as("m");
        var p = PROJECT.as("p");

        Condition condition = pp.MEMBER_ID.eq(memberId).and(pp.DELETED_AT.isNull());

        // 총 개수 조회
        int total = dsl.selectCount()
                .from(pp)
                .where(condition)
                .fetchOne(0, int.class);

        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 페이징된 데이터 조회 (프로젝트 정보 포함)
        List<ProjectParticipantSummaryVo> participants;
        
        if (pageable.isUnpaged()) {
            // Pageable이 unpaged인 경우 페이징 없이 조회
            participants = dsl.select(
                            pp.ID,
                            pp.PROJECT_ID,
                            pp.MEMBER_ID,
                            m.NAME.as("member_name"),
                            m.GRADE.as("member_grade"),
                            m.PROFILE_IMAGE.as("member_profile_image_url"),
                            p.TITLE.as("project_title"), // 프로젝트 제목을 별도 필드로 저장
                            pp.STATUS,
                            pp.CREATED_AT
                    )
                    .from(pp)
                    .leftJoin(p).on(pp.PROJECT_ID.eq(p.ID))
                    .leftJoin(m).on(pp.MEMBER_ID.eq(m.ID))
                    .where(condition)
                    .orderBy(pp.CREATED_AT.desc())
                    .fetch(mapper::toSummaryVoFromRecord);
        } else {
            // Pageable이 페이징된 경우 limit/offset 적용
            participants = dsl.select(
                            pp.ID,
                            pp.PROJECT_ID,
                            pp.MEMBER_ID,
                            m.NAME.as("member_name"),
                            m.GRADE.as("member_grade"),
                            m.PROFILE_IMAGE.as("member_profile_image_url"),
                            p.TITLE.as("project_title"), // 프로젝트 제목을 별도 필드로 저장
                            pp.STATUS,
                            pp.CREATED_AT
                    )
                    .from(pp)
                    .leftJoin(p).on(pp.PROJECT_ID.eq(p.ID))
                    .leftJoin(m).on(pp.MEMBER_ID.eq(m.ID))
                    .where(condition)
                    .orderBy(pp.CREATED_AT.desc())
                    .limit(pageable.getPageSize())
                    .offset((int) pageable.getOffset())
                    .fetch(mapper::toSummaryVoFromRecord);
        }

        log.info("jOOQ: Found {} member participations", total);
        return new PageImpl<>(participants, pageable, total);
    }

    @Override
    public boolean existsByProjectIdAndMemberId(Long projectId, Long memberId) {
        log.info("jOOQ: Checking if participant exists - projectId: {}, memberId: {}", projectId, memberId);

        var pp = PROJECT_PARTICIPANT.as("pp");

        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(pp)
                        .where(pp.PROJECT_ID.eq(projectId))
                        .and(pp.MEMBER_ID.eq(memberId))
                        // Only consider PENDING or APPROVED as existing application/membership
                        .and(pp.STATUS.in(
                                inline(ProjectParticipantStatus.PENDING.name()),
                                inline(ProjectParticipantStatus.APPROVED.name())
                        ))
                        .and(pp.DELETED_AT.isNull())
        );

        log.info("jOOQ: Participant exists: {} - projectId: {}, memberId: {}", exists, projectId, memberId);
        return exists;
    }

    @Override
    public Optional<ProjectParticipantVo> findByProjectIdAndMemberId(Long projectId, Long memberId) {
        log.info("jOOQ: Finding participant by project and member - projectId: {}, memberId: {}",
                projectId, memberId);

        var pp = PROJECT_PARTICIPANT.as("pp");
        var m = MEMBER.as("m");

        Optional<ProjectParticipantVo> result = dsl.select(
                        pp.ID,
                        pp.PROJECT_ID,
                        pp.MEMBER_ID,
                        m.NAME.as("member_name"),
                        pp.STATUS,
                        pp.CREATED_AT,
                        pp.UPDATED_AT
                )
                .from(pp)
                .leftJoin(m).on(pp.MEMBER_ID.eq(m.ID))
                .where(pp.PROJECT_ID.eq(projectId))
                .and(pp.MEMBER_ID.eq(memberId))
                .and(pp.DELETED_AT.isNull())
                .orderBy(pp.UPDATED_AT.desc())
                .limit(1)
                .fetchOptional(mapper::toVoFromRecord);

        log.info("jOOQ: Participant found - projectId: {}, memberId: {}", projectId, memberId);
        return result;
    }

    @Override
    public Optional<ProjectParticipantVo> findPendingByProjectIdAndMemberId(Long projectId, Long memberId) {
        log.info("jOOQ: Finding pending participant - projectId: {}, memberId: {}", projectId, memberId);

        var pp = PROJECT_PARTICIPANT.as("pp");
        var m = MEMBER.as("m");

        Optional<ProjectParticipantVo> result = dsl.select(
                        pp.ID,
                        pp.PROJECT_ID,
                        pp.MEMBER_ID,
                        m.NAME.as("member_name"),
                        pp.STATUS,
                        pp.CREATED_AT,
                        pp.UPDATED_AT
                )
                .from(pp)
                .leftJoin(m).on(pp.MEMBER_ID.eq(m.ID))
                .where(pp.PROJECT_ID.eq(projectId))
                .and(pp.MEMBER_ID.eq(memberId))
                .and(pp.STATUS.eq(ProjectParticipantStatus.PENDING.name()))
                .and(pp.DELETED_AT.isNull())
                .fetchOptional(mapper::toVoFromRecord);

        log.info("jOOQ: Pending participant found - projectId: {}, memberId: {}", projectId, memberId);
        return result;
    }

    @Override
    public long countApprovedParticipantsByProjectId(Long projectId) {
        log.info("jOOQ: Counting approved participants - projectId: {}", projectId);

        var pp = PROJECT_PARTICIPANT.as("pp");

        long count = dsl.selectCount()
                .from(pp)
                .where(pp.PROJECT_ID.eq(projectId))
                .and(pp.STATUS.eq(ProjectParticipantStatus.APPROVED.name()))
                .and(pp.DELETED_AT.isNull())
                .fetchOne(0, long.class);

        log.info("jOOQ: Approved participant count: {} - projectId: {}", count, projectId);
        return count;
    }

    @Override
    public long countPendingParticipantsByProjectId(Long projectId) {
        log.info("jOOQ: Counting pending participants - projectId: {}", projectId);

        var pp = PROJECT_PARTICIPANT.as("pp");

        long count = dsl.selectCount()
                .from(pp)
                .where(pp.PROJECT_ID.eq(projectId))
                .and(pp.STATUS.eq(ProjectParticipantStatus.PENDING.name()))
                .and(pp.DELETED_AT.isNull())
                .fetchOne(0, long.class);

        log.info("jOOQ: Pending participant count: {} - projectId: {}", count, projectId);
        return count;
    }

    @Override
    public List<ProjectParticipantSummaryVo> findAllApprovedByProjectId(Long projectId) {
        log.info("jOOQ: Finding all approved participants by project - projectId: {}", projectId);

        var pp = PROJECT_PARTICIPANT.as("pp");
        var m = MEMBER.as("m");
        var p = PROJECT.as("p");

        List<ProjectParticipantSummaryVo> participants = dsl.select(
                        pp.ID,
                        pp.PROJECT_ID,
                        pp.MEMBER_ID,
                        m.NAME.as("member_name"),
                        m.GRADE.as("member_grade"),
                        m.PROFILE_IMAGE.as("member_profile_image_url"),
                        p.TITLE.as("project_title"),
                        pp.STATUS,
                        pp.CREATED_AT
                )
                .from(pp)
                .leftJoin(m).on(pp.MEMBER_ID.eq(m.ID))
                .leftJoin(p).on(pp.PROJECT_ID.eq(p.ID))
                .where(pp.PROJECT_ID.eq(projectId)
                        .and(pp.STATUS.eq(ProjectParticipantStatus.APPROVED.name()))
                        .and(pp.DELETED_AT.isNull()))
                .orderBy(pp.CREATED_AT.desc())
                .fetch(mapper::toSummaryVoFromRecord);

        log.info("jOOQ: Found {} approved participants", participants.size());
        return participants;
    }

    @Override
    public long countActiveProjectsByMemberId(Long memberId) {
        log.info("jOOQ: Counting active projects for member - memberId: {}", memberId);

        var pp = PROJECT_PARTICIPANT.as("pp");
        var pj = PROJECT.as("p");

        long count = dsl.selectCount()
                .from(pp)
                .join(pj).on(pp.PROJECT_ID.eq(pj.ID))
                .where(pp.MEMBER_ID.eq(memberId))
                .and(pp.STATUS.eq(ProjectParticipantStatus.APPROVED.name()))
                .and(pp.DELETED_AT.isNull())
                .and(pj.DELETED_AT.isNull())
                .and(pj.STARTED_AT.le(currentOffsetDateTime()))
                .and(pj.ENDED_AT.gt(currentOffsetDateTime()))
                .fetchOne(0, long.class);

        log.info("jOOQ: Active projects count: {} - memberId: {}", count, memberId);
        return count;
    }

    @Override
    public boolean isApprovedMember(Long projectId, Long memberId) {
        log.info("jOOQ: Checking if member is approved - projectId: {}, memberId: {}", projectId, memberId);

        var pp = PROJECT_PARTICIPANT.as("pp");

        boolean isApproved = dsl.fetchExists(
                dsl.selectOne()
                        .from(pp)
                        .where(pp.PROJECT_ID.eq(projectId))
                        .and(pp.MEMBER_ID.eq(memberId))
                        .and(pp.STATUS.eq(ProjectParticipantStatus.APPROVED.name()))
                        .and(pp.DELETED_AT.isNull())
        );

        log.info("jOOQ: Member approved status - projectId: {}, memberId: {}, isApproved: {}", 
                projectId, memberId, isApproved);
        return isApproved;
    }

    @Override
    public boolean isMember(Long projectId, Long memberId) {
        log.info("jOOQ: Checking if member exists - projectId: {}, memberId: {}", projectId, memberId);

        var pp = PROJECT_PARTICIPANT.as("pp");

        boolean isMember = dsl.fetchExists(
                dsl.selectOne()
                        .from(pp)
                        .where(pp.PROJECT_ID.eq(projectId))
                        .and(pp.MEMBER_ID.eq(memberId))
                        .and(pp.DELETED_AT.isNull())
        );

        log.info("jOOQ: Member exists status - projectId: {}, memberId: {}, isMember: {}", 
                projectId, memberId, isMember);
        return isMember;
    }
}