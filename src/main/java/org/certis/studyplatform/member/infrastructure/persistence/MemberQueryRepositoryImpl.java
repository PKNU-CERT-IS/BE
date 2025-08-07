package org.certis.studyplatform.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.repository.query.MemberQueryRepository;
import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.infrastructure.mapper.MemberMapper;
import org.certis.studyplatform.member.application.object.query.SearchMembersQuery;
import org.certis.studyplatform.member.application.object.query.GetMembersQuery;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

/**
 * Member Query Repository 구현체 (Infrastructure Layer)
 *
 * jOOQ 기반 Query 작업 전용 구현체
 *
 * 책임:
 * - jOOQ를 통한 Query 작업 (R Operations)
 * - Domain ↔ Raw Data 변환 (Mapper 사용)
 * - 복잡한 조회 쿼리 및 성능 최적화
 * - 페이징 및 동적 쿼리 구성
 * - 통합 매퍼 시스템 사용: MemberMapper
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class MemberQueryRepositoryImpl implements MemberQueryRepository {

    private final DSLContext dsl;
    private final MemberMapper memberMapper;

    // 테이블 이름을 직접 사용 (jOOQ 코드 생성 전까지)
    private static final String MEMBER_TABLE = "member";

    @Override
    public Optional<Member> findById(MemberIdVo memberIdVo) {
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
                            field("skills", Object.class),
                            field("major", String.class),
                            field("description", String.class),
                            field("created_at", ZonedDateTime.class),
                            field("updated_at", ZonedDateTime.class)
                    )
                    .from(table(MEMBER_TABLE))
                    .where(field("id").eq(memberIdVo.value())
                            .and(field("deleted_at").isNull()))
                    .fetchOptional(record -> {
                        // 통합 매퍼 사용: Raw Data → Domain 변환
                        return memberMapper.toDomain(
                            record.get("id", Long.class),
                            record.get("name", String.class),
                            record.get("student_number", String.class),
                            record.get("profile_image", String.class),
                            record.get("grade", String.class),
                            record.get("role", String.class),
                            record.get("skills", Object.class),
                            record.get("major", String.class),
                            record.get("description", String.class),
                            record.get("created_at", ZonedDateTime.class),
                            record.get("updated_at", ZonedDateTime.class)
                        );
                    });
        } catch (Exception e) {
            log.error("Error finding member by ID {}: {}", memberIdVo.value(), e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Page<Member> searchMembers(SearchMembersQuery query) {
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
            List<Member> members = dsl.select(
                            field("id", Long.class),
                            field("name", String.class),
                            field("student_number", String.class),
                            field("profile_image", String.class),
                            field("grade", String.class),
                            field("role", String.class),
                            field("skills", Object.class),
                            field("major", String.class),
                            field("description", String.class),
                            field("created_at", ZonedDateTime.class),
                            field("updated_at", ZonedDateTime.class)
                    )
                    .from(table(MEMBER_TABLE))
                    .where(conditions.and(field("deleted_at").isNull()))
                    .orderBy(buildOrderBy(query.pageable()))
                    .limit(query.pageable().getPageSize())
                    .offset(query.pageable().getOffset())
                    .fetch(record -> {
                        // 통합 매퍼 사용: Raw Data → Domain 변환
                        return memberMapper.toDomain(
                            record.get("id", Long.class),
                            record.get("name", String.class),
                            record.get("student_number", String.class),
                            record.get("profile_image", String.class),
                            record.get("grade", String.class),
                            record.get("role", String.class),
                            record.get("skills", Object.class),
                            record.get("major", String.class),
                            record.get("description", String.class),
                            record.get("created_at", ZonedDateTime.class),
                            record.get("updated_at", ZonedDateTime.class)
                        );
                    });

            return new PageImpl<>(members, query.pageable(), total);

        } catch (Exception e) {
            log.error("Error searching members: {}", e.getMessage());
            return Page.empty(query.pageable());
        }
    }

    @Override
    public Page<Member> findAll(GetMembersQuery query) {
        log.info("Query Infrastructure: Finding all members with pagination");

        try {
            // 총 개수 조회
            int total = dsl.selectCount()
                    .from(table(MEMBER_TABLE))
                    .where(field("deleted_at").isNull())
                    .fetchOneInto(Integer.class);

            // 페이징된 데이터 조회
            List<Member> members = dsl.select(
                            field("id", Long.class),
                            field("name", String.class),
                            field("student_number", String.class),
                            field("profile_image", String.class),
                            field("grade", String.class),
                            field("role", String.class),
                            field("skills", Object.class),
                            field("major", String.class),
                            field("description", String.class),
                            field("created_at", ZonedDateTime.class),
                            field("updated_at", ZonedDateTime.class)
                    )
                    .from(table(MEMBER_TABLE))
                    .where(field("deleted_at").isNull())
                    .orderBy(buildOrderBy(query.pageable()))
                    .limit(query.pageable().getPageSize())
                    .offset(query.pageable().getOffset())
                    .fetch(record -> {
                        // 통합 매퍼 사용: Raw Data → Domain 변환
                        return memberMapper.toDomain(
                            record.get("id", Long.class),
                            record.get("name", String.class),
                            record.get("student_number", String.class),
                            record.get("profile_image", String.class),
                            record.get("grade", String.class),
                            record.get("role", String.class),
                            record.get("skills", Object.class),
                            record.get("major", String.class),
                            record.get("description", String.class),
                            record.get("created_at", ZonedDateTime.class),
                            record.get("updated_at", ZonedDateTime.class)
                        );
                    });

            return new PageImpl<>(members, query.pageable(), total);

        } catch (Exception e) {
            log.error("Error finding all members: {}", e.getMessage());
            return Page.empty(query.pageable());
        }
    }

    // ================================================================
    // PRIVATE HELPER METHODS
    // ================================================================

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
    public Optional<Member> findMemberById(Long memberId) {
        return findById(new MemberIdVo(memberId));
    }

    @Override
    public boolean existsById(Long memberId) {
        return findById(new MemberIdVo(memberId)).isPresent();
    }

    @Override
    public Page<Member> findMembers(MemberSearchCriteria searchCriteria, Pageable pageable) {
        // TODO: MemberSearchCriteria를 SearchMembersQuery로 변환하는 로직 필요
        // 임시로 기본 검색 쿼리 사용
        SearchMembersQuery query = new SearchMembersQuery(
            searchCriteria.keyword(),
            searchCriteria.grade(),
            searchCriteria.role(),
            pageable
        );
        return searchMembers(query);
    }
}