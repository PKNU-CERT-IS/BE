package org.certis.studyplatform.member.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.shared.security.CurrentUser;
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
 * memberId -> currentUser 정보로 조회
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileFacadeService profileFacadeService;

    /**
     * 프로필 정보 조회
     * GET /api/v1/profile/me
     */
    @GetMapping("/me")
    public ResponseEntity<GlobalResponseHandler<ProfileInfoResponseDto>> getProfile(
            @AuthenticationPrincipal CurrentUser currentUser) {
        log.info("REST: Getting profile for member ID: {}", currentUser.getId());

        ProfileInfoResponseDto profileResponseDto = profileFacadeService.getMyProfile(currentUser.getId());

        log.info("REST: Profile retrieved successfully for member ID: {}", currentUser.getId());

        return GlobalResponseHandler.success(ResponseStatus.PROFILE_FIND_SUCCESS, profileResponseDto);
    }

    /**
     * 프로필 정보 수정
     * PUT /api/v1/profile/me
     */
    @PutMapping("/me")
    public ResponseEntity<GlobalResponseHandler<Void>> updateProfile(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ProfileUpdateRequestDto request) {
        log.info("REST: Updating profile for member ID: {}", currentUser.getId());

        profileFacadeService.updateMyProfile(currentUser.getId(), request);

        log.info("REST: Profile updated successfully for member ID: {}", currentUser.getId());

        return GlobalResponseHandler.success(ResponseStatus.PROFILE_UPDATE_SUCCESS);
    }

    /**
     * 스터디 내용 조회
     * GET /api/v1/profile/me/study
     */
    @GetMapping("/me/study")
    public ResponseEntity<GlobalResponseHandler<List<ProfileStudyResponseDto>>> getStudies(
            @AuthenticationPrincipal CurrentUser currentUser) {
        log.info("REST: Getting studies for member ID: {}", currentUser);

        List<ProfileStudyResponseDto> studies = profileFacadeService.getMyStudies(currentUser.getId());

        log.info("REST: Found {} studies for member ID: {}", studies.size(), currentUser.getId());

        return GlobalResponseHandler.success(ResponseStatus.PROFILE_FIND_SUCCESS, studies);
    }

    /**
     * 프로젝트 내용 조회
     * GET /api/v1/profile/me/project
     */
    @GetMapping("/me/project")
    public ResponseEntity<GlobalResponseHandler<List<ProfileProjectResponseDto>>> getProjects(
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("REST: Getting projects for member ID: {}", currentUser.getId());

        List<ProfileProjectResponseDto> projects = profileFacadeService.getMyProjects(currentUser.getId());

        log.info("REST: Found {} projects for member ID: {}", projects.size(), currentUser.getId());

        return GlobalResponseHandler.success(ResponseStatus.PROFILE_FIND_SUCCESS, projects);
    }

    /**
     * 블로그 내용 조회
     * GET /api/v1/profile/me/blog
     */
    @GetMapping("/me/blog")
    public ResponseEntity<GlobalResponseHandler<List<ProfileBlogResponseDto>>> getBlogs(
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("REST: Getting blogs for member ID: {}", currentUser.getId());

        List<ProfileBlogResponseDto> blogs = profileFacadeService.getMyBlogs(currentUser.getId());

        log.info("REST: Found {} blogs for member ID: {}", blogs.size(), currentUser.getId());

        return GlobalResponseHandler.success(ResponseStatus.PROFILE_FIND_SUCCESS, blogs);
    }
}