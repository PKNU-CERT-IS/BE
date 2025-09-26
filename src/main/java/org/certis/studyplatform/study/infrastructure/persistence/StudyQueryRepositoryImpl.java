package org.certis.studyplatform.study.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;
import org.certis.studyplatform.study.domain.vo.*;
import org.certis.studyplatform.study.infrastructure.mapper.StudyInfrastructureMapper;
import org.jooq.*;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.certis.studyplatform.member.domain.MemberGrade;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;

import static org.certis.generated.jooq.Tables.*;
import static org.jooq.impl.DSL.*;

/**
 * Study Query Repository Implementation using Generated jOOQ Tables
 *
 * ✅ 일관된 코드 스타일:
 * - Optional.of() 체이닝 방식
 * - .fetch().into(Record.class) 변환
 * - .filter() 빈 결과 처리
 * - .map() 함수형 변환
 * - StudyAttached 정보 포함
 *
 * ✅ CQRS 패턴에서 Query 전용 Repository
 * Infrastructure Layer
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudyQueryRepositoryImpl implements StudyQueryRepository {

    @Qualifier("jooqDataSource")
    private final DSLContext dsl;

    private final StudyInfrastructureMapper mapper;
    @Override
    public Optional<StudyEndSubmissionInfoVo> getEndSubmissionInfo(Long studyId) {
        var s = STUDY.as("s");
        var m = MEMBER.as("m");
        return Optional.ofNullable(
                dsl.select(
                            s.ID,
                            s.STATUS.as("status"),
                            s.RESULT_SUBMIT_STATUS,
                            s.RESULT_SUBMITTED_AT,
                            s.RESULT_ATTACHED_URL,
                            s.CATEGORY,
                            s.SUBCATEGORY,
                            s.TITLE,
                            s.DESCRIPTION,
                            s.MEMBER_ID,
                            m.NAME,
                            m.GRADE,
                            s.STARTED_AT,
                            s.ENDED_AT,
                            select(count())
                                    .from(STUDY_PARTICIPANT)
                                    .where(STUDY_PARTICIPANT.STUDY_ID.eq(s.ID))
                                    .and(STUDY_PARTICIPANT.STATUS.eq(StudyParticipantStatus.APPROVED.name()))
                                    .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                                    .asField("current_participants"),
                            s.MAX_PARTICIPANTS_NUMBER
                        )
                        .from(s)
                        .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                        .where(s.ID.eq(studyId))
                        .and(s.DELETED_AT.isNull())
                        .fetchOne()
        ).map(r -> {
            String resultSubmitStatusString = r.get(s.RESULT_SUBMIT_STATUS);
            ResultSubmitStatus resultSubmitStatus = resultSubmitStatusString != null ? ResultSubmitStatus.valueOf(resultSubmitStatusString) : null;
            org.certis.studyplatform.study.domain.StudyStatus studyStatus = r.get("status", org.certis.studyplatform.study.domain.StudyStatus.class);
            return new StudyEndSubmissionInfoVo(
                    r.get(s.ID),
                    studyStatus,
                    resultSubmitStatus,
                    r.get(s.RESULT_SUBMITTED_AT),
                    r.get(s.RESULT_ATTACHED_URL),
                    r.get(s.CATEGORY),
                    r.get(s.SUBCATEGORY),
                    r.get(s.TITLE),
                    r.get(s.DESCRIPTION),
                    r.get(s.MEMBER_ID),
                    r.get(m.NAME),
                    r.get(m.GRADE, MemberGrade.class),
                    r.get(s.STARTED_AT),
                    r.get(s.ENDED_AT),
                    r.get("current_participants", Integer.class),
                    r.get(s.MAX_PARTICIPANTS_NUMBER)
            );
        });
    }

    @Override
    public Optional<StudyVo> findStudyDetailById(Long studyId) {
        log.info("jOOQ: Finding study detail by ID - {}", studyId);

        var s = STUDY.as("s");
        var m = MEMBER.as("m");
        var sa = STUDY_ATTACHED.as("sa");

        Optional<StudyVo> result = Optional.of(
                        dsl.select(
                                        s.ID,
                                        s.STATUS.as("status"),
                                        s.MEMBER_ID,
                                        m.NAME.as("creator_name"),
                                        m.GRADE.as("creator_grade"),
                                        s.TITLE,
                                        s.DESCRIPTION,
                                        s.CONTENT,
                                        s.CATEGORY,
                                        s.SUBCATEGORY,
                                        s.MAX_PARTICIPANTS_NUMBER,
                                        s.RESULT_SUBMIT_STATUS.as("result_submit_status"),
                                        s.STARTED_AT,
                                        s.ENDED_AT,
                                        s.CREATED_AT,
                                        s.UPDATED_AT,
                                        // 현재 참여자 수 서브쿼리
                                        select(count())
                                                .from(STUDY_PARTICIPANT)
                                                .where(STUDY_PARTICIPANT.STUDY_ID.eq(s.ID))
                                                .and(STUDY_PARTICIPANT.STATUS.eq(StudyParticipantStatus.APPROVED.name()))
                                                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                                                .asField("current_participants"),
                                        // StudyAttached 정보
                                        sa.ID.as("attached_id"),
                                        sa.NAME.as("attached_name"),
                                        sa.TYPE.as("attached_type"),
                                        sa.SIZE.as("attached_size"),
                                        sa.ATTACHED_URL
                                )
                                .from(s)
                                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                                .leftJoin(sa).on(s.ID.eq(sa.STUDY_ID).and(sa.DELETED_AT.isNull()))
                                .where(s.ID.eq(studyId))
                                .and(s.DELETED_AT.isNull())
                                .fetch()
                                .stream()
                                .map(record -> (Record) record)
                                .toList()
                ).filter(records -> !records.isEmpty())
                .map(records -> mapper.toStudyVoFromRecordsWithAttachments(records));

        log.info("jOOQ: Study detail found - ID: {}", studyId);
        return result;
    }

    @Override
    public StudySearchResultVo findStudies(StudySearchCriteriaVo criteria, Pageable pageable) {
        log.info("jOOQ: Finding studies with criteria - {}", criteria);
        log.debug("Pageable info - page: {}, size: {}, sort: {}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        var s = STUDY.as("s");
        var m = MEMBER.as("m");

        // 동적 조건 구성
        Condition conditions = buildSearchConditions(criteria);
        log.debug("jOOQ: Search conditions built successfully");

        // 총 개수 조회
        int total = dsl.select(countDistinct(s.ID))
                .from(s)
                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                .where(conditions.and(s.DELETED_AT.isNull()))
                .fetchOne(0, int.class);

        log.debug("jOOQ: Count query result: {} total studies found", total);

        if (total == 0) {
            log.debug("jOOQ: No studies found matching criteria, returning empty result");
            return StudySearchResultVo.empty(pageable.getPageSize());
        }

        // 페이징된 데이터 조회 with StudyAttached
        List<StudySummaryVo> studySummaries = Optional.of(
                        dsl.select(
                                        s.ID,
                                        s.STATUS.as("status"),
                                        s.TITLE,
                                        s.DESCRIPTION,
                                        s.CATEGORY,
                                        s.SUBCATEGORY,
                                        s.STARTED_AT,
                                        s.ENDED_AT,
                                        s.MEMBER_ID,
                                        m.NAME.as("creator_name"),
                                        m.GRADE.as("creator_grade"),
                                        s.MAX_PARTICIPANTS_NUMBER,
                                        s.RESULT_SUBMIT_STATUS.as("result_submit_status"),
                                        s.DELETED_AT.as("deleted_at"),
                                        // 현재 참여자 수 서브쿼리
                                        select(count())
                                                .from(STUDY_PARTICIPANT)
                                                .where(STUDY_PARTICIPANT.STUDY_ID.eq(s.ID))
                                                .and(STUDY_PARTICIPANT.STATUS.eq(StudyParticipantStatus.APPROVED.name()))
                                                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                                                .asField("current_participants"),
                                        // StudyAttached 정보
                                        STUDY_ATTACHED.ID.as("attached_id"),
                                        STUDY_ATTACHED.NAME.as("attached_name"),
                                        STUDY_ATTACHED.TYPE.as("attached_type"),
                                        STUDY_ATTACHED.SIZE.as("attached_size"),
                                        STUDY_ATTACHED.ATTACHED_URL
                                )
                                .from(s)
                                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                                .leftJoin(STUDY_ATTACHED).on(s.ID.eq(STUDY_ATTACHED.STUDY_ID)
                                        .and(STUDY_ATTACHED.DELETED_AT.isNull()))
                                .where(conditions.and(s.DELETED_AT.isNull()))
                                .orderBy(buildOrderBy(pageable))
                                .limit(pageable.getPageSize())
                                .offset((int) pageable.getOffset())
                                .fetch()
                                .stream()
                                .map(record -> (Record) record)
                                .toList()
                ).filter(records -> !records.isEmpty())
                .map(records -> mapper.groupRecordsByStudyIdToSummaryVos(records))
                .orElse(List.of());

        log.debug("jOOQ: Data query executed successfully, found {} study summaries",
                studySummaries.size());

        int totalPages = (int) Math.ceil((double) total / pageable.getPageSize());

        log.info("jOOQ: Found {} studies", total);

        return StudySearchResultVo.of(
                studySummaries,
                (long) total,
                totalPages,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
    }

    @Override
    public StudySearchResultVo findStudiesByMemberId(Long memberId, Pageable pageable) {
        log.info("jOOQ: Finding studies by memberId - {}", memberId);

        var s = STUDY.as("s");
        var m = MEMBER.as("m");
        var sa = STUDY_ATTACHED.as("sa");

        Condition condition = s.MEMBER_ID.eq(memberId);

        int total = dsl.selectCount()
                .from(s)
                .where(condition.and(s.DELETED_AT.isNull()))
                .fetchOne(0, int.class);

        if (total == 0) {
            return StudySearchResultVo.empty(pageable.getPageSize());
        }

        List<StudySummaryVo> studySummaries = Optional.of(
                        dsl.select(
                                        s.ID,
                                        s.STATUS.as("status"),
                                        s.TITLE,
                                        s.DESCRIPTION,
                                        s.CATEGORY,
                                        s.SUBCATEGORY,
                                        s.STARTED_AT,
                                        s.ENDED_AT,
                                        s.MEMBER_ID,
                                        m.NAME.as("creator_name"),
                                        m.GRADE.as("creator_grade"),
                                        s.MAX_PARTICIPANTS_NUMBER,
                                        s.RESULT_SUBMIT_STATUS.as("result_submit_status"),
                                        select(count())
                                                .from(STUDY_PARTICIPANT)
                                                .where(STUDY_PARTICIPANT.STUDY_ID.eq(s.ID))
                                                .and(STUDY_PARTICIPANT.STATUS.eq(StudyParticipantStatus.APPROVED.name()))
                                                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                                                .asField("current_participants"),
                                        sa.ID.as("attached_id"),
                                        sa.NAME.as("attached_name"),
                                        sa.TYPE.as("attached_type"),
                                        sa.SIZE.as("attached_size"),
                                        sa.ATTACHED_URL
                                )
                                .from(s)
                                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                                .leftJoin(sa).on(s.ID.eq(sa.STUDY_ID).and(sa.DELETED_AT.isNull()))
                                .where(condition.and(s.DELETED_AT.isNull()))
                                .orderBy(s.CREATED_AT.desc())
                                .limit(pageable.getPageSize())
                                .offset((int) pageable.getOffset())
                                .fetch()
                                .stream()
                                .map(record -> (Record) record)
                                .toList()
                ).filter(records -> !records.isEmpty())
                .map(records -> mapper.groupRecordsByStudyIdToSummaryVos(records))
                .orElse(List.of());

        return createSearchResult(studySummaries, total, pageable);
    }

    @Override
    public StudySearchResultVo findStudiesByCategory(String category, Pageable pageable) {
        log.info("jOOQ: Finding studies by category - {}", category);

        var s = STUDY.as("s");
        var m = MEMBER.as("m");
        var sa = STUDY_ATTACHED.as("sa");

        Condition condition = s.CATEGORY.eq(category);

        int total = dsl.selectCount()
                .from(s)
                .where(condition.and(s.DELETED_AT.isNull()))
                .fetchOne(0, int.class);

        if (total == 0) {
            return StudySearchResultVo.empty(pageable.getPageSize());
        }

        List<StudySummaryVo> studySummaries = Optional.of(
                        dsl.select(
                                        s.ID,
                                        s.STATUS.as("status"),
                                        s.TITLE,
                                        s.DESCRIPTION,
                                        s.CATEGORY,
                                        s.SUBCATEGORY,
                                        s.STARTED_AT,
                                        s.ENDED_AT,
                                        s.MEMBER_ID,
                                        m.NAME.as("creator_name"),
                                        m.GRADE.as("creator_grade"),
                                        s.MAX_PARTICIPANTS_NUMBER,
                                        s.RESULT_SUBMIT_STATUS.as("result_submit_status"),
                                        select(count())
                                                .from(STUDY_PARTICIPANT)
                                                .where(STUDY_PARTICIPANT.STUDY_ID.eq(s.ID))
                                                .and(STUDY_PARTICIPANT.STATUS.eq(StudyParticipantStatus.APPROVED.name()))
                                                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                                                .asField("current_participants"),
                                        sa.ID.as("attached_id"),
                                        sa.NAME.as("attached_name"),
                                        sa.TYPE.as("attached_type"),
                                        sa.SIZE.as("attached_size"),
                                        sa.ATTACHED_URL
                                )
                                .from(s)
                                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                                .leftJoin(sa).on(s.ID.eq(sa.STUDY_ID).and(sa.DELETED_AT.isNull()))
                                .where(condition.and(s.DELETED_AT.isNull()))
                                .orderBy(s.CREATED_AT.desc())
                                .limit(pageable.getPageSize())
                                .offset((int) pageable.getOffset())
                                .fetch()
                                .stream()
                                .map(record -> (Record) record)
                                .toList()
                ).filter(records -> !records.isEmpty())
                .map(records -> mapper.groupRecordsByStudyIdToSummaryVos(records))
                .orElse(List.of());

        return createSearchResult(studySummaries, total, pageable);
    }

    @Override
    public StudySearchResultVo findActiveStudies(Pageable pageable) {
        log.info("jOOQ: Finding active studies");

        var s = STUDY.as("s");
        var m = MEMBER.as("m");
        var sa = STUDY_ATTACHED.as("sa");

        OffsetDateTime now = OffsetDateTime.now();
        Condition condition = s.STARTED_AT.lessOrEqual(now)
                .and(s.ENDED_AT.greaterOrEqual(now));

        int total = dsl.selectCount()
                .from(s)
                .where(condition.and(s.DELETED_AT.isNull()))
                .fetchOne(0, int.class);

        if (total == 0) {
            return StudySearchResultVo.empty(pageable.getPageSize());
        }

        List<StudySummaryVo> studySummaries = Optional.of(
                        dsl.select(
                                        s.ID,
                                        s.STATUS.as("status"),
                                        s.TITLE,
                                        s.DESCRIPTION,
                                        s.CATEGORY,
                                        s.SUBCATEGORY,
                                        s.STARTED_AT,
                                        s.ENDED_AT,
                                        s.MEMBER_ID,
                                        m.NAME.as("creator_name"),
                                        m.GRADE.as("creator_grade"),
                                        s.MAX_PARTICIPANTS_NUMBER,
                                        s.RESULT_SUBMIT_STATUS.as("result_submit_status"),
                                        select(count())
                                                .from(STUDY_PARTICIPANT)
                                                .where(STUDY_PARTICIPANT.STUDY_ID.eq(s.ID))
                                                .and(STUDY_PARTICIPANT.STATUS.eq(StudyParticipantStatus.APPROVED.name()))
                                                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                                                .asField("current_participants"),
                                        sa.ID.as("attached_id"),
                                        sa.NAME.as("attached_name"),
                                        sa.TYPE.as("attached_type"),
                                        sa.SIZE.as("attached_size"),
                                        sa.ATTACHED_URL
                                )
                                .from(s)
                                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                                .leftJoin(sa).on(s.ID.eq(sa.STUDY_ID).and(sa.DELETED_AT.isNull()))
                                .where(condition.and(s.DELETED_AT.isNull()))
                                .orderBy(s.CREATED_AT.desc())
                                .limit(pageable.getPageSize())
                                .offset((int) pageable.getOffset())
                                .fetch()
                                .stream()
                                .map(record -> (Record) record)
                                .toList()
                ).filter(records -> !records.isEmpty())
                .map(records -> mapper.groupRecordsByStudyIdToSummaryVos(records))
                .orElse(List.of());

        return createSearchResult(studySummaries, total, pageable);
    }

    @Override
    public java.util.List<StudyEndSubmissionInfoVo> findEndSubmissionsInProgress() {
        var s = STUDY.as("s");
        var m = MEMBER.as("m");
        return dsl.select(
                        s.ID,
                        s.STATUS.as("status"),
                        s.RESULT_SUBMIT_STATUS,
                        s.RESULT_SUBMITTED_AT,
                        s.RESULT_ATTACHED_URL,
                        s.CATEGORY,
                        s.SUBCATEGORY,
                        s.TITLE,
                        s.DESCRIPTION,
                        s.MEMBER_ID,
                        m.NAME,
                        m.GRADE,
                        s.STARTED_AT,
                        s.ENDED_AT,
                        select(count())
                                .from(STUDY_PARTICIPANT)
                                .where(STUDY_PARTICIPANT.STUDY_ID.eq(s.ID))
                                .and(STUDY_PARTICIPANT.STATUS.eq(StudyParticipantStatus.APPROVED.name()))
                                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                                .asField("current_participants"),
                        s.MAX_PARTICIPANTS_NUMBER
                )
                .from(s)
                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                .where(s.RESULT_SUBMIT_STATUS.eq(org.certis.studyplatform.shared.domain.ResultSubmitStatus.INPROGRESS.name()))
                .and(s.DELETED_AT.isNull())
                .orderBy(s.RESULT_SUBMITTED_AT.desc())
                .fetch(r -> new StudyEndSubmissionInfoVo(
                        r.get(s.ID),
                        r.get("status", org.certis.studyplatform.study.domain.StudyStatus.class),
                        org.certis.studyplatform.shared.domain.ResultSubmitStatus.valueOf(r.get(s.RESULT_SUBMIT_STATUS)),
                        r.get(s.RESULT_SUBMITTED_AT),
                        r.get(s.RESULT_ATTACHED_URL),
                        r.get(s.CATEGORY),
                        r.get(s.SUBCATEGORY),
                        r.get(s.TITLE),
                        r.get(s.DESCRIPTION),
                        r.get(s.MEMBER_ID),
                        r.get(m.NAME),
                        r.get(m.GRADE, MemberGrade.class),
                        r.get(s.STARTED_AT),
                        r.get(s.ENDED_AT),
                        r.get("current_participants", Integer.class),
                        r.get(s.MAX_PARTICIPANTS_NUMBER)
                ));
    }

    @Override
    public StudySearchResultVo findStudiesByKeyword(String keyword, Pageable pageable) {
        log.info("jOOQ: Finding studies by keyword - {}", keyword);

        var s = STUDY.as("s");
        var m = MEMBER.as("m");
        var sa = STUDY_ATTACHED.as("sa");

        String likeKeyword = "%" + keyword + "%";
        Condition condition = s.TITLE.likeIgnoreCase(likeKeyword)
                .or(s.DESCRIPTION.likeIgnoreCase(likeKeyword))
                .or(s.CATEGORY.likeIgnoreCase(likeKeyword))
                .or(s.SUBCATEGORY.likeIgnoreCase(likeKeyword))
                .or(m.NAME.likeIgnoreCase(likeKeyword));

        int total = dsl.select(countDistinct(s.ID))
                .from(s)
                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                .where(condition.and(s.DELETED_AT.isNull()))
                .fetchOne(0, int.class);

        if (total == 0) {
            return StudySearchResultVo.empty(pageable.getPageSize());
        }

        List<StudySummaryVo> studySummaries = Optional.of(
                        dsl.select(
                                        s.ID,
                                        s.TITLE,
                                        s.DESCRIPTION,
                                        s.CATEGORY,
                                        s.SUBCATEGORY,
                                        s.STARTED_AT,
                                        s.ENDED_AT,
                                        s.MEMBER_ID,
                                        m.NAME.as("creator_name"),
                                        m.GRADE.as("creator_grade"),
                                        s.MAX_PARTICIPANTS_NUMBER,
                                        s.RESULT_SUBMIT_STATUS.as("result_submit_status"),
                                        select(count())
                                                .from(STUDY_PARTICIPANT)
                                                .where(STUDY_PARTICIPANT.STUDY_ID.eq(s.ID))
                                                .and(STUDY_PARTICIPANT.STATUS.eq(StudyParticipantStatus.APPROVED.name()))
                                                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                                                .asField("current_participants"),
                                        sa.ID.as("attached_id"),
                                        sa.NAME.as("attached_name"),
                                        sa.TYPE.as("attached_type"),
                                        sa.SIZE.as("attached_size"),
                                        sa.ATTACHED_URL
                                )
                                .from(s)
                                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                                .leftJoin(sa).on(s.ID.eq(sa.STUDY_ID).and(sa.DELETED_AT.isNull()))
                                .where(condition.and(s.DELETED_AT.isNull()))
                                .orderBy(s.CREATED_AT.desc())
                                .limit(pageable.getPageSize())
                                .offset((int) pageable.getOffset())
                                .fetch()
                                .stream()
                                .map(record -> (Record) record)
                                .toList()
                ).filter(records -> !records.isEmpty())
                .map(records -> mapper.groupRecordsByStudyIdToSummaryVos(records))
                .orElse(List.of());

        return createSearchResult(studySummaries, total, pageable);
    }

    @Override
    public StudySearchResultVo findStudiesBySkills(List<String> skills, Pageable pageable) {
        log.info("jOOQ: Finding studies by skills - {}", skills);

        var s = STUDY.as("s");
        var m = MEMBER.as("m");
        var sa = STUDY_ATTACHED.as("sa");

        String[] skillsArray = convertSkillsToArray(skills);

        // ✅ jOOQ로 PostgreSQL 배열 연산자 사용 (타입 안전)
        Condition condition = condition("s.skills && CAST(? AS text[])", (Object) skillsArray);

        int total = dsl.selectCount()
                .from(s)
                .where(condition.and(s.DELETED_AT.isNull()))
                .fetchOne(0, int.class);

        if (total == 0) {
            return StudySearchResultVo.empty(pageable.getPageSize());
        }

        List<StudySummaryVo> studySummaries = Optional.of(
                        dsl.select(
                                        s.ID,
                                        s.TITLE,
                                        s.DESCRIPTION,
                                        s.CATEGORY,
                                        s.SUBCATEGORY,
                                        s.STARTED_AT,
                                        s.ENDED_AT,
                                        s.MEMBER_ID,
                                        m.NAME.as("creator_name"),
                                        m.GRADE.as("creator_grade"),
                                        s.MAX_PARTICIPANTS_NUMBER,
                                        s.RESULT_SUBMIT_STATUS.as("result_submit_status"),
                                        select(count())
                                                .from(STUDY_PARTICIPANT)
                                                .where(STUDY_PARTICIPANT.STUDY_ID.eq(s.ID))
                                                .and(STUDY_PARTICIPANT.STATUS.eq(StudyParticipantStatus.APPROVED.name()))
                                                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                                                .asField("current_participants"),
                                        sa.ID.as("attached_id"),
                                        sa.NAME.as("attached_name"),
                                        sa.TYPE.as("attached_type"),
                                        sa.SIZE.as("attached_size"),
                                        sa.ATTACHED_URL
                                )
                                .from(s)
                                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                                .leftJoin(sa).on(s.ID.eq(sa.STUDY_ID).and(sa.DELETED_AT.isNull()))
                                .where(condition.and(s.DELETED_AT.isNull()))
                                .orderBy(s.CREATED_AT.desc())
                                .limit(pageable.getPageSize())
                                .offset((int) pageable.getOffset())
                                .fetch()
                                .stream()
                                .map(record -> (Record) record)
                                .toList()
                ).filter(records -> !records.isEmpty())
                .map(records -> mapper.groupRecordsByStudyIdToSummaryVos(records))
                .orElse(List.of());

        return createSearchResult(studySummaries, total, pageable);
    }

    @Override
    public Page<StudySummaryVo> findCompletedStudiesByMember(Long memberId, Pageable pageable) {
        log.info("Repository: Finding completed studies by member - memberId: {}", memberId);

        var s = STUDY.as("s");
        var m = MEMBER.as("m");
        var sa = STUDY_ATTACHED.as("sa");

        // 전체 개수 조회
        int totalCount = dsl.selectCount()
                .from(s)
                .where(s.MEMBER_ID.eq(memberId))
                .and(s.DELETED_AT.isNull())
                .and(s.ENDED_AT.lessThan(OffsetDateTime.now())) // 완료 조건
                .fetchOne(0, int.class);

        if (totalCount == 0) {
            return Page.empty(pageable);
        }

        // 데이터 조회 - StudySummaryVo 구조에 맞게 수정
        List<StudySummaryVo> studies = Optional.of(
                        dsl.select(
                                        s.ID,
                                        s.STATUS.as("status"),
                                        s.TITLE,
                                        s.DESCRIPTION,
                                        s.CATEGORY,
                                        s.SUBCATEGORY,
                                        s.STARTED_AT,
                                        s.ENDED_AT,
                                        s.MEMBER_ID,
                                        m.NAME.as("creator_name"),
                                        m.GRADE.as("creator_grade"),
                                        s.MAX_PARTICIPANTS_NUMBER,
                                        select(count())
                                                .from(STUDY_PARTICIPANT)
                                                .where(STUDY_PARTICIPANT.STUDY_ID.eq(s.ID))
                                                .and(STUDY_PARTICIPANT.STATUS.eq(StudyParticipantStatus.APPROVED.name()))
                                                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                                                .asField("current_participants"),
                                        sa.ID.as("attached_id"),
                                        sa.NAME.as("attached_name"),
                                        sa.TYPE.as("attached_type"),
                                        sa.SIZE.as("attached_size"),
                                        sa.ATTACHED_URL
                                )
                                .from(s)
                                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                                .leftJoin(sa).on(s.ID.eq(sa.STUDY_ID).and(sa.DELETED_AT.isNull()))
                                .where(s.MEMBER_ID.eq(memberId))
                                .and(s.DELETED_AT.isNull())
                                .and(s.ENDED_AT.lessThan(OffsetDateTime.now()))
                                .orderBy(s.ENDED_AT.desc()) // 최근 완료된 순
                                .limit(pageable.getPageSize())
                                .offset((int) pageable.getOffset())
                                .fetch()
                                .stream()
                                .map(record -> (Record) record)
                                .toList()
                ).filter(records -> !records.isEmpty())
                .map(records -> mapper.groupRecordsByStudyIdToSummaryVos(records))
                .orElse(List.of());

        return new PageImpl<>(studies, pageable, totalCount);
    }

    @Override
    public List<StudySummaryVo> findCompletedStudiesListByMember(Long memberId) {
        log.info("Repository: Finding completed studies list by member - memberId: {}", memberId);

        var s = STUDY.as("s");
        var m = MEMBER.as("m");
        var sa = STUDY_ATTACHED.as("sa");

        return Optional.of(
                        dsl.select(
                                        s.ID,
                                        s.STATUS.as("status"),
                                        s.TITLE,
                                        s.DESCRIPTION,
                                        s.CATEGORY,
                                        s.SUBCATEGORY,
                                        s.STARTED_AT,
                                        s.ENDED_AT,
                                        s.MEMBER_ID,
                                        m.NAME.as("creator_name"),
                                        m.GRADE.as("creator_grade"),
                                        s.MAX_PARTICIPANTS_NUMBER,
                                        select(count())
                                                .from(STUDY_PARTICIPANT)
                                                .where(STUDY_PARTICIPANT.STUDY_ID.eq(s.ID))
                                                .and(STUDY_PARTICIPANT.STATUS.eq(StudyParticipantStatus.APPROVED.name()))
                                                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                                                .asField("current_participants"),
                                        sa.ID.as("attached_id"),
                                        sa.NAME.as("attached_name"),
                                        sa.TYPE.as("attached_type"),
                                        sa.SIZE.as("attached_size"),
                                        sa.ATTACHED_URL
                                )
                                .from(s)
                                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                                .leftJoin(sa).on(s.ID.eq(sa.STUDY_ID).and(sa.DELETED_AT.isNull()))
                                .where(s.MEMBER_ID.eq(memberId))
                                .and(s.DELETED_AT.isNull())
                                .and(s.ENDED_AT.lessThan(OffsetDateTime.now()))
                                .orderBy(s.ENDED_AT.desc())
                                .fetch()
                                .stream()
                                .map(record -> (Record) record)
                                .toList()
                ).filter(records -> !records.isEmpty())
                .map(mapper::groupRecordsByStudyIdToSummaryVos)
                .orElse(List.of());
    }

    // ================= Additional Helper Methods for Domain Service =================

    @Override
    public boolean existsByTitle(String title) {
        log.info("jOOQ: Checking if study exists by title - {}", title);

        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(STUDY)
                        .where(STUDY.TITLE.eq(title))
                        .and(STUDY.DELETED_AT.isNull())
        );

        log.info("jOOQ: Study exists by title: {} - title: {}", exists, title);
        return exists;
    }

    @Override
    public boolean existsByTitleAndIdNot(String title, Long studyId) {
        log.info("jOOQ: Checking if study exists by title excluding ID - title: {}, excludeId: {}",
                title, studyId);

        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(STUDY)
                        .where(STUDY.TITLE.eq(title))
                        .and(STUDY.ID.ne(studyId))
                        .and(STUDY.DELETED_AT.isNull())
        );

        log.info("jOOQ: Study exists by title (excluding ID): {} - title: {}, excludeId: {}",
                exists, title, studyId);
        return exists;
    }

    @Override
    public Optional<StudyVo> findById(Long studyId) {
        log.info("jOOQ: Finding study VO by ID - {}", studyId);

        var s = STUDY.as("s");
        var m = MEMBER.as("m");
        var sa = STUDY_ATTACHED.as("sa");

        Optional<StudyVo> result = Optional.of(
                        dsl.select(
                                        s.ID,
                                        s.STATUS.as("status"),
                                        s.TITLE,
                                        s.DESCRIPTION,
                                        s.CONTENT,
                                        s.CATEGORY,
                                        s.SUBCATEGORY,
                                        s.STARTED_AT,
                                        s.ENDED_AT,
                                        s.MEMBER_ID,
                                        m.NAME.as("creator_name"),
                                        m.GRADE.as("creator_grade"),
                                        s.MAX_PARTICIPANTS_NUMBER,
                                        s.DELETED_AT.as("deleted_at"),
                                        s.CREATED_AT,
                                        s.UPDATED_AT,
                                        // 현재 참여자 수 서브쿼리
                                        select(count())
                                                .from(STUDY_PARTICIPANT)
                                                .where(STUDY_PARTICIPANT.STUDY_ID.eq(s.ID))
                                                .and(STUDY_PARTICIPANT.STATUS.eq(StudyParticipantStatus.APPROVED.name()))
                                                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                                                .asField("current_participants"),
                                        sa.ID.as("attached_id"),
                                        sa.NAME.as("attached_name"),
                                        sa.TYPE.as("attached_type"),
                                        sa.SIZE.as("attached_size"),
                                        sa.ATTACHED_URL
                                )
                                .from(s)
                                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                                .leftJoin(sa).on(s.ID.eq(sa.STUDY_ID).and(sa.DELETED_AT.isNull()))
                                .where(s.ID.eq(studyId))
                                .fetch()
                                .stream()
                                .map(record -> (Record) record)
                                .toList()
                ).filter(records -> !records.isEmpty())
                .map(records -> mapper.toStudyVoFromRecordsWithAttachments(records));

        log.info("jOOQ: Study VO found - ID: {}", studyId);
        return result;
    }

    @Override
    public Optional<StudyVo> findByIdAndDeletedAtIsNull(Long studyId) {
        log.info("jOOQ: Finding study VO by ID (non-deleted) - {}", studyId);

        var s = STUDY.as("s");
        var m = MEMBER.as("m");
        var sa = STUDY_ATTACHED.as("sa");

        Optional<StudyVo> result = Optional.of(
                        dsl.select(
                                        s.ID,
                                        s.STATUS.as("status"),
                                        s.TITLE,
                                        s.DESCRIPTION,
                                        s.CONTENT,
                                        s.CATEGORY,
                                        s.SUBCATEGORY,
                                        s.STARTED_AT,
                                        s.ENDED_AT,
                                        s.MEMBER_ID,
                                        m.NAME.as("creator_name"),
                                        m.GRADE.as("creator_grade"),
                                        s.MAX_PARTICIPANTS_NUMBER,
                                        s.DELETED_AT.as("deleted_at"),
                                        s.CREATED_AT,
                                        s.UPDATED_AT,
                                        // 현재 참여자 수 서브쿼리
                                        select(count())
                                                .from(STUDY_PARTICIPANT)
                                                .where(STUDY_PARTICIPANT.STUDY_ID.eq(s.ID))
                                                .and(STUDY_PARTICIPANT.STATUS.eq(StudyParticipantStatus.APPROVED.name()))
                                                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                                                .asField("current_participants"),
                                        sa.ID.as("attached_id"),
                                        sa.NAME.as("attached_name"),
                                        sa.TYPE.as("attached_type"),
                                        sa.SIZE.as("attached_size"),
                                        sa.ATTACHED_URL
                                )
                                .from(s)
                                .leftJoin(m).on(s.MEMBER_ID.eq(m.ID))
                                .leftJoin(sa).on(s.ID.eq(sa.STUDY_ID).and(sa.DELETED_AT.isNull()))
                                .where(s.ID.eq(studyId))
                                .and(s.DELETED_AT.isNull())
                                .fetch()
                                .stream()
                                .map(record -> (Record) record)
                                .toList()
                ).filter(records -> !records.isEmpty())
                .map(records -> mapper.toStudyVoFromRecordsWithAttachments(records));

        log.info("jOOQ: Study VO found - ID: {}", studyId);
        return result;
    }

    // ================================================================
    // PRIVATE HELPER METHODS
    // ================================================================

    /**
     * ✅ jOOQ 생성 테이블로 검색 조건 구성
     */
    private Condition buildSearchConditions(StudySearchCriteriaVo criteria) {
        log.debug("jOOQ: Building search conditions with criteria: {}", criteria);

        // Use aliases to match the query structure
        var s = STUDY.as("s");
        var m = MEMBER.as("m");
        
        Condition conditions = noCondition();

        // 키워드 검색 (title, description, creatorName 포함)
        if (criteria.keyword() != null && !criteria.keyword().trim().isEmpty()) {
            String likeKeyword = "%" + criteria.keyword() + "%";
            conditions = conditions.and(
                    s.TITLE.likeIgnoreCase(likeKeyword)
                            .or(s.DESCRIPTION.likeIgnoreCase(likeKeyword))
                            .or(m.NAME.likeIgnoreCase(likeKeyword))
            );
            log.debug("jOOQ: Added keyword condition: {}", likeKeyword);
        }

        if (criteria.category() != null) {
            conditions = conditions.and(s.CATEGORY.eq(criteria.category()));
            log.debug("jOOQ: Added category condition: {}", criteria.category());
        }

        if (criteria.subCategory() != null) {
            conditions = conditions.and(s.SUBCATEGORY.eq(criteria.subCategory()));
            log.debug("jOOQ: Added subcategory condition: {}", criteria.subCategory());
        }

        // 학기 필터 (semester 기간 검색)
        if (criteria.semester() != null && !criteria.semester().trim().isEmpty()) {
            try {
                // DateTimeUtils를 사용하여 semester 기간 파싱
                var semesterPeriod = org.certis.studyplatform.shared.util.DateTimeUtils.parseSemesterPeriod(criteria.semester());

                // 스터디 기간이 학기 기간과 겹치는 경우를 검색
                conditions = conditions.and(
                        s.STARTED_AT.lessOrEqual(semesterPeriod.endDate())
                                .and(s.ENDED_AT.greaterOrEqual(semesterPeriod.startDate()))
                );
                log.debug("jOOQ: Added semester condition: {} to {}",
                        semesterPeriod.startDate(), semesterPeriod.endDate());
            } catch (IllegalArgumentException e) {
                log.warn("jOOQ: Invalid semester format: {}", criteria.semester());
                // 잘못된 형식의 경우 조건 무시
            }
        }

        // 상태 필터 (StudyStatus 기반 검색 - started_at, ended_at 기반)
        if (criteria.status() != null) {
            OffsetDateTime now = OffsetDateTime.now();
            switch (criteria.status()) {
                case READY -> {
                    // 시작 전: started_at이 현재 시간보다 미래
                    conditions = conditions.and(s.STARTED_AT.greaterThan(now));
                    log.debug("jOOQ: Added READY status condition (started_at > now)");
                }
                case INPROGRESS -> {
                    // 진행 중: started_at <= now < ended_at
                    conditions = conditions.and(s.STARTED_AT.lessOrEqual(now))
                            .and(s.ENDED_AT.greaterThan(now));
                    log.debug("jOOQ: Added INPROGRESS status condition (started_at <= now < ended_at)");
                }
                case COMPLETED -> {
                    // 완료: ended_at <= now
                    conditions = conditions.and(s.ENDED_AT.lessOrEqual(now));
                    log.debug("jOOQ: Added COMPLETED status condition (ended_at <= now)");
                }
                case REJECTED -> {
                    // 거절됨: deleted_at이 null이 아님 (삭제된 스터디)
                    conditions = conditions.and(s.DELETED_AT.isNotNull());
                    log.debug("jOOQ: Added REJECTED status condition (deleted_at is not null)");
                }
            }
        }

        log.debug("jOOQ: Final conditions built: {}", conditions);
        return conditions;
    }


    /**
     * ✅ jOOQ 생성 테이블로 정렬 조건 구성
     */
    private OrderField<?>[] buildOrderBy(Pageable pageable) {
        var s = STUDY.as("s"); // 테이블 별칭 정의
        
        if (pageable.getSort().isUnsorted()) {
            return new OrderField<?>[]{s.CREATED_AT.desc()};
        }

        return pageable.getSort().stream()
                .map(order -> {
                    Field<?> sortField; // OrderField → Field로 변경
                    switch (order.getProperty()) {
                        case "createdAt":
                            sortField = s.CREATED_AT;
                            break;
                        case "updatedAt":
                            sortField = s.UPDATED_AT;
                            break;
                        case "startedAt":
                            sortField = s.STARTED_AT;
                            break;
                        case "endedAt":
                            sortField = s.ENDED_AT;
                            break;
                        case "title":
                            sortField = s.TITLE;
                            break;
                        case "memberId":
                            sortField = s.MEMBER_ID;
                            break;
                        case "maxParticipantsNumber":
                            sortField = s.MAX_PARTICIPANTS_NUMBER;
                            break;
                        default:
                            sortField = s.CREATED_AT; // 기본값
                    }
                    return order.isAscending() ? sortField.asc() : sortField.desc();
                })
                .toArray(OrderField[]::new);
    }

    /**
     * 공통 검색 결과 생성
     */
    private StudySearchResultVo createSearchResult(List<StudySummaryVo> studySummaries,
                                                   Integer totalElements, Pageable pageable) {
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

        log.debug("jOOQ: Found {} studies", totalElements);

        return new StudySearchResultVo(
                studySummaries,
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

    @Override
    public List<StudyAttachedVo> findAttachmentsByStudyId(Long studyId) {
        log.info("jOOQ: Finding study attachments by study ID - {}", studyId);

        var sa = STUDY_ATTACHED.as("sa");

        List<StudyAttachedVo> attachments = dsl.select(
                        sa.ID,
                        sa.NAME,
                        sa.TYPE,
                        sa.SIZE,
                        sa.ATTACHED_URL
                )
                .from(sa)
                .where(sa.STUDY_ID.eq(studyId))
                .and(sa.DELETED_AT.isNull())
                .orderBy(sa.CREATED_AT.asc())
                .fetch()
                .stream()
                .map(record -> StudyAttachedVo.of(
                        record.get(sa.ID),
                        record.get(sa.NAME),
                        record.get(sa.TYPE),
                        record.get(sa.SIZE),
                        record.get(sa.ATTACHED_URL)
                ))
                .toList();

        log.info("jOOQ: Found {} attachments for study ID: {}", attachments.size(), studyId);
        return attachments;
    }

    @Override
    public List<Long> findApprovedStudiesStartedBefore(OffsetDateTime currentTime) {
        log.info("jOOQ: Finding approved studies started before {}", currentTime);

        List<Long> studyIds = dsl.select(STUDY.ID)
                .from(STUDY)
                .where(STUDY.STATUS.eq("APPROVED"))
                .and(STUDY.STARTED_AT.le(currentTime))
                .and(STUDY.DELETED_AT.isNull())
                .fetch()
                .stream()
                .map(record -> record.get(STUDY.ID))
                .toList();

        log.info("jOOQ: Found {} approved studies started before {}", studyIds.size(), currentTime);
        return studyIds;
    }

    @Override
    public Optional<org.certis.studyplatform.study.infrastructure.persistence.entity.StudyEntity> findEntityById(Long studyId) {
        log.info("jOOQ: Finding study entity by ID - {}", studyId);
        
        // JPA Repository를 통해 Entity 직접 조회
        return Optional.ofNullable(
            dsl.selectFrom(STUDY)
                .where(STUDY.ID.eq(studyId))
                .and(STUDY.DELETED_AT.isNull())
                .fetchOne()
        ).map(record -> {
            // Record를 StudyEntity로 변환
            return org.certis.studyplatform.study.infrastructure.persistence.entity.StudyEntity.builder()
                .id(record.get(STUDY.ID))
                .memberId(record.get(STUDY.MEMBER_ID))
                .title(record.get(STUDY.TITLE))
                .description(record.get(STUDY.DESCRIPTION))
                .content(record.get(STUDY.CONTENT))
                .category(record.get(STUDY.CATEGORY))
                .subcategory(record.get(STUDY.SUBCATEGORY))
                .maxParticipantsNumber(record.get(STUDY.MAX_PARTICIPANTS_NUMBER))
                .startedAt(record.get(STUDY.STARTED_AT))
                .endedAt(record.get(STUDY.ENDED_AT))
                .createdAt(record.get(STUDY.CREATED_AT))
                .updatedAt(record.get(STUDY.UPDATED_AT))
                .deletedAt(record.get(STUDY.DELETED_AT))
                .resultSubmittedAt(record.get(STUDY.RESULT_SUBMITTED_AT))
                .resultSubmitStatus(record.get(STUDY.RESULT_SUBMIT_STATUS) != null ? 
                    org.certis.studyplatform.shared.domain.ResultSubmitStatus.valueOf(record.get(STUDY.RESULT_SUBMIT_STATUS)) : null)
                .resultAttachmentUrl(record.get(STUDY.RESULT_ATTACHED_URL))
                .status(record.get(STUDY.STATUS) != null ? 
                    org.certis.studyplatform.study.domain.StudyStatus.valueOf(record.get(STUDY.STATUS)) : 
                    org.certis.studyplatform.study.domain.StudyStatus.READY)
                .build();
        });
    }
}