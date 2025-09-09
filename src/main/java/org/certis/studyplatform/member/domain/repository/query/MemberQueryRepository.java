package org.certis.studyplatform.member.domain.repository.query;

import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.application.object.query.SearchMembersQuery;
import org.certis.studyplatform.member.application.object.query.GetMembersQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Member Query Repository (Read Operations Only) - VO 기반
 *
 * CQRS Query 측면의 Repository 인터페이스 (Domain Layer) - VO 기반
 * jOOQ를 통한 최적화된 읽기 작업만 제공
 *
 * 책임:
 * - 복잡한 조회 쿼리 수행 (R Operations) - VO 기반
 * - Domain VO 반환
 * - 성능 최적화된 읽기 전용 쿼리
 * - 페이징 및 필터링 지원
 *
 * 특징:
 * - Domain Layer에 위치하며 Domain VO 반환
 * - Infrastructure Layer에서 구현
 * - Domain 객체 대신 VO들만 사용
 */
public interface MemberQueryRepository {

    /**
     * 회원 상세 정보 조회 (VO 기반)
     *
     * @param memberId 회원 ID VO
     * @return 회원 상세 정보 VO (Optional)
     */
    Optional<MemberVo> findById(MemberIdVo memberId);


    Page<MemberSummaryVo> findMembers(MemberSearchCriteriaVo searchCriteria, Pageable pageable);

    /**
     * 필터링된 회원 목록 조회 (페이징) - Legacy 호환성
     *
     * @param nameFilter 이름 필터 (Like 검색)
     * @param roleFilter 역할 필터 (정확 일치)
     * @param gradeFilter 학년 필터 (정확 일치)
     * @param skillFilter 기술 필터 (Like 검색)
     * @param pageable 페이징 정보
     * @return 페이징된 회원 요약 VO
     */
    default Page<MemberSummaryVo> findMembers(String nameFilter,
                                              String roleFilter,
                                              String gradeFilter,
                                              String skillFilter,
                                              Pageable pageable) {
        MemberSearchCriteriaVo criteria = MemberSearchCriteriaVo.builder()
                .keyword(nameFilter)
                .role(roleFilter)
                .grade(gradeFilter)
                .skill(skillFilter)
                .build();

        return findMembers(criteria, pageable);
    }

    /**
     * 회원 존재 여부 확인
     *
     * @param memberId 회원 ID
     * @return 존재 여부
     */
    boolean existsById(Long memberId);

    Optional<MemberTokenInfoVo> findTokenInfoById(Long memberId);

    List<MemberSearchForAdminVo> searchMembersForAdmin(SearchKeywordVo keywordVo);

    List<MemberWithPenaltyVo> findExpiredUpsolvers(OffsetDateTime now);

    List<MemberWithContactVo> searchMembersWithContact(MemberSearchConditionVo searchConditionVo);
}