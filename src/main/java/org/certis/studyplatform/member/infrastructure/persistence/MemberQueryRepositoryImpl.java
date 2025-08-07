package org.certis.studyplatform.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.repository.MemberQueryRepository;
import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.infrastructure.mapper.MemberMapper;
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
    public Optional<Member> findById(Long memberId) {
        if (memberId == null) {
            return Optional.empty();
        }
        
        log.debug("Query Infrastructure: Finding member by ID: {}", memberId);
        
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
                        field("created_at", ZonedDateTime.class),
                        field("updated_at", ZonedDateTime.class)
                    )
                    .from(table(MEMBER_TABLE))
                    .where(field("id").eq(memberId)
                           .and(field("deleted_at").isNull()))
                    .fetchOptional(record -> new Member(
                        new MemberIdVo(record.value1()),
                        record.value2(),
                        new StudentNumberVo(record.value3()),
                        record.value4() != null ? new ProfileImageVo(record.value4()) : null,
                        record.value5(),
                        record.value6(),
                        new SkillsVo(memberMapper.parseSkillsFromDatabase(record.value7())),
                        record.value8(),
                        record.value9(),
                        record.value10()
                    ));
        } catch (Exception e) {
            log.error("Error finding member by ID {}: {}", memberId, e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public Page<Member> findMembers(MemberSearchCriteria searchCriteria, Pageable pageable) {
        log.debug("Query Infrastructure: Finding members with criteria: {}", searchCriteria);
        
        try {
            // 동적 조건 구성
            Condition conditions = buildSearchConditions(searchCriteria);
            
            // 총 개수 조회
            int total = dsl.selectCount()
                           .from(table(MEMBER_TABLE))
                           .where(conditions)
                           .fetchOne(0, int.class);
            
            // 페이징된 결과 조회
            List<Member> members = dsl.select(
                        field("id", Long.class),
                        field("name", String.class),
                        field("student_number", String.class),
                        field("profile_image", String.class),
                        field("grade", String.class),
                        field("role", String.class),
                        field("skills", Object.class),
                        field("major", String.class),
                        field("created_at", ZonedDateTime.class),
                        field("updated_at", ZonedDateTime.class)
                    )
                    .from(table(MEMBER_TABLE))
                    .where(conditions)
                    .orderBy(buildOrderBy(pageable))
                    .limit(pageable.getPageSize())
                    .offset((int) pageable.getOffset())
                    .fetch(record -> new Member(
                        new MemberIdVo(record.value1()),
                        record.value2(),
                        new StudentNumberVo(record.value3()),
                        record.value4() != null ? new ProfileImageVo(record.value4()) : null,
                        record.value5(),
                        record.value6(),
                        new SkillsVo(memberMapper.parseSkillsFromDatabase(record.value7())),
                        record.value8(),
                        record.value9(),
                        record.value10()
                    ));
            
            return new PageImpl<>(members, pageable, total);
        } catch (Exception e) {
            log.error("Error finding members with criteria: {}", e.getMessage());
            return new PageImpl<>(List.of(), pageable, 0);
        }
    }
    
    /**
     * 검색 조건을 jOOQ Condition으로 변환
     */
    private Condition buildSearchConditions(MemberSearchCriteria searchCriteria) {
        Condition conditions = trueCondition();
        
        // 키워드 검색 (이름, 전공, 기술 스택에서 검색)
        String keyword = searchCriteria.getSafeKeyword();
        if (keyword != null) {
            Condition keywordCondition = field("name").like("%" + keyword + "%")
                    .or(field("major").like("%" + keyword + "%"))
                    .or(field("skills").cast(String.class).like("%" + keyword + "%"));
            conditions = conditions.and(keywordCondition);
        }
        
        // 학년 필터
        String grade = searchCriteria.getSafeGrade();
        if (grade != null) {
            conditions = conditions.and(field("grade").eq(grade));
        }
        
        // 역할 필터
        String role = searchCriteria.getSafeRole();
        if (role != null) {
            conditions = conditions.and(field("role").eq(role));
        }
        
        // 특정 기술 필터
        String skill = searchCriteria.getSafeSkill();
        if (skill != null) {
            conditions = conditions.and(field("skills").cast(String.class).like("%" + skill + "%"));
        }
        
        // Soft delete 조건
        conditions = conditions.and(field("deleted_at").isNull());
        
        return conditions;
    }
    
    /**
     * 페이징 정보를 jOOQ OrderBy로 변환
     */
    private org.jooq.OrderField<?>[] buildOrderBy(Pageable pageable) {
        if (pageable.getSort().isEmpty()) {
            return new org.jooq.OrderField[]{field("created_at").desc()};
        }
        
        return pageable.getSort().stream()
                .map(order -> {
                    org.jooq.Field<Object> field = field(order.getProperty());
                    return order.isAscending() ? field.asc() : field.desc();
                })
                .toArray(org.jooq.OrderField[]::new);
    }

    @Override
    public Optional<Member> findMemberById(Long memberId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findMemberById'");
    }
}