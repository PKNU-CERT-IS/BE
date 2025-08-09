package org.certis.studyplatform.member.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.common.security.CurrentUser;
import org.certis.studyplatform.member.application.ProfileFacadeService;
import org.certis.studyplatform.member.presentation.dto.request.ProfileUpdateRequestDto;
import org.certis.studyplatform.member.presentation.dto.response.ProfileInfoResponseDto;
import org.certis.studyplatform.member.presentation.dto.response.ProfileStudyResponseDto;
import org.certis.studyplatform.member.presentation.dto.response.ProfileProjectResponseDto;
import org.certis.studyplatform.member.presentation.dto.response.ProfileBlogResponseDto;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Profile REST Controller
 *
 * 프로필 관련 REST API 엔드포인트 제공
 * 현재 로그인한 사용자 기준의 프로필 정보 처리
 *
 * ✅ RequestDTO → Facade → VO → ResponseDTO → Controller 패턴 적용
 * ✅ Presentation Layer에서 적절한 ResponseDTO 반환
 *
 * 책임:
 * - 자신의 프로필 정보 조회/수정
 * - 관련된 스터디/프로젝트/블로그 정보 조회
 * - HTTP 요청/응답 처리
 * - 입력 검증 및 응답 포맷팅
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileFacadeService profileFacadeService;

    /**
     * 프로필 정보 조회
     * GET /api/v1/profile/me/{memberId}
     */
    @GetMapping("/me/{memberId}")
    public ResponseEntity<GlobalResponseHandler<ProfileInfoResponseDto>> getProfile(
            @PathVariable Long memberId) {
        log.info("REST: Getting profile for member ID: {}", memberId);

        ProfileInfoResponseDto profileResponseDto = profileFacadeService.getMyProfile(memberId);

        log.info("REST: Profile retrieved successfully for member ID: {}", memberId);

        return GlobalResponseHandler.success(ResponseStatus.PROFILE_FIND_SUCCESS, profileResponseDto);
    }

    /**
     * 프로필 정보 수정
     * PUT /api/v1/profile/me/{memberId}
     */
    @PutMapping("/me/{memberId}")
    public ResponseEntity<GlobalResponseHandler<Void>> updateProfile(
            @PathVariable Long memberId,
            @Valid @RequestBody ProfileUpdateRequestDto request) {
        log.info("REST: Updating profile for member ID: {}", memberId);

        profileFacadeService.updateMyProfile(memberId, request);

        log.info("REST: Profile updated successfully for member ID: {}", memberId);

        return GlobalResponseHandler.success(ResponseStatus.PROFILE_UPDATE_SUCCESS);
    }

    /**
     * 스터디 내용 조회
     * GET /api/v1/profile/{memberId}/study
     */
    @GetMapping("/{memberId}/study")
    public ResponseEntity<GlobalResponseHandler<List<ProfileStudyResponseDto>>> getStudies(
            @PathVariable Long memberId) {
        log.info("REST: Getting studies for member ID: {}", memberId);

        List<ProfileStudyResponseDto> studies = profileFacadeService.getMyStudies(memberId);

        log.info("REST: Found {} studies for member ID: {}", studies.size(), memberId);

        return GlobalResponseHandler.success(ResponseStatus.PROFILE_FIND_SUCCESS, studies);
    }

    /**
     * 프로젝트 내용 조회
     * GET /api/v1/profile/{memberId}/project
     */
    @GetMapping("/{memberId}/project")
    public ResponseEntity<GlobalResponseHandler<List<ProfileProjectResponseDto>>> getProjects(
            @PathVariable Long memberId) {
        log.info("REST: Getting projects for member ID: {}", memberId);

        List<ProfileProjectResponseDto> projects = profileFacadeService.getMyProjects(memberId);

        log.info("REST: Found {} projects for member ID: {}", projects.size(), memberId);

        return GlobalResponseHandler.success(ResponseStatus.PROFILE_FIND_SUCCESS, projects);
    }

    /**
     * 블로그 내용 조회
     * GET /api/v1/profile/{memberId}/blog
     */
    @GetMapping("/{memberId}/blog")
    public ResponseEntity<GlobalResponseHandler<List<ProfileBlogResponseDto>>> getBlogs(
            @PathVariable Long memberId) {
        log.info("REST: Getting blogs for member ID: {}", memberId);

        List<ProfileBlogResponseDto> blogs = profileFacadeService.getMyBlogs(memberId);

        log.info("REST: Found {} blogs for member ID: {}", blogs.size(), memberId);

        return GlobalResponseHandler.success(ResponseStatus.PROFILE_FIND_SUCCESS, blogs);
    }

//    /**
//     * 자신의 프로필 정보 조회
//     * GET /api/v1/profile/me
//     */
//    @GetMapping("/me")
//    public ResponseEntity<GlobalResponseHandler<ProfileInfoResponseDto>> getMyProfile(
//            @AuthenticationPrincipal CurrentUser currentUser) {
//        log.info("REST: Getting my profile for user ID: {}", currentUser.getId());
//
//        ProfileInfoResponseDto profileResponseDto = profileFacadeService.getMyProfile(currentUser.getId());
//
//        log.info("REST: My profile retrieved successfully for user ID: {}", currentUser.getId());
//
//        return GlobalResponseHandler.success(ResponseStatus.PROFILE_FIND_SUCCESS, profileResponseDto);
//    }
//
//    /**
//     * 자신의 프로필 정보 수정
//     * PUT /api/v1/profile/me
//     */
//    @PutMapping("/me")
//    public ResponseEntity<GlobalResponseHandler<Void>> updateMyProfile(
//            @AuthenticationPrincipal CurrentUser currentUser,
//            @Valid @RequestBody ProfileUpdateRequestDto request) {
//        log.info("REST: Updating my profile for user ID: {}", currentUser.getId());
//
//        profileFacadeService.updateMyProfile(currentUser.getId(), request);
//
//        log.info("REST: My profile updated successfully for user ID: {}", currentUser.getId());
//
//        return GlobalResponseHandler.success(ResponseStatus.PROFILE_UPDATE_SUCCESS);
//    }
//
//    /**
//     * 자신이 관여한 스터디 내용 조회
//     * GET /api/v1/profile/study
//     */
//    @GetMapping("/study")
//    public ResponseEntity<GlobalResponseHandler<List<ProfileStudyResponseDto>>> getMyStudies(
//            @AuthenticationPrincipal CurrentUser currentUser) {
//        log.info("REST: Getting my studies for user ID: {}", currentUser.getId());
//
//        List<ProfileStudyResponseDto> studies = profileFacadeService.getMyStudies(currentUser.getId());
//
//        log.info("REST: Found {} studies for user ID: {}", studies.size(), currentUser.getId());
//
//        return GlobalResponseHandler.success(ResponseStatus.PROFILE_FIND_SUCCESS, studies);
//    }
//
//    /**
//     * 자신이 관여한 프로젝트 내용 조회
//     * GET /api/v1/profile/project
//     */
//    @GetMapping("/project")
//    public ResponseEntity<GlobalResponseHandler<List<ProfileProjectResponseDto>>> getMyProjects(
//            @AuthenticationPrincipal CurrentUser currentUser) {
//        log.info("REST: Getting my projects for user ID: {}", currentUser.getId());
//
//        List<ProfileProjectResponseDto> projects = profileFacadeService.getMyProjects(currentUser.getId());
//
//        log.info("REST: Found {} projects for user ID: {}", projects.size(), currentUser.getId());
//
//        return GlobalResponseHandler.success(ResponseStatus.PROFILE_FIND_SUCCESS, projects);
//    }
//
//    /**
//     * 자신이 관여한 블로그 내용 조회
//     * GET /api/v1/profile/blog
//     */
//    @GetMapping("/blog")
//    public ResponseEntity<GlobalResponseHandler<List<ProfileBlogResponseDto>>> getMyBlogs(
//            @AuthenticationPrincipal CurrentUser currentUser) {
//        log.info("REST: Getting my blogs for user ID: {}", currentUser.getId());
//
//        List<ProfileBlogResponseDto> blogs = profileFacadeService.getMyBlogs(currentUser.getId());
//
//        log.info("REST: Found {} blogs for user ID: {}", blogs.size(), currentUser.getId());
//
//        return GlobalResponseHandler.success(ResponseStatus.PROFILE_FIND_SUCCESS, blogs);
//    }
}