package org.certis.studyplatform.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.repository.query.MemberQueryRepository;
import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.infrastructure.mapper.MemberInfrastructureMapper;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.certis.studyplatform.member.application.object.query.SearchMembersQuery;
import org.certis.studyplatform.member.application.object.query.GetMembersQuery;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
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
                            field("birthday", ZonedDateTime.class),
                            field("gender", String.class),
                            field("created_at", ZonedDateTime.class),
                            field("updated_at", ZonedDateTime.class)
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
                            field("birthday", ZonedDateTime.class),
                            field("gender", String.class),
                            field("created_at", ZonedDateTime.class)
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
                            field("birthday", ZonedDateTime.class),
                            field("gender", String.class),
                            field("created_at", ZonedDateTime.class)
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
                            field("birthday", ZonedDateTime.class),
                            field("gender", String.class),
                            field("created_at", ZonedDateTime.class)
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
                .grade(record.get("grade", String.class))
                .role(record.get("role", String.class))
                .skills(record.get("skills", String[].class)) // String[] 배열
                .major(record.get("major", String.class))
                .description(record.get("description", String.class))
                .birthday(record.get("birthday", ZonedDateTime.class))
                .gender(record.get("gender", String.class))
                .createdAt(record.get("created_at", ZonedDateTime.class))
                .updatedAt(record.get("updated_at", ZonedDateTime.class))
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

        if (query.role() != null && !query.role().trim().isEmpty()) {
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
}