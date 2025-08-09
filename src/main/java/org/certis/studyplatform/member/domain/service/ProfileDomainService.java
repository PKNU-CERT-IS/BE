package org.certis.studyplatform.member.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.application.object.command.UpdateProfileCommand;
import org.certis.studyplatform.member.application.object.query.GetProfileBlogsByMemberIdQuery;
import org.certis.studyplatform.member.application.object.query.GetProfileByMemberIdQuery;
import org.certis.studyplatform.member.application.object.query.GetProfileProjectsByMemberIdQuery;
import org.certis.studyplatform.member.application.object.query.GetProfileStudiesByMemberIdQuery;
import org.certis.studyplatform.member.domain.repository.command.ProfileCommandRepository;
import org.certis.studyplatform.member.domain.repository.query.ProfileQueryRepository;
import org.certis.studyplatform.member.domain.vo.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Profile Domain Service
 *
 * Clean Architecture Domain Layer의 서비스
 * 순수한 비즈니스 로직과 도메인 규칙을 처리
 *
 * 책임:
 * - 내 프로필 관련 도메인 로직 처리
 * - 도메인 규칙 검증
 * - 도메인 객체 간 상호작용 조정
 * - Repository 인터페이스를 통한 영속성 추상화
 *
 * 특징:
 * - Infrastructure를 모름 (Repository Interface만 사용)
 * - 순수한 비즈니스 로직에만 집중
 * - Application Service가 이 서비스를 호출
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileDomainService {

    private final ProfileCommandRepository profileCommandRepository;
    private final ProfileQueryRepository profileQueryRepository;

    // ================================================================
    // COMMAND OPERATIONS (쓰기 작업)
    // ================================================================

    /**
     * 내 프로필 정보 수정 (Profile domain 객체 사용, VO 반환)
     *
     * @param command 회원 ID
     * @return 수정된 프로필 VO
     */
    public ProfileVo updateMyProfile(UpdateProfileCommand command) {
        log.info("Domain: Updating my profile for member ID: {}", command.memberId());

        MemberIdVo memberIdVo = new MemberIdVo(command.memberId());

        // 1. 기존 프로필 조회 (없으면 예외 발생)
        Optional<ProfileVo> profileVo = profileQueryRepository.findByMemberId(memberIdVo);

        if (profileVo.get() == null) {
            throw new DomainException(ExceptionStatus.PROFILE_DOMAIN_NOT_FOUND);
        }

        // 2. 프로필 수정 권한 검증
        validateProfileUpdatePermission(profileVo.get(), command.memberId());


        // 5. 영속화
        ProfileVo savedProfile = profileCommandRepository.save(profileVo.get());

        log.info("Domain: My profile updated successfully for member ID: {}", command.memberId());
        
        // 6. Profile domain 객체를 VO로 변환해서 반환
        return savedProfile;
    }

    // ================================================================
    // QUERY OPERATIONS (읽기 작업)
    // ================================================================

    /**
     * 회원 ID로 프로필 조회 (Query용 - VO 반환)
     *
     * @param query GetProfileByMemberIdQuery
     * @return 프로필 VO
     */
    public ProfileVo getProfileVoByMemberId(GetProfileByMemberIdQuery query) {
        log.info("Domain: Getting profile VO by member ID: {}", query.memberId());

        MemberIdVo memberIdVo = new MemberIdVo(query.memberId());

        Optional<ProfileVo> profileVo = profileQueryRepository.findByMemberId(memberIdVo);

        log.info("Domain: Profile VO found for member ID: {}", memberIdVo);
        return profileVo.orElse(null);
    }

    /**
     * 회원이 관여한 스터디 목록 조회
     *
     * @param query GetProfileStudiesByMemberIdQuery
     * @return 스터디 VO 목록
     */
    public List<ProfileStudyVo> getStudiesByMemberId(GetProfileStudiesByMemberIdQuery query) {
        log.info("Domain: Getting studies by member ID: {}", query.memberId());

        MemberIdVo memberIdVo = new MemberIdVo(query.memberId());

        List<ProfileStudyVo> studyVos = profileQueryRepository.findStudiesByMemberId(memberIdVo);

        log.info("Domain: Found {} studies for member ID: {}", studyVos.size(), query.memberId());
        return studyVos;
    }

    /**
     * 회원이 관여한 프로젝트 목록 조회
     *
     * @param query 회원 ID
     * @return 프로젝트 VO 목록
     */
    public List<ProfileProjectVo> getProjectsByMemberId(GetProfileProjectsByMemberIdQuery query) {
        log.info("Domain: Getting projects by member ID: {}", query.memberId());

        MemberIdVo memberIdVo = new MemberIdVo(query.memberId());

        List<ProfileProjectVo> projectVos = profileQueryRepository.findProjectsByMemberId(memberIdVo);

        log.info("Domain: Found {} projects for member ID: {}", projectVos.size(), memberIdVo);
        return projectVos;
    }

    /**
     * 회원이 작성한 블로그 목록 조회
     *
     * @param query 회원 ID
     * @return 블로그 VO 목록
     */
    public List<ProfileBlogVo> getBlogsByMemberId(GetProfileBlogsByMemberIdQuery query) {
        log.info("Domain: Getting blogs by member ID: {}", query.memberId());

        MemberIdVo memberIdVo = new MemberIdVo(query.memberId());

        List<ProfileBlogVo> blogVos = profileQueryRepository.findBlogsByMemberId(memberIdVo);

        log.info("Domain: Found {} blogs for member ID: {}", blogVos.size(), memberIdVo);
        return blogVos;
    }

//    /**
//     * 프로필 존재 여부 확인
//     *
//     * @param memberId 회원 ID
//     * @return 존재 여부
//     */
//    public boolean isProfileExists(Long memberId) {
//        return profileCommandRepository.existsByMemberId(memberId);
//    }

    // ================================================================
    // PRIVATE VALIDATION METHODS (도메인 규칙 검증)
    // ================================================================

    private void validateProfileUpdatePermission(ProfileVo profileVo, Long memberId) {
        // 본인의 프로필인지 확인
        if (!profileVo.memberId().equals(memberId)) {
            throw new DomainException(ExceptionStatus.PROFILE_DOMAIN_UPDATE_NOT_ALLOWED,
                    "자신의 프로필만 수정할 수 있습니다.");
        }
    }
//
//    private void validateProfileUpdateRules(Profile profile) {
//        // 프로필 이름 유효성 검증
//        if (profile.getName() == null || profile.getName().trim().isEmpty()) {
//            throw new DomainException(ExceptionStatus.PROFILE_DOMAIN_INVALID_NAME,
//                    "프로필 이름은 필수입니다.");
//        }
//
//        if (profile.getName().length() > 50) {
//            throw new DomainException(ExceptionStatus.PROFILE_DOMAIN_INVALID_NAME,
//                    "프로필 이름은 50자를 초과할 수 없습니다.");
//        }
//
//        // 설명 길이 검증
//        if (profile.getDescription() != null && profile.getDescription().length() > 500) {
//            throw new DomainException(ExceptionStatus.PROFILE_DOMAIN_INVALID_DESCRIPTION,
//                    "프로필 설명은 500자를 초과할 수 없습니다.");
//        }
//
//        // 프로필 이미지 URL 검증
//        if (profile.getProfileImageValue() != null &&
//                profile.getProfileImageValue().length() > 500) {
//            throw new DomainException(ExceptionStatus.PROFILE_DOMAIN_INVALID_IMAGE_URL,
//                    "프로필 이미지 URL이 너무 깁니다.");
//        }
//
//        // 기타 비즈니스 규칙 검증...
//        validateBusinessRules(profile);
//    }
//
//    private void validateBusinessRules(Profile profile) {
//        // 추가적인 비즈니스 규칙들
//        // 예: 특정 단어 금지, 이미지 포맷 확인 등
//
//        // 부적절한 내용 필터링 (향후 구현)
//        // if (containsInappropriateContent(profile.getDescription())) {
//        //     throw new DomainException(ExceptionStatus.PROFILE_DOMAIN_INAPPROPRIATE_CONTENT,
//        //             "부적절한 내용이 포함되어 있습니다.");
//        // }
//    }
}