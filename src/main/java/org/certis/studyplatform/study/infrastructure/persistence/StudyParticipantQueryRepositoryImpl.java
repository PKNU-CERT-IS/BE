package org.certis.studyplatform.study.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;
import org.certis.studyplatform.study.domain.StudyStatus;
import org.certis.studyplatform.study.domain.repository.StudyParticipantQueryRepository;
import org.certis.studyplatform.study.domain.vo.StudyParticipantSummaryVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantVo;
import org.certis.studyplatform.study.infrastructure.mapper.StudyInfrastructureMapper;
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
 * Study Participant Query Repository Implementation using Generated jOOQ Tables
 *
 * CQRS Query 측면의 Repository 구현체 (Read 작업)
 * Infrastructure Layer
 * jOOQ를 사용하여 복잡한 조회 쿼리 수행
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudyParticipantQueryRepositoryImpl implements StudyParticipantQueryRepository {

    @Qualifier("jooqDataSource")
    private final DSLContext dsl;

    private final StudyInfrastructureMapper mapper;

    @Override
    public Optional<StudyParticipantVo> findById(Long participantId) {
        log.info("jOOQ: Finding participant by ID - {}", participantId);

        var s = STUDY_PARTICIPANT.as("ss");
        var m = MEMBER.as("m");

        Optional<StudyParticipantVo> result = dsl.select(
                        s.ID,
                        s.STUDY_ID,
                        s.MEMBER_ID,
                        m.NAME.as("member_name"),
                        s.STATUS,
                        s.CREATED_AT,
                        s.UPDATED_AT
                )
                .from(s)
                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                .where(s.ID.eq(participantId))
                .and(s.DELETED_AT.isNull())
                .fetchOptional(mapper::toVoFromRecord);

        log.info("jOOQ: Participant found - ID: {}", participantId);
        return result;
    }

    @Override
    public Page<StudyParticipantSummaryVo> findByStudyId(Long studyId, StudyParticipantStatus status, Pageable pageable) {
        log.info("jOOQ: Finding participants by study - studyId: {}, status: {}", studyId, status);

        var s = STUDY_PARTICIPANT.as("ss");
        var m = MEMBER.as("m");
        var st = STUDY.as("st");

        Condition condition = s.STUDY_ID.eq(studyId).and(s.DELETED_AT.isNull());
        if (status != null) {
            condition = condition.and(s.STATUS.eq(status.name()));
        }

        // 총 개수 조회
        int total = dsl.selectCount()
                .from(s)
                .where(condition)
                .fetchOne(0, int.class);

        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 페이징된 데이터 조회
        List<StudyParticipantSummaryVo> participants = dsl.select(
                        s.ID,
                        s.STUDY_ID,
                        s.MEMBER_ID,
                        m.NAME.as("member_name"),
                        m.GRADE.as("member_grade"),
                        m.PROFILE_IMAGE.as("member_profile_image_url"),
                        st.TITLE.as("study_title"),
                        s.STATUS,
                        s.CREATED_AT
                )
                .from(s)
                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                .leftJoin(st).on(s.STUDY_ID.eq(st.ID))
                .where(condition)
                .orderBy(s.CREATED_AT.desc())
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetch(mapper::toSummaryVoFromRecord);

        log.info("jOOQ: Found {} participants", total);
        return new PageImpl<>(participants, pageable, total);
    }

    @Override
    public Page<StudyParticipantSummaryVo> findByStudyId(Long studyId, Pageable pageable) {
        return findByStudyId(studyId, null, pageable);
    }

    @Override
    public Page<StudyParticipantSummaryVo> findByMemberId(Long memberId, Pageable pageable) {
        log.info("jOOQ: Finding participants by member - memberId: {}", memberId);

        var s = STUDY_PARTICIPANT.as("s");
        var p = STUDY.as("p");
        var m = MEMBER.as("m");

        Condition condition = s.MEMBER_ID.eq(memberId).and(s.DELETED_AT.isNull());

        // 총 개수 조회
        int total = dsl.selectCount()
                .from(s)
                .where(condition)
                .fetchOne(0, int.class);

        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 페이징된 데이터 조회 (스터디 정보 포함)
        List<StudyParticipantSummaryVo> participants;
        
        if (pageable.isUnpaged()) {
            // Pageable이 unpaged인 경우 페이징 없이 조회
                    var query = dsl.select(
                            s.ID,
                            s.STUDY_ID,
                            s.MEMBER_ID,
                            m.NAME.as("member_name"),
                            m.GRADE.as("member_grade"),
                            m.PROFILE_IMAGE.as("member_profile_image_url"),
                            p.TITLE.as("study_title"), // 스터디 제목을 별도 필드로 저장
                            s.STATUS,
                            s.CREATED_AT
                    )
                    .from(s)
                    .leftJoin(p).on(s.STUDY_ID.eq(p.ID))
                    .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                    .where(condition)
                    .orderBy(s.CREATED_AT.desc());
            
            log.info("jOOQ Query: {}", query.getSQL());
            log.info("jOOQ Bind values: {}", query.getBindValues());
            
            participants = query.fetch(mapper::toSummaryVoFromRecord);
        } else {
            // Pageable이 페이징된 경우 limit/offset 적용
            participants = dsl.select(
                            s.ID,
                            s.STUDY_ID,
                            s.MEMBER_ID,
                            m.NAME.as("member_name"),
                            m.GRADE.as("member_grade"),
                            m.PROFILE_IMAGE.as("member_profile_image_url"),
                            p.TITLE.as("study_title"), // 스터디 제목을 별도 필드로 저장
                            s.STATUS,
                            s.CREATED_AT
                    )
                    .from(s)
                    .leftJoin(p).on(s.STUDY_ID.eq(p.ID))
                    .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                    .where(condition)
                    .orderBy(s.CREATED_AT.desc())
                    .limit(pageable.getPageSize())
                    .offset((int) pageable.getOffset())
                    .fetch(mapper::toSummaryVoFromRecord);
        }

        log.info("jOOQ: Found {} member participations", total);
        return new PageImpl<>(participants, pageable, total);
    }

    @Override
    public boolean existsByStudyIdAndMemberId(Long studyId, Long memberId) {
        log.info("jOOQ: Checking if participant exists - studyId: {}, memberId: {}", studyId, memberId);

        var s = STUDY_PARTICIPANT.as("ss");

        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(s)
                        .where(s.STUDY_ID.eq(studyId))
                        .and(s.MEMBER_ID.eq(memberId))
                        // Only consider PENDING or APPROVED as existing application/membership
                        .and(s.STATUS.in(
                                inline(StudyParticipantStatus.PENDING.name()),
                                inline(StudyParticipantStatus.APPROVED.name())
                        ))
                        .and(s.DELETED_AT.isNull())
        );

        log.info("jOOQ: Participant exists: {} - studyId: {}, memberId: {}", exists, studyId, memberId);
        return exists;
    }

    @Override
    public Optional<StudyParticipantVo> findByStudyIdAndMemberId(Long studyId, Long memberId) {
        log.info("jOOQ: Finding participant by study and member - studyId: {}, memberId: {}",
                studyId, memberId);

        var s = STUDY_PARTICIPANT.as("ss");
        var m = MEMBER.as("m");

        Optional<StudyParticipantVo> result = dsl.select(
                        s.ID,
                        s.STUDY_ID,
                        s.MEMBER_ID,
                        m.NAME.as("member_name"),
                        s.STATUS,
                        s.CREATED_AT,
                        s.UPDATED_AT
                )
                .from(s)
                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                .where(s.STUDY_ID.eq(studyId))
                .and(s.MEMBER_ID.eq(memberId))
                .and(s.DELETED_AT.isNull())
                .orderBy(s.UPDATED_AT.desc())
                .limit(1)
                .fetchOptional(mapper::toVoFromRecord);

        log.info("jOOQ: Participant found - studyId: {}, memberId: {}", studyId, memberId);
        return result;
    }

    @Override
    public Optional<StudyParticipantVo> findPendingByStudyIdAndMemberId(Long studyId, Long memberId) {
        log.info("jOOQ: Finding pending participant - studyId: {}, memberId: {}", studyId, memberId);

        var s = STUDY_PARTICIPANT.as("ss");
        var m = MEMBER.as("m");

        Optional<StudyParticipantVo> result = dsl.select(
                        s.ID,
                        s.STUDY_ID,
                        s.MEMBER_ID,
                        m.NAME.as("member_name"),
                        s.STATUS,
                        s.CREATED_AT,
                        s.UPDATED_AT
                )
                .from(s)
                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                .where(s.STUDY_ID.eq(studyId))
                .and(s.MEMBER_ID.eq(memberId))
                .and(s.STATUS.eq(StudyParticipantStatus.PENDING.name()))
                .and(s.DELETED_AT.isNull())
                .fetchOptional(mapper::toVoFromRecord);

        log.info("jOOQ: Pending participant found - studyId: {}, memberId: {}", studyId, memberId);
        return result;
    }

    @Override
    public long countApprovedParticipantsByStudyId(Long studyId) {
        log.info("jOOQ: Counting approved participants - studyId: {}", studyId);

        var s = STUDY_PARTICIPANT.as("ss");

        long count = dsl.selectCount()
                .from(s)
                .where(s.STUDY_ID.eq(studyId))
                .and(s.STATUS.eq(StudyParticipantStatus.APPROVED.name()))
                .and(s.DELETED_AT.isNull())
                .fetchOne(0, long.class);

        log.info("jOOQ: Approved participant count: {} - studyId: {}", count, studyId);
        return count;
    }

    @Override
    public long countPendingParticipantsByStudyId(Long studyId) {
        log.info("jOOQ: Counting pending participants - studyId: {}", studyId);

        var s = STUDY_PARTICIPANT.as("ss");

        long count = dsl.selectCount()
                .from(s)
                .where(s.STUDY_ID.eq(studyId))
                .and(s.STATUS.eq(StudyParticipantStatus.PENDING.name()))
                .and(s.DELETED_AT.isNull())
                .fetchOne(0, long.class);

        log.info("jOOQ: Pending participant count: {} - studyId: {}", count, studyId);
        return count;
    }

    @Override
    public List<StudyParticipantSummaryVo> findAllApprovedByStudyId(Long studyId) {
        log.info("jOOQ: Finding all approved participants by study - studyId: {}", studyId);
        log.debug("=== DEBUG: findAllApprovedByStudyId called with studyId: {} ===", studyId);

        var s = STUDY_PARTICIPANT.as("ss");
        var m = MEMBER.as("m");
        var st = STUDY.as("st");

        try {
            List<StudyParticipantSummaryVo> participants = dsl.select(
                            s.ID,
                            s.STUDY_ID,
                            s.MEMBER_ID,
                            m.NAME.as("member_name"),
                            m.GRADE.as("member_grade"),
                            m.PROFILE_IMAGE.as("member_profile_image_url"),
                            st.TITLE.as("study_title"),
                            s.STATUS,
                            s.CREATED_AT
                    )
                    .from(s)
                    .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                    .leftJoin(st).on(s.STUDY_ID.eq(st.ID))
                    .where(s.STUDY_ID.eq(studyId)
                            .and(s.STATUS.eq(StudyParticipantStatus.APPROVED.name()))
                            .and(s.DELETED_AT.isNull()))
                    .orderBy(s.CREATED_AT.desc())
                    .fetch(mapper::toSummaryVoFromRecord);

            log.info("jOOQ: Found {} approved participants", participants.size());
            log.debug("=== DEBUG: Found {} approved participants ===", participants.size());
            for (StudyParticipantSummaryVo participant : participants) {
                log.debug("=== DEBUG: Participant - memberId: {}, status: {} ===", participant.memberId(), participant.status());
            }
            return participants;
        } catch (Exception e) {
            log.error("jOOQ: Error finding approved participants - studyId: {}, error: {}", studyId, e.getMessage(), e);
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public long countActiveStudiesByMemberId(Long memberId) {
        log.info("jOOQ: Counting active studies for member - memberId: {}", memberId);

        var s = STUDY_PARTICIPANT.as("sp");
        var st = STUDY.as("s");

        long count = dsl.selectCount()
                .from(s)
                .join(st).on(s.STUDY_ID.eq(st.ID))
                .where(s.MEMBER_ID.eq(memberId))
                .and(s.STATUS.eq(StudyParticipantStatus.APPROVED.name()))
                .and(s.DELETED_AT.isNull())
                .and(st.DELETED_AT.isNull())
                .and(st.ENDED_AT.greaterOrEqual(currentOffsetDateTime()))
                .and(st.STATUS.in(
                        StudyStatus.READY.name(),
                        StudyStatus.APPROVED.name(),
                        StudyStatus.INPROGRESS.name()
                ))
                .fetchOne(0, long.class);

        log.info("jOOQ: Active studies count: {} - memberId: {}", count, memberId);
        return count;
    }
}
