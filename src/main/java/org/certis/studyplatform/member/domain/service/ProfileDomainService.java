package org.certis.studyplatform.member.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.Profile;
import org.certis.studyplatform.member.domain.repository.command.ProfileCommandRepository;
import org.certis.studyplatform.member.domain.repository.query.ProfileQueryRepository;
import org.certis.studyplatform.member.domain.vo.ProfileVo;
import org.certis.studyplatform.member.domain.vo.ProfileStudyVo;
import org.certis.studyplatform.member.domain.vo.ProfileProjectVo;
import org.certis.studyplatform.member.domain.vo.ProfileBlogVo;
import org.springframework.stereotype.Service;

import java.util.List;

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
     * 내 프로필 정보 수정 (도메인 로직 포함)
     *
     * @param memberId 회원 ID
     * @param updateAction 업데이트 액션 (함수형 인터페이스)
     * @return 수정된 프로필 도메인 객체
     */
    public Profile updateMyProfile(Long memberId, ProfileUpdateAction updateAction) {
        log.info("Domain: Updating my profile for member ID: {}", memberId);

        // 1. 기존 프로필 조회 (없으면 예외 발생)
        Profile profile = findProfileByMemberId(memberId);

        // 2. 프로필 수정 권한 검증
        validateProfileUpdatePermission(profile, memberId);

        // 3. 도메인 로직을 통한 업데이트
        updateAction.apply(profile);

        // 4. 도메인 규칙 검증
        validateProfileUpdateRules(profile);

        // 5. 영속화
        Profile savedProfile = profileCommandRepository.save(profile);

        log.info("Domain: My profile updated successfully for member ID: {}", memberId);
        return savedProfile;
    }

    // ================================================================
    // QUERY OPERATIONS (읽기 작업)
    // ================================================================

    /**
     * 회원 ID로 프로필 조회 (도메인 서비스용)
     *
     * @param memberId 회원 ID
     * @return 프로필 도메인 객체
     */
    public Profile findProfileByMemberId(Long memberId) {
        return profileCommandRepository.findByMemberId(memberId)
                .orElseThrow(() -> new DomainException(ExceptionStatus.PROFILE_DOMAIN_NOT_FOUND,
                        "프로필을 찾을 수 없습니다. 회원 ID: " + memberId));
    }

    /**
     * 회원 ID로 프로필 조회 (Query용)
     *
     * @param memberId 회원 ID
     * @return 프로필 VO
     */
    public ProfileVo getProfileVoByMemberId(Long memberId) {
        log.info("Domain: Getting profile VO by member ID: {}", memberId);

        Profile profile = profileQueryRepository.findByMemberId(memberId)
                .orElseThrow(() -> new DomainException(ExceptionStatus.PROFILE_DOMAIN_NOT_FOUND,
                        "프로필을 찾을 수 없습니다. 회원 ID: " + memberId));

        ProfileVo profileVo = ProfileVo.from(profile);

        log.info("Domain: Profile VO found for member ID: {}", memberId);
        return profileVo;
    }

    /**
     * 회원이 관여한 스터디 목록 조회
     *
     * @param memberId 회원 ID
     * @return 스터디 VO 목록
     */
    public List<ProfileStudyVo> getStudiesByMemberId(Long memberId) {
        log.info("Domain: Getting studies by member ID: {}", memberId);

        List<ProfileStudyVo> studyVos = profileQueryRepository.findStudiesByMemberId(memberId);

        log.info("Domain: Found {} studies for member ID: {}", studyVos.size(), memberId);
        return studyVos;
    }

    /**
     * 회원이 관여한 프로젝트 목록 조회
     *
     * @param memberId 회원 ID
     * @return 프로젝트 VO 목록
     */
    public List<ProfileProjectVo> getProjectsByMemberId(Long memberId) {
        log.info("Domain: Getting projects by member ID: {}", memberId);

        List<ProfileProjectVo> projectVos = profileQueryRepository.findProjectsByMemberId(memberId);

        log.info("Domain: Found {} projects for member ID: {}", projectVos.size(), memberId);
        return projectVos;
    }

    /**
     * 회원이 작성한 블로그 목록 조회
     *
     * @param memberId 회원 ID
     * @return 블로그 VO 목록
     */
    public List<ProfileBlogVo> getBlogsByMemberId(Long memberId) {
        log.info("Domain: Getting blogs by member ID: {}", memberId);

        List<ProfileBlogVo> blogVos = profileQueryRepository.findBlogsByMemberId(memberId);

        log.info("Domain: Found {} blogs for member ID: {}", blogVos.size(), memberId);
        return blogVos;
    }

    /**
     * 프로필 존재 여부 확인
     *
     * @param memberId 회원 ID
     * @return 존재 여부
     */
    public boolean isProfileExists(Long memberId) {
        return profileCommandRepository.existsByMemberId(memberId);
    }

    // ================================================================
    // PRIVATE VALIDATION METHODS (도메인 규칙 검증)
    // ================================================================

    private void validateProfileUpdatePermission(Profile profile, Long memberId) {
        // 본인의 프로필인지 확인
        if (!profile.getMemberId().equals(memberId)) {
            throw new DomainException(ExceptionStatus.PROFILE_DOMAIN_UPDATE_NOT_ALLOWED,
                    "자신의 프로필만 수정할 수 있습니다.");
        }
    }

    private void validateProfileUpdateRules(Profile profile) {
        // 프로필 이름 유효성 검증
        if (profile.getName() == null || profile.getName().trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.PROFILE_DOMAIN_INVALID_NAME,
                    "프로필 이름은 필수입니다.");
        }

        if (profile.getName().length() > 50) {
            throw new DomainException(ExceptionStatus.PROFILE_DOMAIN_INVALID_NAME,
                    "프로필 이름은 50자를 초과할 수 없습니다.");
        }

        // 설명 길이 검증
        if (profile.getDescription() != null && profile.getDescription().length() > 500) {
            throw new DomainException(ExceptionStatus.PROFILE_DOMAIN_INVALID_DESCRIPTION,
                    "프로필 설명은 500자를 초과할 수 없습니다.");
        }

        // 프로필 이미지 URL 검증
        if (profile.getProfileImageValue() != null &&
                profile.getProfileImageValue().length() > 500) {
            throw new DomainException(ExceptionStatus.PROFILE_DOMAIN_INVALID_IMAGE_URL,
                    "프로필 이미지 URL이 너무 깁니다.");
        }

        // 기타 비즈니스 규칙 검증...
        validateBusinessRules(profile);
    }

    private void validateBusinessRules(Profile profile) {
        // 추가적인 비즈니스 규칙들
        // 예: 특정 단어 금지, 이미지 포맷 확인 등

        // 부적절한 내용 필터링 (향후 구현)
        // if (containsInappropriateContent(profile.getDescription())) {
        //     throw new DomainException(ExceptionStatus.PROFILE_INAPPROPRIATE_CONTENT,
        //             "부적절한 내용이 포함되어 있습니다.");
        // }
    }

    /**
     * 프로필 업데이트를 위한 함수형 인터페이스
     */
    @FunctionalInterface
    public interface ProfileUpdateAction {
        void apply(Profile profile);
    }
}