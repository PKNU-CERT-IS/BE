package org.certis.studyplatform.member.application.mapper;

import lombok.RequiredArgsConstructor;
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
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Profile Application Layer 통합 매퍼 (Composition 패턴)
 *
 * 분리된 매퍼들을 조합하여 단일 인터페이스를 제공합니다:
 * 1. ProfileApplicationCommandMapper - Command 변환 담당
 * 2. ProfileApplicationDtoMapper - DTO 변환 담당
 * 3. ProfileApplicationQueryMapper - Query 변환 담당
 *
 * ✅ 각 매퍼의 독립성 유지
 * ✅ 단일 책임 원칙 준수
 * ✅ 사용하는 쪽에서는 단일 인터페이스 제공
 * ✅ 개별 매퍼 단위 테스트 가능
 */
@Component
@RequiredArgsConstructor
public class ProfileApplicationMapper {

    // 분리된 매퍼들을 의존성 주입
    private final ProfileApplicationCommandMapper commandMapper;
    private final ProfileApplicationDtoMapper dtoMapper;
    private final ProfileApplicationQueryMapper queryMapper;

    // ================================================================
    // REQUEST DTO -> COMMAND/QUERY (CommandMapper 위임)
    // ================================================================

    /**
     * ProfileUpdateRequestDto → UpdateProfileCommand 변환
     * CommandMapper에 위임
     */
    public UpdateProfileCommand toUpdateProfileCommand(Long memberId, ProfileUpdateRequestDto dto) {
        return commandMapper.toUpdateProfileCommand(memberId, dto);
    }

    /**
     * memberId → GetProfileByMemberIdQuery 변환
     * QueryMapper에 위임
     */
    public GetProfileByMemberIdQuery toGetProfileByMemberIdQuery(Long memberId) {
        return queryMapper.toGetProfileByMemberIdQuery(memberId);
    }

    /**
     * memberId → GetProfileStudiesByMemberIdQuery 변환
     * QueryMapper에 위임
     */
    public GetProfileStudiesByMemberIdQuery toGetProfileStudiesByMemberIdQuery(Long memberId) {
        return queryMapper.toGetProfileStudiesByMemberIdQuery(memberId);
    }

    /**
     * memberId → GetProfileProjectsByMemberIdQuery 변환
     * QueryMapper에 위임
     */
    public GetProfileProjectsByMemberIdQuery toGetProfileProjectsByMemberIdQuery(Long memberId) {
        return queryMapper.toGetProfileProjectsByMemberIdQuery(memberId);
    }

    /**
     * memberId → GetProfileBlogsByMemberIdQuery 변환
     * QueryMapper에 위임
     */
    public GetProfileBlogsByMemberIdQuery toGetProfileBlogsByMemberIdQuery(Long memberId) {
        return queryMapper.toGetProfileBlogsByMemberIdQuery(memberId);
    }

    // ================================================================
    // DOMAIN VO -> RESPONSE DTO (DtoMapper 위임)
    // ================================================================

    /**
     * ProfileVo → ProfileInfoResponseDto 변환
     * DtoMapper에 위임
     */
    public ProfileInfoResponseDto toProfileInfoResponseDto(ProfileVo vo) {
        return dtoMapper.toProfileInfoResponseDto(vo);
    }

    /**
     * ProfileStudyVo → ProfileStudyResponseDto 변환
     * DtoMapper에 위임
     */
    public ProfileStudyResponseDto toProfileStudyResponseDto(ProfileStudyVo vo) {
        return dtoMapper.toProfileStudyResponseDto(vo);
    }

    /**
     * ProfileProjectVo → ProfileProjectResponseDto 변환
     * DtoMapper에 위임
     */
    public ProfileProjectResponseDto toProfileProjectResponseDto(ProfileProjectVo vo) {
        return dtoMapper.toProfileProjectResponseDto(vo);
    }

    /**
     * ProfileBlogVo → ProfileBlogResponseDto 변환
     * DtoMapper에 위임
     */
    public ProfileBlogResponseDto toProfileBlogResponseDto(ProfileBlogVo vo) {
        return dtoMapper.toProfileBlogResponseDto(vo);
    }

    // ================================================================
    // COLLECTION MAPPING (DtoMapper 위임)
    // ================================================================

    /**
     * List<ProfileStudyVo> → List<ProfileStudyResponseDto> 변환
     * DtoMapper에 위임
     */
    public List<ProfileStudyResponseDto> toProfileStudyResponseDtoList(List<ProfileStudyVo> voList) {
        return dtoMapper.toProfileStudyResponseDtoList(voList);
    }

    /**
     * List<ProfileProjectVo> → List<ProfileProjectResponseDto> 변환
     * DtoMapper에 위임
     */
    public List<ProfileProjectResponseDto> toProfileProjectResponseDtoList(List<ProfileProjectVo> voList) {
        return dtoMapper.toProfileProjectResponseDtoList(voList);
    }

    /**
     * List<ProfileBlogVo> → List<ProfileBlogResponseDto> 변환
     * DtoMapper에 위임
     */
    public List<ProfileBlogResponseDto> toProfileBlogResponseDtoList(List<ProfileBlogVo> voList) {
        return dtoMapper.toProfileBlogResponseDtoList(voList);
    }
}