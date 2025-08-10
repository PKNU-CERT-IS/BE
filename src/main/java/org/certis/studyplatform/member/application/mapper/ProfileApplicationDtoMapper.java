package org.certis.studyplatform.member.application.mapper;

import org.certis.studyplatform.member.domain.vo.ProfileVo;
import org.certis.studyplatform.member.domain.vo.ProfileStudyVo;
import org.certis.studyplatform.member.domain.vo.ProfileProjectVo;
import org.certis.studyplatform.member.domain.vo.ProfileBlogVo;
import org.certis.studyplatform.member.presentation.dto.response.ProfileInfoResponseDto;
import org.certis.studyplatform.member.presentation.dto.response.ProfileStudyResponseDto;
import org.certis.studyplatform.member.presentation.dto.response.ProfileProjectResponseDto;
import org.certis.studyplatform.member.presentation.dto.response.ProfileBlogResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Profile VO to DTO 변환을 담당하는 Mapper
 * Clean Architecture: Application Layer에서 Domain VO → Presentation DTO 변환
 */
@Component
public class ProfileApplicationDtoMapper {

    // ================================================================
    // PROFILE RESPONSE MAPPING
    // ================================================================

    /**
     * ProfileVo → ProfileInfoResponseDto 변환
     */
    public ProfileInfoResponseDto toProfileInfoResponseDto(ProfileVo profileVo) {
        if (profileVo == null) return null;

        return ProfileInfoResponseDto.builder()
                .memberId(profileVo.memberId())
                .name(profileVo.name())
                .description(profileVo.description())
                .profileImage(profileVo.profileImage())
                .build();
    }

    // ================================================================
    // STUDY RESPONSE MAPPING
    // ================================================================

    /**
     * ProfileStudyVo → ProfileStudyResponseDto 변환
     */
    public ProfileStudyResponseDto toProfileStudyResponseDto(ProfileStudyVo studyVo) {
        if (studyVo == null) return null;

        return ProfileStudyResponseDto.builder()
                .studyId(studyVo.studyId())
                .title(studyVo.title())
                .description(studyVo.description())
                .status(studyVo.status())
                .studyStartDate(studyVo.studyStartDate())
                .studyEndDate(studyVo.studyEndDate())
                .build();
    }

    /**
     * List<ProfileStudyVo> → List<ProfileStudyResponseDto> 변환
     */
    public List<ProfileStudyResponseDto> toProfileStudyResponseDtoList(List<ProfileStudyVo> studyVos) {
        if (studyVos == null) return null;

        return studyVos.stream()
                .map(this::toProfileStudyResponseDto)
                .collect(Collectors.toList());
    }

    // ================================================================
    // PROJECT RESPONSE MAPPING
    // ================================================================

    /**
     * ProfileProjectVo → ProfileProjectResponseDto 변환
     */
    public ProfileProjectResponseDto toProfileProjectResponseDto(ProfileProjectVo projectVo) {
        if (projectVo == null) return null;

        return ProfileProjectResponseDto.builder()
                .projectId(projectVo.projectId())
                .title(projectVo.title())
                .description(projectVo.description())
                .status(projectVo.status())
                .projectStartDate(projectVo.projectStartDate())
                .projectEndDate(projectVo.projectEndDate())
                .build();
    }

    /**
     * List<ProfileProjectVo> → List<ProfileProjectResponseDto> 변환
     */
    public List<ProfileProjectResponseDto> toProfileProjectResponseDtoList(List<ProfileProjectVo> projectVos) {
        if (projectVos == null) return null;

        return projectVos.stream()
                .map(this::toProfileProjectResponseDto)
                .collect(Collectors.toList());
    }

    // ================================================================
    // BLOG RESPONSE MAPPING
    // ================================================================

    /**
     * ProfileBlogVo → ProfileBlogResponseDto 변환
     */
    public ProfileBlogResponseDto toProfileBlogResponseDto(ProfileBlogVo blogVo) {
        if (blogVo == null) return null;

        return ProfileBlogResponseDto.builder()
                .blogId(blogVo.blogId())
                .title(blogVo.title())
                .tags(blogVo.tags())
                .viewCount(blogVo.viewCount())
                .likeCount(blogVo.likeCount())
                .createdAt(blogVo.createdAt())
                .build();
    }

    /**
     * List<ProfileBlogVo> → List<ProfileBlogResponseDto> 변환
     */
    public List<ProfileBlogResponseDto> toProfileBlogResponseDtoList(List<ProfileBlogVo> blogVos) {
        if (blogVos == null) return null;

        return blogVos.stream()
                .map(this::toProfileBlogResponseDto)
                .collect(Collectors.toList());
    }
}