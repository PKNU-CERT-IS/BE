package org.certis.studyplatform.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.exception.InfrastructureException;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.repository.query.MemberQueryRepository;
import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.infrastructure.mapper.MemberInfrastructureMapper;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.certis.studyplatform.member.application.object.query.SearchMembersQuery;
import org.certis.studyplatform.member.application.object.query.GetMembersQuery;
import org.jooq.*;
import org.jooq.Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

/**
 * Member Query Repository 구현체 (Infrastructure Layer) - VO 기반
 *
 * jOOQ 기반 Query 작업 전용 구현체 (VO 기반)
 *
 * 책임:
 * - jOOQ를 통한 Query 작업 (R Operations) - VO 기반
 * - Raw Data → MemberEntity → VO 변환 (MemberInfrastructureMapper 사용)
 * - 복잡한 조회 쿼리 및 성능 최적화
 * - 페이징 및 동적 쿼리 구성
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class MemberQueryRepositoryImpl implements MemberQueryRepository {

    private final DSLContext dsl;
    private final MemberInfrastructureMapper memberInfrastructureMapper;

    // 테이블 이름을 직접 사용 (jOOQ 코드 생성 전까지)
    private static final String MEMBER_TABLE = "member";

    @Override
    public Optional<MemberVo> findById(MemberIdVo memberIdVo) {
        if (memberIdVo == null) {
            return Optional.empty();
        }

        log.info("Query Infrastructure: Finding member by ID: {}", memberIdVo.value());

        try {
            return dsl.select(
                            field("id", Long.class),
                            field("name", String.class),
                            field("student_number", String.class),
                            field("profile_image", String.class),
                            field("grade", String.class),
                            field("role", String.class),
                            field("skills", String[].class), // String[] 배열로 처리
                            field("major", String.class),
                            field("description", String.class),
                            field("birthday", OffsetDateTime.class),
                            field("gender", String.class),
                            field("created_at", OffsetDateTime.class),
                            field("updated_at", OffsetDateTime.class)
                    )
                    .from(table(MEMBER_TABLE))
                    .where(field("id").eq(memberIdVo.value())
                            .and(field("deleted_at").isNull()))
                    .fetchOptional(record -> {
                        // Raw Data → MemberEntity → VO 변환
                        MemberEntity entity = recordToMemberEntity(record);
                        return memberInfrastructureMapper.toMemberVo(entity);
                    });
        } catch (Exception e) {
            log.error("Error finding member by ID {}: {}", memberIdVo.value(), e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Page<MemberSummaryVo> searchMembers(SearchMembersQuery query) {
        log.info("Query Infrastructure: Searching members with criteria: {}", query.keyword());

        try {
            // 동적 조건 구성
            Condition conditions = buildSearchConditions(query);

            // 총 개수 조회
            int total = dsl.selectCount()
                    .from(table(MEMBER_TABLE))
                    .where(conditions.and(field("deleted_at").isNull()))
                    .fetchOneInto(Integer.class);

            // 페이징된 데이터 조회
            List<MemberSummaryVo> members = dsl.select(
                            field("id", Long.class),
                            field("name", String.class),
                            field("student_number", String.class),
                            field("profile_image", String.class),
                            field("grade", String.class),
                            field("role", String.class),
                            field("skills", String[].class), // String[] 배열로 처리
                            field("major", String.class),
                            field("description", String.class),
                            field("birthday", OffsetDateTime.class),
                            field("gender", String.class),
                            field("created_at", OffsetDateTime.class)
                    )
                    .from(table(MEMBER_TABLE))
                    .where(conditions.and(field("deleted_at").isNull()))
                    .orderBy(buildOrderBy(query.pageable()))
                    .limit(query.pageable().getPageSize())
                    .offset(query.pageable().getOffset())
                    .fetch(record -> {
                        // Raw Data → MemberEntity → VO 변환
                        MemberEntity entity = recordToMemberEntity(record);
                        return memberInfrastructureMapper.toMemberSummaryVo(entity);
                    });

            return new PageImpl<>(members, query.pageable(), total);

        } catch (Exception e) {
            log.error("Error searching members: {}", e.getMessage());
            return Page.empty(query.pageable());
        }
    }

    @Override
    public Page<MemberSummaryVo> findAll(GetMembersQuery query) {
        log.info("Query Infrastructure: Finding all members with pagination");

        try {
            // 총 개수 조회
            int total = dsl.selectCount()
                    .from(table(MEMBER_TABLE))
                    .where(field("deleted_at").isNull())
                    .fetchOneInto(Integer.class);

            // 페이징된 데이터 조회
            List<MemberSummaryVo> members = dsl.select(
                            field("id", Long.class),
                            field("name", String.class),
                            field("student_number", String.class),
                            field("profile_image", String.class),
                            field("grade", String.class),
                            field("role", String.class),
                            field("skills", String[].class), // String[] 배열로 처리
                            field("major", String.class),
                            field("description", String.class),
                            field("birthday", OffsetDateTime.class),
                            field("gender", String.class),
                            field("created_at", OffsetDateTime.class)
                    )
                    .from(table(MEMBER_TABLE))
                    .where(field("deleted_at").isNull())
                    .orderBy(buildOrderBy(query.pageable()))
                    .limit(query.pageable().getPageSize())
                    .offset(query.pageable().getOffset())
                    .fetch(record -> {
                        // Raw Data → MemberEntity → VO 변환
                        MemberEntity entity = recordToMemberEntity(record);
                        return memberInfrastructureMapper.toMemberSummaryVo(entity);
                    });

            return new PageImpl<>(members, query.pageable(), total);

        } catch (Exception e) {
            log.error("Error finding all members: {}", e.getMessage());
            return Page.empty(query.pageable());
        }
    }

    @Override
    public Page<MemberSummaryVo> findMembers(MemberSearchCriteriaVo searchCriteria, Pageable pageable) {
        log.info("Query Infrastructure: Finding members with criteria VO");

        try {
            // 동적 조건 구성
            Condition conditions = buildSearchConditionsFromCriteria(searchCriteria);

            // 총 개수 조회
            int total = dsl.selectCount()
                    .from(table(MEMBER_TABLE))
                    .where(conditions.and(field("deleted_at").isNull()))
                    .fetchOneInto(Integer.class);

            // 페이징된 데이터 조회
            List<MemberSummaryVo> members = dsl.select(
                            field("id", Long.class),
                            field("name", String.class),
                            field("student_number", String.class),
                            field("profile_image", String.class),
                            field("grade", String.class),
                            field("role", String.class),
                            field("skills", String[].class), // String[] 배열로 처리
                            field("major", String.class),
                            field("description", String.class),
                            field("birthday", OffsetDateTime.class),
                            field("gender", String.class),
                            field("created_at", OffsetDateTime.class)
                    )
                    .from(table(MEMBER_TABLE))
                    .where(conditions.and(field("deleted_at").isNull()))
                    .orderBy(buildOrderBy(pageable))
                    .limit(pageable.getPageSize())
                    .offset(pageable.getOffset())
                    .fetch(record -> {
                        // Raw Data → MemberEntity → VO 변환
                        MemberEntity entity = recordToMemberEntity(record);
                        return memberInfrastructureMapper.toMemberSummaryVo(entity);
                    });

            return new PageImpl<>(members, pageable, total);

        } catch (Exception e) {
            log.error("Error finding members with criteria: {}", e.getMessage());
            return Page.empty(pageable);
        }
    }

    // ================================================================
    // PRIVATE HELPER METHODS
    // ================================================================

    /**
     * jOOQ Record를 MemberEntity로 변환
     * MemberEntity.builder()를 사용하여 변환
     */
    private MemberEntity recordToMemberEntity(Record record) {
        return MemberEntity.builder()
                .id(record.get("id", Long.class))
                .name(record.get("name", String.class))
                .studentNumber(record.get("student_number", String.class))
                .profileImage(record.get("profile_image", String.class))
                .grade(record.get("grade", MemberGrade.class))
                .role(record.get("role", MemberRole.class))
                .skills(record.get("skills", String[].class)) // String[] 배열
                .major(record.get("major", String.class))
                .description(record.get("description", String.class))
                .birthday(record.get("birthday", OffsetDateTime.class))
                .gender(record.get("gender", String.class))
                .createdAt(record.get("created_at", OffsetDateTime.class))
                .updatedAt(record.get("updated_at", OffsetDateTime.class))
                .build();
    }

    private Condition buildSearchConditions(SearchMembersQuery query) {
        Condition conditions = noCondition();

        if (query.keyword() != null && !query.keyword().trim().isEmpty()) {
            String keyword = "%" + query.keyword().trim() + "%";
            conditions = conditions.and(
                    field("name").likeIgnoreCase(keyword)
                            .or(field("student_number").likeIgnoreCase(keyword))
                            .or(field("major").likeIgnoreCase(keyword))
            );
        }

        if (query.grade() != null && !query.grade().trim().isEmpty()) {
            conditions = conditions.and(field("grade").eq(query.grade()));
        }

        if (query.role() != null) {
            conditions = conditions.and(field("role").eq(query.role()));
        }

        // skills 검색 - PostgreSQL array 함수 사용
        if (query.skills() != null && !query.skills().isEmpty()) {
            String skillKeyword = "%" + String.join("%", query.skills()) + "%";
            conditions = conditions.and(
                    field("array_to_string(skills, ',')")
                            .likeIgnoreCase(skillKeyword)
            );
        }

        return conditions;
    }

    private Condition buildSearchConditionsFromCriteria(MemberSearchCriteriaVo searchCriteria) {
        Condition conditions = noCondition();

        if (searchCriteria.getSafeKeyword() != null) {
            String keyword = "%" + searchCriteria.getSafeKeyword() + "%";
            conditions = conditions.and(
                    field("name").likeIgnoreCase(keyword)
                            .or(field("student_number").likeIgnoreCase(keyword))
                            .or(field("major").likeIgnoreCase(keyword))
            );
        }

        if (searchCriteria.getSafeGrade() != null) {
            conditions = conditions.and(field("grade").eq(searchCriteria.getSafeGrade()));
        }

        if (searchCriteria.getSafeRole() != null) {
            conditions = conditions.and(field("role").eq(searchCriteria.getSafeRole()));
        }

        if (searchCriteria.getSafeSkill() != null) {
            String skill = "%" + searchCriteria.getSafeSkill() + "%";
            // PostgreSQL array 검색
            conditions = conditions.and(
                    field("array_to_string(skills, ',')")
                            .likeIgnoreCase(skill)
            );
        }

        return conditions;
    }

    private org.jooq.OrderField<?>[] buildOrderBy(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return new org.jooq.OrderField<?>[]{field("created_at").desc()};
        }

        return pageable.getSort().stream()
                .map(order -> {
                    org.jooq.Field<?> field = field(order.getProperty());
                    return order.isAscending() ? field.asc() : field.desc();
                })
                .toArray(org.jooq.OrderField[]::new);
    }

    // ================================================================
    // LEGACY METHODS (하위 호환성 유지)
    // ================================================================

    @Override
    public Optional<MemberVo> findMemberById(Long memberId) {
        return findById(new MemberIdVo(memberId));
    }

    @Override
    public boolean existsById(Long memberId) {
        return findById(new MemberIdVo(memberId)).isPresent();
    }

    @Override
    public Optional<MemberTokenInfoVo> findTokenInfoById(Long memberId) {
        log.debug("Infrastructure: JWT 토큰용 회원 정보 조회 시작: memberId={}", memberId);

        if (memberId == null || memberId <= 0) {
            log.debug("Infrastructure: 유효하지 않은 memberId: {}", memberId);
            return Optional.empty();
        }

        try {
            // jOOQ를 사용한 JOIN 쿼리
            Record result = dsl.select(
                            field("m.id", Long.class),
                            field("m.name", String.class),
                            field("m.student_number", String.class),
                            field("m.role", String.class),
                            field("mc.email", String.class)
                    )
                    .from(table("member").as("m"))
                    .leftJoin(table("member_contact").as("mc"))
                    .on(field("m.id").eq(field("mc.member_id")))
                    .where(field("m.id").eq(memberId))
                    .and(field("m.deleted_at").isNull()) // 삭제되지 않은 회원만
                    .fetchOne();

            if (result == null) {
                log.debug("Infrastructure: 회원 정보를 찾을 수 없음: memberId={}", memberId);
                return Optional.empty();
            }

            // VO 변환
            MemberTokenInfoVo tokenInfo = MemberTokenInfoVo.of(
                    result.get("m.id", Long.class),
                    result.get("m.name", String.class),
                    result.get("m.student_number", String.class),
                    result.get("mc.email", String.class),
                    MemberRole.valueOf(result.get("m.role", String.class))
            );

            log.debug("Infrastructure: JWT 토큰용 회원 정보 조회 성공: memberId={}, hasEmail={}",
                    memberId, tokenInfo.hasEmail());

            return Optional.of(tokenInfo);

        } catch (Exception e) {
            log.error("Infrastructure: JWT 토큰용 회원 정보 조회 중 오류 발생: memberId={}, error={}",
                    memberId, e.getMessage(), e);
            throw new InfrastructureException(ExceptionStatus.MEMBER_INFRASTRUCTURE_NOT_FOUND);
        }
    }

    @Override
    public List<MemberSearchForAdminVo> searchMembersForAdmin(SearchKeywordVo keywordVo) {
        log.info("Query Infrastructure: Admin member search keyword={}",
                keywordVo != null ? keywordVo.value() : "ALL");

        // 1. 기본 조건: 삭제되지 않은 회원
        Condition condition = field("m.deleted_at").isNull();

        if (keywordVo != null && keywordVo.value() != null && !keywordVo.value().isBlank()) {
            String keyword = "%" + keywordVo.value().trim() + "%";
            condition = condition.and(
                    field("m.name").likeIgnoreCase(keyword)
                            .or(field("m.student_number").likeIgnoreCase(keyword))
                            .or(field("m.major").likeIgnoreCase(keyword))
            );
        }

        // 2. 기본 회원 정보 조회
        Result<Record12<Long, String, String, String, String, String, OffsetDateTime, OffsetDateTime, OffsetDateTime, String, String, Long>>
                memberRecords = dsl.select(
                        field("m.id", Long.class).as("id"),
                        field("m.name", String.class).as("name"),
                        field("m.role", String.class).as("role"),
                        field("m.major", String.class).as("major"),
                        field("m.student_number", String.class).as("student_number"),
                        field("m.grade", String.class).as("grade"),
                        field("m.birthday", OffsetDateTime.class).as("birthday"),
                        field("m.grace_period", OffsetDateTime.class).as("grace_period"),
                        field("m.created_at", OffsetDateTime.class).as("created_at"),
                        field("mc.phone_number", String.class).as("phone_number"),
                        field("mc.email", String.class).as("email"),
                        field("mp.penalty_point", Long.class).as("penalty_point")
                )
                .from(table("member").as("m"))
                .leftJoin(table("member_contact").as("mc")).on(field("mc.member_id").eq(field("m.id")))
                .leftJoin(table("member_penalty").as("mp")).on(field("mp.member_id").eq(field("m.id")))
                .where(condition)
                .fetch();

        if (memberRecords.isEmpty()) {
            return List.of();
        }

        // memberId 목록 추출
        List<Long> memberIds = memberRecords.map(r -> r.get("id", Long.class));

        // 3. 스터디 활동 조회
        Map<Long, List<String>> studyMap = dsl.select(
                        field("sp.member_id", Long.class).as("member_id"),
                        field("s.title", String.class).as("title")
                )
                .from(table("study_participant").as("sp"))
                .join(table("study").as("s")).on(field("s.id").eq(field("sp.study_id")))
                .where(field("sp.member_id").in(memberIds))
                .and(field("s.deleted_at").isNull())
                .and(field("s.started_at").le(OffsetDateTime.now()))
                .and(field("s.ended_at").gt(OffsetDateTime.now()))
                .fetchGroups(
                        r -> r.get("member_id", Long.class),
                        r -> r.get("title", String.class)
                );

        // 4. 프로젝트 활동 조회
        Map<Long, List<String>> projectMap = dsl.select(
                        field("pp.member_id", Long.class).as("member_id"),
                        field("p.title", String.class).as("title")
                )
                .from(table("project_participant").as("pp"))
                .join(table("project").as("p")).on(field("p.id").eq(field("pp.project_id")))
                .where(field("pp.member_id").in(memberIds))
                .and(field("p.deleted_at").isNull())
                .and(field("p.started_at").le(OffsetDateTime.now()))
                .and(field("p.ended_at").gt(OffsetDateTime.now()))
                .fetchGroups(
                        r -> r.get("member_id", Long.class),
                        r -> r.get("title", String.class)
                );

        // 5. 최종 매핑
        return memberRecords.stream()
                .map(r -> {
                    Long memberId = r.get("id", Long.class);
                    return new MemberSearchForAdminVo(
                            new MemberIdVo(memberId),
                            r.get("name", String.class),
                            MemberRole.valueOf(r.get("role", String.class)),
                            r.get("major", String.class),
                            r.get("student_number", String.class),
                            studyMap.getOrDefault(memberId, List.of()),
                            projectMap.getOrDefault(memberId, List.of()),
                            r.get("penalty_point", Long.class),
                            r.get("grace_period", OffsetDateTime.class),
                            r.get("grade", String.class),
                            r.get("birthday", OffsetDateTime.class),
                            r.get("phone_number", String.class),
                            r.get("email", String.class),
                            r.get("created_at", OffsetDateTime.class)
                    );
                })
                .toList();
    }

    @Override
    public List<MemberWithPenaltyVo> findExpiredUpsolvers(OffsetDateTime now) {
        return dsl.select(
                        field("m.id", Long.class).as("id"),
                        field("m.role", String.class).as("role"),
                        field("m.grace_period", OffsetDateTime.class).as("grace_period"),
                        field("mp.penalty_point", Long.class).as("penalty_point")
                )
                .from(table("member").as("m"))
                .leftJoin(table("member_penalty").as("mp")).on(field("mp.member_id").eq(field("m.id")))
                .where(field("m.role").eq("UPSOLVER"))
                .and(field("m.grace_period").le(now))
                .and(field("m.deleted_at").isNull())
                .fetch(record -> new MemberWithPenaltyVo(
                        new MemberIdVo(record.get("id", Long.class)),
                        MemberRole.valueOf(record.get("role", String.class)),
                        record.get("grace_period", OffsetDateTime.class),
                        record.get("penalty_point", Long.class)
                ));
    }



}