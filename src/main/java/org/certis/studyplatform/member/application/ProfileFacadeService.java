package org.certis.studyplatform.member.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.application.command.ProfileCommandService;
import org.certis.studyplatform.member.application.query.ProfileQueryService;
import org.certis.studyplatform.member.application.mapper.ProfileApplicationMapper;
import org.certis.studyplatform.member.application.object.command.UpdateProfileCommand;
import org.certis.studyplatform.member.application.object.query.GetProfileByMemberIdQuery;
import org.certis.studyplatform.member.application.object.query.GetProfileStudiesByMemberIdQuery;
import org.certis.studyplatform.member.application.object.query.GetProfileProjectsByMemberIdQuery;
import org.certis.studyplatform.member.application.object.query.GetProfileBlogsByMemberIdQuery;
import org.certis.studyplatform.member.domain.vo.ProfileVo;
import org.certis.studyplatform.member.domain.vo.ProfileStudyVo;
import org.certis.studyplatform.member.domain.vo.ProfileProjectVo;
import org.certis.studyplatform.member.domain.vo.ProfileBlogVo;
import org.certis.studyplatform.member.presentation.dto.request.ProfileUpdateRequestDto;
import org.certis.studyplatform.member.presentation.dto.response.ProfileInfoResponseDto;
import org.certis.studyplatform.member.presentation.dto.response.ProfileStudyResponseDto;
import org.certis.studyplatform.member.presentation.dto.response.ProfileProjectResponseDto;
import org.certis.studyplatform.member.presentation.dto.response.ProfileBlogResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Profile Facade Service (Query Object 적용 버전)
 *
 * ✅ CQRS Services 조율
 * ✅ 조합형 Application Mapper 활용한 데이터 변환
 * ✅ Query Object 패턴 적용으로 타입 안전성 확보
 * ✅ Clean Architecture: Parameter → Query Object → Domain → VO → ResponseDTO
 *
 * 개선 사항:
 * - 매퍼를 통한 Query Object 생성
 * - QueryService에 구조화된 Query 전달
 * - 타입 안전성과 의미 명확성 향상
 *
 * 흐름:
 * 1. Parameter → Mapper → Query Object
 * 2. Query Object → QueryService → Domain Logic → VO
 * 3. VO → Mapper → ResponseDTO
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileFacadeService {

    // CQRS Services
    private final ProfileCommandService profileCommandService;
    private final ProfileQueryService profileQueryService;

    // 조합형 통합 Application Mapper
    private final ProfileApplicationMapper profileMapper;

    /**
     * 내 프로필 정보 조회
     *
     * @param memberId 현재 로그인한 회원 ID
     * @return 프로필 정보 (ResponseDTO 반환)
     */
    public ProfileInfoResponseDto getMyProfile(Long memberId) {
        log.info("Getting my profile via facade - member ID: {}", memberId);

        // 1. Mapper를 통한 Parameter → Query Object 변환
        GetProfileByMemberIdQuery query = profileMapper.toGetProfileByMemberIdQuery(memberId);

        // 2. Query Service 실행 - Query Object → Domain Logic → VO 조회
        ProfileVo profileVo = profileQueryService.getProfileByMemberId(query);

        // 3. Mapper를 통한 VO → ResponseDTO 변환
        ProfileInfoResponseDto responseDto = profileMapper.toProfileInfoResponseDto(profileVo);

        log.info("My profile retrieved via facade - member ID: {}", memberId);

        return responseDto;
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

        // 1. Mapper를 통한 DTO → Command Object 변환
        UpdateProfileCommand command = profileMapper.toUpdateProfileCommand(memberId, request);

        // 2. Command Service 실행
        profileCommandService.updateMyProfile(command);

        log.info("My profile updated via facade - member ID: {}", memberId);
    }

    /**
     * 프로필 이미지 업로드
     *
     * @param memberId 현재 로그인한 회원 ID
     * @param file 업로드할 이미지 파일
     * @return 업로드된 이미지 URL
     */
    public String uploadProfileImage(Long memberId, MultipartFile file) {
        log.info("Uploading profile image via facade - member ID: {}", memberId);

        // S3에 이미지 업로드
        String imageUrl = profileCommandService.uploadProfileImage(memberId, file);

        log.info("Profile image uploaded via facade - member ID: {}, URL: {}", memberId, imageUrl);
        return imageUrl;
    }

    /**
     * 내가 관여한 스터디 목록 조회
     *
     * @param memberId 현재 로그인한 회원 ID
     * @return 스터디 목록 (ResponseDTO 반환)
     */
    public List<ProfileStudyResponseDto> getMyStudies(Long memberId) {
        log.info("Getting my studies via facade - member ID: {}", memberId);

        // 1. Mapper를 통한 Parameter → Query Object 변환
        GetProfileStudiesByMemberIdQuery query = profileMapper.toGetProfileStudiesByMemberIdQuery(memberId);

        // 2. Query Service 실행 - Query Object → Domain Logic → VO 목록 조회
        List<ProfileStudyVo> studyVos = profileQueryService.getStudiesByMemberId(query);

        // 3. Mapper를 통한 VO → ResponseDTO 변환
        List<ProfileStudyResponseDto> responseDtos = profileMapper.toProfileStudyResponseDtoList(studyVos);

        log.info("My studies retrieved via facade - member ID: {}, count: {}", memberId, responseDtos.size());

        return responseDtos;
    }

    /**
     * 내가 관여한 프로젝트 목록 조회
     *
     * @param memberId 현재 로그인한 회원 ID
     * @return 프로젝트 목록 (ResponseDTO 반환)
     */
    public List<ProfileProjectResponseDto> getMyProjects(Long memberId) {
        log.info("Getting my projects via facade - member ID: {}", memberId);

        // 1. Mapper를 통한 Parameter → Query Object 변환
        GetProfileProjectsByMemberIdQuery query = profileMapper.toGetProfileProjectsByMemberIdQuery(memberId);

        // 2. Query Service 실행 - Query Object → Domain Logic → VO 목록 조회
        List<ProfileProjectVo> projectVos = profileQueryService.getProjectsByMemberId(query);

        // 3. Mapper를 통한 VO → ResponseDTO 변환
        List<ProfileProjectResponseDto> responseDtos = profileMapper.toProfileProjectResponseDtoList(projectVos);

        log.info("My projects retrieved via facade - member ID: {}, count: {}", memberId, responseDtos.size());

        return responseDtos;
    }

    /**
     * 내가 작성한 블로그 목록 조회
     *
     * @param memberId 현재 로그인한 회원 ID
     * @return 블로그 목록 (ResponseDTO 반환)
     */
    public List<ProfileBlogResponseDto> getMyBlogs(Long memberId) {
        log.info("Getting my blogs via facade - member ID: {}", memberId);

        // 1. Mapper를 통한 Parameter → Query Object 변환
        GetProfileBlogsByMemberIdQuery query = profileMapper.toGetProfileBlogsByMemberIdQuery(memberId);

        // 2. Query Service 실행 - Query Object → Domain Logic → VO 목록 조회
        List<ProfileBlogVo> blogVos = profileQueryService.getBlogsByMemberId(query);

        // 3. Mapper를 통한 VO → ResponseDTO 변환
        List<ProfileBlogResponseDto> responseDtos = profileMapper.toProfileBlogResponseDtoList(blogVos);

        log.info("My blogs retrieved via facade - member ID: {}, count: {}", memberId, responseDtos.size());

        return responseDtos;
    }
}