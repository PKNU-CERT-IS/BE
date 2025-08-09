package org.certis.studyplatform.member.domain.repository.query;

import org.certis.studyplatform.member.domain.Profile;
import org.certis.studyplatform.member.domain.vo.*;

import java.util.List;
import java.util.Optional;

/**
 * Profile Query Repository Interface
 *
 * Clean Architecture Domain Layer의 Repository 인터페이스
 * 프로필 도메인의 읽기 작업(Query)을 담당
 *
 * 특징:
 * - Domain Layer에 위치 (Infrastructure를 모름)
 * - 순수 도메인 객체와 DTO 사용
 * - Infrastructure Layer에서 구현
 * - jOOQ 등 읽기 최적화 기술로 구현 예정
 * - 복잡한 조인 쿼리 최적화 가능
 */
public interface ProfileQueryRepository {

    /**
     * 회원 ID로 프로필 조회
     *
     * @param memberIdVo 회원 ID
     * @return 프로필 도메인 객체 Optional
     */
    Optional<ProfileVo> findByMemberId(MemberIdVo memberIdVo);

    /**
     * 회원이 관여한 스터디 목록 조회
     * 스터디 참여자 테이블과 조인하여 정보 조회
     *
     * @param memberIdVo 회원 ID
     * @return 스터디 VO 목록
     */
    List<ProfileStudyVo> findStudiesByMemberId(MemberIdVo memberIdVo);

    /**
     * 회원이 관여한 프로젝트 목록 조회
     * 프로젝트 참여자 테이블과 조인하여 정보 조회
     *
     * @param memberIdVo 회원 ID
     * @return 프로젝트 VO 목록
     */
    List<ProfileProjectVo> findProjectsByMemberId(MemberIdVo memberIdVo);

    /**
     * 회원이 작성한 블로그 목록 조회
     * 블로그 테이블에서 작성자 ID로 조회
     *
     * @param memberIdVo 회원 ID
     * @return 블로그 VO 목록
     */
    List<ProfileBlogVo> findBlogsByMemberId(MemberIdVo memberIdVo);

    /**
     * 프로필 존재 여부 확인
     *
     * @param memberIdVo 회원 ID
     * @return 존재 여부
     */
    boolean existsByMemberId(MemberIdVo memberIdVo);

    /**
     * 회원의 스터디 참여 개수 조회
     *
     * @param memberIdVo 회원 ID
     * @return 참여 중인 스터디 개수
     */
    long countStudiesByMemberId(MemberIdVo memberIdVo);

    /**
     * 회원의 프로젝트 참여 개수 조회
     *
     * @param memberIdVo 회원 ID
     * @return 참여 중인 프로젝트 개수
     */
    long countProjectsByMemberId(MemberIdVo memberIdVo);

    /**
     * 회원의 블로그 작성 개수 조회
     *
     * @param memberIdVo 회원 ID
     * @return 작성한 블로그 개수
     */
    long countBlogsByMemberId(MemberIdVo memberIdVo);

    /**
     * 회원의 최근 활동 요약 조회
     * 최근 30일 내 스터디/프로젝트/블로그 활동 개수
     *
     * @param memberIdVo 회원 ID
     * @return 활동 요약 정보
     */
    ActivitySummaryVo getRecentActivitySummary(MemberIdVo memberIdVo);
}