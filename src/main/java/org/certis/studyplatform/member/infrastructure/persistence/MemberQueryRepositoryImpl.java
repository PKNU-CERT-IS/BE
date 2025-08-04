package org.certis.studyplatform.member.infrastructure.persistence;

import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.domain.repository.MemberQueryRepository;
import org.certis.studyplatform.member.domain.repository.MemberQueryRepository.MemberSearchCriteria;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Member Query Repository 구현체 (Infrastructure Layer)
 * 
 * MemberQueryRepository 인터페이스의 jOOQ 기반 구현체
 * Query 패턴에 따른 읽기 작업 전용
 * 
 * 책임:
 * - jOOQ를 통한 Query 작업 (R Operations)
 * - 복잡한 조회 쿼리 및 성능 최적화
 * - 페이징 및 필터링 지원
 * - 동적 쿼리 구성 및 집계 함수 처리
 * 
 * CQRS 패턴:
 * - Query 전용: jOOQ 사용으로 복잡한 쿼리와 성능에 최적화
 */
@Repository
@Primary
@Slf4j
public class MemberQueryRepositoryImpl implements MemberQueryRepository {
    
    private final MemberDataAccessRepository jooqDataAccess;
    
    public MemberQueryRepositoryImpl(@Qualifier("memberJooqDataAccessRepository") MemberDataAccessRepository jooqDataAccess) {
        this.jooqDataAccess = jooqDataAccess;
    }
    
    @Override
    public Optional<Member> findMemberById(Long memberId) {
        log.debug("Query Infrastructure: Finding member by ID through jOOQ: {}", memberId);
        return jooqDataAccess.findMemberByIdForQuery(memberId);
    }
    
    @Override
    public Page<Member> findMembers(MemberSearchCriteria searchCriteria, Pageable pageable) {
        log.debug("Query Infrastructure: Finding members through jOOQ with criteria: {}", searchCriteria);
        
        // Domain SearchCriteria를 Infrastructure DataAccess용으로 변환
        MemberDataAccessRepository.MemberSearchCriteria dataAccessCriteria = 
            convertToDataAccessCriteria(searchCriteria);
        
        return jooqDataAccess.findMembersByCriteria(dataAccessCriteria, pageable);
    }
    
    /**
     * Domain SearchCriteria를 Infrastructure DataAccess용으로 변환
     */
    private MemberDataAccessRepository.MemberSearchCriteria convertToDataAccessCriteria(
            MemberSearchCriteria domainCriteria) {
        
        return new MemberDataAccessRepository.MemberSearchCriteria(
            domainCriteria.keyword(),
            domainCriteria.grade(),
            domainCriteria.role(),
            domainCriteria.skill()
        );
    }
} 