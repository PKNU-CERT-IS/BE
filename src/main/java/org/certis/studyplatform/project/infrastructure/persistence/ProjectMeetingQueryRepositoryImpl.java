package org.certis.studyplatform.project.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.project.domain.repository.ProjectMeetingQueryRepository;
import org.certis.studyplatform.project.domain.vo.*;
import org.certis.studyplatform.project.infrastructure.mapper.ProjectMeetingInfrastructureMapper;
import org.jooq.DSLContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import static org.certis.generated.jooq.Tables.*;

import java.util.List;
import java.util.Optional;

/**
 * Project Meeting Query Repository Implementation using Generated jOOQ Tables
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
public class ProjectMeetingQueryRepositoryImpl implements ProjectMeetingQueryRepository {

    private final DSLContext dsl;
    private final ProjectMeetingInfrastructureMapper mapper;

    @Override
    public Optional<ProjectMeetingVo> findById(Long meetingId) {
        log.info("jOOQ: Finding project meeting by ID - {}", meetingId);

        Optional<ProjectMeetingVo> result = dsl.select(
                        PROJECT_MEETING.ID,
                        PROJECT_MEETING.PROJECT_ID,
                        PROJECT_MEETING.TITLE,
                        PROJECT_MEETING.CONTENT,
                        PROJECT_MEETING.PARTICIPANTS,
                        PROJECT_MEETING.MEMBER_ID,
                        PROJECT_MEETING.CREATED_AT,
                        PROJECT_MEETING.UPDATED_AT
                )
                .from(PROJECT_MEETING)
                .where(PROJECT_MEETING.ID.eq(meetingId))
                .and(PROJECT_MEETING.DELETED_AT.isNull())
                .fetchOptional(record -> mapper.toVo(record));

        log.info("jOOQ: Project meeting found - ID: {}", meetingId);
        return result;
    }

    @Override
    public Page<ProjectMeetingSummaryVo> findByProjectId(Long projectId, Pageable pageable) {
        log.info("jOOQ: Clean conversion - projectId: {}", projectId);

        validateProjectExists(projectId);

        var pm = PROJECT_MEETING.as("pm");
        var m = MEMBER.as("m");

        // 전체 개수
        int totalCount = dsl.selectCount()
                .from(pm)
                .where(pm.PROJECT_ID.eq(projectId))
                .and(pm.DELETED_AT.isNull())
                .fetchOne(0, int.class);

        List<ProjectMeetingSummaryVo> summaryVos = dsl.select(
                        pm.ID,
                        pm.TITLE,
                        pm.PARTICIPANTS,
                        pm.MEMBER_ID,
                        pm.CREATED_AT,
                        m.NAME.as("writer_name")
                )
                .from(pm)
                .leftJoin(m).on(pm.MEMBER_ID.eq(m.ID))
                .where(pm.PROJECT_ID.eq(projectId))
                .and(pm.DELETED_AT.isNull())
                .orderBy(pm.CREATED_AT.desc())
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetch(mapper::recordToSummaryVo);

        return new PageImpl<>(summaryVos, pageable, totalCount);
    }

    @Override
    public boolean existsById(Long meetingId) {
        log.info("jOOQ: Checking if project meeting exists - ID: {}", meetingId);

        // ✅ jOOQ 생성 테이블로 존재 여부 확인
        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(PROJECT_MEETING)
                        .where(PROJECT_MEETING.ID.eq(meetingId))
                        .and(PROJECT_MEETING.DELETED_AT.isNull())
        );

        log.info("jOOQ: Project meeting exists: {} - ID: {}", exists, meetingId);
        return exists;
    }

    @Override
    public boolean hasEditPermission(Long meetingId, Long requesterId) {
        log.info("jOOQ: Checking edit permission - meetingId: {}, requesterId: {}", meetingId, requesterId);

        // ✅ jOOQ 생성 테이블로 권한 확인
        boolean hasPermission = dsl.fetchExists(
                dsl.selectOne()
                        .from(PROJECT_MEETING)
                        .where(PROJECT_MEETING.ID.eq(meetingId))
                        .and(PROJECT_MEETING.MEMBER_ID.eq(requesterId))
                        .and(PROJECT_MEETING.DELETED_AT.isNull())
        );

        log.info("jOOQ: Edit permission: {} - meetingId: {}, requesterId: {}", hasPermission, meetingId, requesterId);
        return hasPermission;
    }
    // ================= Private Helper Methods =================

    /**
     * ✅ jOOQ 생성 테이블로 프로젝트 존재 여부 검증
     */
    private boolean validateProjectExists(Long projectId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(PROJECT)
                        .where(PROJECT.ID.eq(projectId))
                        .and(PROJECT.DELETED_AT.isNull())
        );
    }

}