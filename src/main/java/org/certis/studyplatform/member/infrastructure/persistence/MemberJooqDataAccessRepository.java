package org.certis.studyplatform.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.MemberRole;
import org.jooq.DSLContext;
import org.jooq.Condition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

/**
 * jOOQ 기반 Member 데이터 액세스 구현체
 * 
 * Infrastructure Layer의 실제 jOOQ 데이터 액세스 구현
 * 복잡한 Query 작업과 최적화된 읽기 작업을 담당
 * 
 * 책임:
 * - jOOQ를 통한 복잡한 쿼리 수행
 * - 성능 최적화된 읽기 전용 쿼리
 * - 페이징 및 필터링 지원
 * - 동적 쿼리 구성
 */
@Repository("memberJooqDataAccessRepository")
@RequiredArgsConstructor
@Slf4j
public class MemberJooqDataAccessRepository implements MemberDataAccessRepository {
    
    private final DSLContext dsl;
    
    // 테이블 이름을 직접 사용 (jOOQ 코드 생성 전까지)
    private static final String MEMBER_TABLE = "member";
    
    // Command Operations (기본 구현만 제공 - JPA 구현체 사용 권장)
    
    @Override
    public Member saveMember(Member member) {
        log.warn("Save operation should be handled by JPA implementation");
        throw new UnsupportedOperationException("jOOQ 구현체는 읽기 전용입니다. JPA 구현체를 사용하세요.");
    }
    
    @Override
    public void deleteMemberById(Long memberId) {
        log.warn("Delete operation should be handled by JPA implementation");
        throw new UnsupportedOperationException("jOOQ 구현체는 읽기 전용입니다. JPA 구현체를 사용하세요.");
    }
    
    @Override
    public Optional<Member> findMemberByIdForCommand(Long memberId) {
        // Command용 조회도 가능하지만 JPA 사용 권장
        return findMemberByIdForQuery(memberId);
    }
    
    @Override
    public boolean existsByStudentNumber(String studentNumber) {
        if (studentNumber == null || studentNumber.trim().isEmpty()) {
            return false;
        }
        
        try {
            return dsl.fetchExists(
                selectOne()
                    .from(table(MEMBER_TABLE))
                    .where(field("student_number").eq(studentNumber)
                           .and(field("deleted_at").isNull()))
            );
        } catch (Exception e) {
            log.error("Error checking student number existence {}: {}", studentNumber, e.getMessage());
            return false;
        }
    }
    
    // 이메일 관련 기능은 현재 MemberEntity에 email 필드가 없어서 제외
    
    @Override
    public long countByRole(MemberRole role) {
        if (role == null) {
            return 0L;
        }
        
        try {
            return dsl.selectCount()
                     .from(table(MEMBER_TABLE))
                     .where(field("role").eq(role)
                            .and(field("deleted_at").isNull()))
                     .fetchOne(0, Long.class);
        } catch (Exception e) {
            log.error("Error counting members by role {}: {}", role, e.getMessage());
            return 0L;
        }
    }
    
    @Override
    public long countByGrade(String grade) {
        if (grade == null) {
            return 0L;
        }
        
        try {
            return dsl.selectCount()
                     .from(table(MEMBER_TABLE))
                     .where(field("grade").eq(grade)
                            .and(field("deleted_at").isNull()))
                     .fetchOne(0, Long.class);
        } catch (Exception e) {
            log.error("Error counting members by grade {}: {}", grade, e.getMessage());
            return 0L;
        }
    }
    
    // Query Operations (jOOQ 최적화 구현)
    
    @Override
    public Optional<Member> findMemberByIdForQuery(Long memberId) {
        if (memberId == null) {
            return Optional.empty();
        }
        
        log.debug("Finding member by ID for query: {}", memberId);
        
        try {
            return dsl.select(
                        field("id", Long.class),
                        field("name", String.class),
                        field("student_number", String.class),
                        field("profile_image", String.class),
                        field("grade", String.class),
                        field("role", MemberRole.class),
                        field("skills", Object.class),
                        field("major", String.class),
                        field("created_at", ZonedDateTime.class),
                        field("updated_at", ZonedDateTime.class)
                    )
                    .from(table(MEMBER_TABLE))
                    .where(field("id").eq(memberId)
                           .and(field("deleted_at").isNull()))
                    .fetchOptional(record -> new Member(
                        new org.certis.studyplatform.member.domain.vo.MemberIdVo(record.value1()),
                        record.value2(),
                        new org.certis.studyplatform.member.domain.vo.StudentNumberVo(record.value3()),
                        record.value4() != null ? new org.certis.studyplatform.member.domain.vo.ProfileImageVo(record.value4()) : null,
                        record.value5(),
                        record.value6(),
                        new org.certis.studyplatform.member.domain.vo.SkillsVo(parseSkills(record.value7())),
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
    public Page<Member> findMembersByCriteria(MemberSearchCriteria searchCriteria, Pageable pageable) {
        log.debug("Finding members with criteria: {}", searchCriteria);
        
        try {
            // 동적 조건 구성
            Condition conditions = buildSearchConditions(searchCriteria);
            
            // 총 개수 조회
            int total = dsl.selectCount()
                           .from(table(MEMBER_TABLE))
                           .where(conditions)
                           .fetchOne(0, int.class);
            
//            // 페이징된 결과 조회 -> Domain Entity로 변환
//            List<Member> members = dsl.select(
//                        field("id", Long.class),
//                        field("name", String.class),
//                        field("student_number", String.class),
//                        field("profile_image", String.class),
//                        field("grade", String.class),
//                        field("role", String.class),
//                        field("skills", Object.class),
//                        field("major", String.class),
//                        field("created_at", ZonedDateTime.class),
//                        field("updated_at", ZonedDateTime.class)
//                    )
//                    .from(table(MEMBER_TABLE))
//                    .where(conditions)
//                    .orderBy(buildOrderBy(pageable))
//                    .limit(pageable.getPageSize())
//                    .offset((int) pageable.getOffset())
//                    .fetch(record -> new Member(
////                        new org.certis.studyplatform.member.domain.model.vo.MemberIdVo(record.value1()),
////                        record.value2(),
////                        new org.certis.studyplatform.member.domain.model.vo.StudentNumberVo(record.value3()),
////                        record.value4() != null ? new org.certis.studyplatform.member.domain.model.vo.ProfileImageVo(record.value4()) : null,
////                        record.value5(),
////                        record.value6(),
////                        new org.certis.studyplatform.member.domain.model.vo.SkillsVo(parseSkills(record.value7())),
////                        record.value8(),
////                        record.value9(),
////                        record.value10()
//                    ));
            
//            return new PageImpl<>(members, pageable, total);
            return new PageImpl<>(null, pageable, total);
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
        
        // 학년 필터 (정확 일치)
        String grade = searchCriteria.getSafeGrade();
        if (grade != null) {
            conditions = conditions.and(field("grade").eq(grade));
        }
        
        // 역할 필터 (정확 일치)
        MemberRole role = searchCriteria.getSafeRole();
        if (role != null) {
            conditions = conditions.and(field("role").eq(role));
        }
        
        // 특정 기술 필터 (포함 검색)
        String skill = searchCriteria.getSafeSkill();
        if (skill != null) {
            conditions = conditions.and(field("skills").cast(String.class).like("%" + skill + "%"));
        }
        
        // Soft delete 조건 추가
        conditions = conditions.and(field("deleted_at").isNull());
        
        return conditions;
    }
    
    /**
     * 페이징 정보를 jOOQ OrderBy로 변환
     */
    private org.jooq.OrderField<?>[] buildOrderBy(Pageable pageable) {
        if (pageable.getSort().isEmpty()) {
            // 기본 정렬: 생성일 역순
            return new org.jooq.OrderField[]{field("created_at").desc()};
        }
        
        return pageable.getSort().stream()
                .map(order -> {
                    org.jooq.Field<Object> field = field(order.getProperty());
                    return order.isAscending() ? field.asc() : field.desc();
                })
                .toArray(org.jooq.OrderField[]::new);
    }
    
    /**
     * Skills 파싱 유틸리티
     */
    private List<String> parseSkills(Object skills) {
        if (skills == null) {
            return List.of();
        }
        
        // PostgreSQL array 처리
        String skillsString = skills.toString();
        if (skillsString.startsWith("{") && skillsString.endsWith("}")) {
            skillsString = skillsString.substring(1, skillsString.length() - 1);
            return Arrays.asList(skillsString.split(","));
        }
        
        return List.of(skillsString);
    }
} 