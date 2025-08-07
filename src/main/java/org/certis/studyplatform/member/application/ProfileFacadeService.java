package org.certis.studyplatform.member.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.application.command.ProfileCommandService;
import org.certis.studyplatform.member.application.query.ProfileQueryService;
import org.certis.studyplatform.member.domain.vo.ProfileVo;
import org.certis.studyplatform.member.domain.vo.ProfileStudyVo;
import org.certis.studyplatform.member.domain.vo.ProfileProjectVo;
import org.certis.studyplatform.member.domain.vo.ProfileBlogVo;
import org.certis.studyplatform.member.presentation.dto.request.ProfileUpdateRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Profile Facade Service
 *
 * Controller와 Command/Query Service 사이의 복잡성을 숨기고
 * 단순하고 일관된 인터페이스를 제공하는 Facade 패턴 구현
 *
 * 현재 로그인한 사용자 기준의 프로필 관련 비즈니스 로직 처리
 *
 * ✅ RequestDTO → Facade → VO → Controller (VO 직접 반환) 패턴 적용
 *
 * 책임:
 * - 자신의 프로필 정보 관리
 * - 관련된 스터디/프로젝트/블로그 정보 조회
 * - Command/Query Service 오케스트레이션
 * - 트랜잭션 경계 관리
 * - Cross-cutting concerns 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileFacadeService {

    // CQRS Services
    private final ProfileCommandService profileCommandService;
    private final ProfileQueryService profileQueryService;

    /**
     * 내 프로필 정보 조회
     *
     * @param memberId 현재 로그인한 회원 ID
     * @return 프로필 정보 (VO 직접 반환)
     */
    public ProfileVo getMyProfile(Long memberId) {
        log.info("Getting my profile via facade - member ID: {}", memberId);

        // Query Service 실행
        ProfileVo profileVo = profileQueryService.getProfileByMemberId(memberId);

        log.info("My profile retrieved via facade - member ID: {}", memberId);

        return profileVo;
    }

    /**
     * 내 프로필 정보 수정
     *
     * @param memberId 현재 로그인한 회원 ID
     * @param request 프로필 수정 요청 DTO
     */
    @Transactional
    public void updateMyProfile(Long memberId, ProfileUpdateRequestDto request) {
        log.info("Updating my profile via facade - member ID: {}, fields: name={}, description={}, profileImage={}",
                memberId,
                request.getName() != null,
                request.getDescription() != null,
                request.getProfileImage() != null);

        // Command Service 실행
        profileCommandService.updateMyProfile(memberId, request);

        log.info("My profile updated via facade - member ID: {}", memberId);
    }

    /**
     * 내가 관여한 스터디 목록 조회
     *
     * @param memberId 현재 로그인한 회원 ID
     * @return 스터디 목록 (VO 직접 반환)
     */
    public List<ProfileStudyVo> getMyStudies(Long memberId) {
        log.info("Getting my studies via facade - member ID: {}", memberId);

        // Query Service 실행 - 스터디 정보 조회
        List<ProfileStudyVo> studies = profileQueryService.getStudiesByMemberId(memberId);

        log.info("My studies retrieved via facade - member ID: {}, count: {}", memberId, studies.size());

        return studies;
    }

    /**
     * 내가 관여한 프로젝트 목록 조회
     *
     * @param memberId 현재 로그인한 회원 ID
     * @return 프로젝트 목록 (VO 직접 반환)
     */
    public List<ProfileProjectVo> getMyProjects(Long memberId) {
        log.info("Getting my projects via facade - member ID: {}", memberId);

        // Query Service 실행 - 프로젝트 정보 조회
        List<ProfileProjectVo> projects = profileQueryService.getProjectsByMemberId(memberId);

        log.info("My projects retrieved via facade - member ID: {}, count: {}", memberId, projects.size());

        return projects;
    }

    /**
     * 내가 작성한 블로그 목록 조회
     *
     * @param memberId 현재 로그인한 회원 ID
     * @return 블로그 목록 (VO 직접 반환)
     */
    public List<ProfileBlogVo> getMyBlogs(Long memberId) {
        log.info("Getting my blogs via facade - member ID: {}", memberId);

        // Query Service 실행 - 블로그 정보 조회
        List<ProfileBlogVo> blogs = profileQueryService.getBlogsByMemberId(memberId);

        log.info("My blogs retrieved via facade - member ID: {}, count: {}", memberId, blogs.size());

        return blogs;
    }
}