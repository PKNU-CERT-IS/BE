package org.certis.studyplatform.member.application.mapper;

import org.certis.studyplatform.member.application.object.command.CreateProfileCommand;
import org.certis.studyplatform.member.application.object.command.UpdateMemberCommand;
import org.certis.studyplatform.member.application.object.query.GetMemberByIdQuery;
import org.certis.studyplatform.member.application.object.query.GetMemberSummariesQuery;
import org.certis.studyplatform.member.application.object.query.SearchMembersQuery;
import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.presentation.dto.request.*;
import org.certis.studyplatform.member.presentation.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
/**
 * Application Layer Mapper for the entire Member context.
 *
 * This single mapper is responsible for all conversions between:
 * 1. Presentation DTOs <-> Application Commands/Queries
 * 2. Domain Entities/VOs <-> Presentation DTOs
 *
 * It uses the builder pattern for creating DTOs.
 */
@Component
public class MemberApplicationMapper {

    // ================================================================
    // Request DTO -> Command/Query (for Facade/Service input)
    // ================================================================


    public UpdateMemberCommand toUpdateCommand(Long id, MemberUpdateRequestDto dto) {
        if (dto == null) return null;
        return new UpdateMemberCommand(
                id,
                dto.getName(),
                null, // studentNumber는 업데이트 불가 (불변 필드)
                dto.getGrade(),
                dto.getRole(),
                dto.getMajor(),
                dto.getDescription(),
                dto.getSkills()
        );
    }

    public CreateProfileCommand toCreateProfileCommand(CreateProfileRequestDto dto) {
        if (dto == null) return null;
        return new CreateProfileCommand(
                dto.getMemberId(), dto.getName(), dto.getDescription(), dto.getProfileImageUrl()
        );
    }

    public GetMemberByIdQuery toGetMemberQuery(Long id) {
        return new GetMemberByIdQuery(id);
    }

    public SearchMembersQuery toSearchMembersQuery(SearchMembersRequestDto dto, Pageable pageable) {
        if (dto == null) {
            return new SearchMembersQuery(null, null, null, dto.getSkills(), pageable);
        }
        return new SearchMembersQuery(
                dto.getName(), dto.getGrade(), dto.getRole(), dto.getSkills(), pageable
        );
    }

    public GetMemberSummariesQuery toGetMemberSummariesQuery(SearchMembersRequestDto dto, Pageable pageable) {
        if (dto == null) {
            return new GetMemberSummariesQuery(null, null, null, pageable);
        }
        return new GetMemberSummariesQuery(
                dto.getName(), dto.getGrade(), dto.getRole(), pageable
        );
    }

    public MemberSearchResponseDto toMemberSearchResponseDto(MemberWithContactVo vo) {
        return MemberSearchResponseDto.of(
                vo.id(),
                vo.name(),
                vo.profileImage(),
                vo.grade(),
                vo.role(),
                vo.skills(),
                vo.major(),
                vo.description(),
                vo.createdAt(),
                vo.updatedAt(),
                vo.email(),
                vo.githubUrl(),
                vo.linkedinUrl()
        );
    }


    // ================================================================
    // Domain VO -> Response DTO (for Service output)
    // ================================================================

    /**
     * Converts a MemberSummaryVo to a MemberResponseDto.
     * This method now takes a VO instead of a full domain entity.
     * @param vo The MemberSummaryVo from the application layer.
     * @return The response DTO for the presentation layer.
     */
    public MemberResponseDto toMemberResponseDto(MemberSummaryVo vo) {
        if (vo == null) return null;
        return MemberResponseDto.builder()
                .id(vo.getMemberId())
                .name(vo.getNameValue())
                .studentNumber(vo.getStudentNumberValue())
                .grade(vo.getGradeValue().toString())
                .role(vo.getRoleValue().toString())
                .major(vo.getMajorValue())
                .description(vo.description())
                .skills(vo.getSkillsValues())
                .profileImage(vo.getProfileImageValue())
                .createdAt(vo.createdAt())
                .build();
    }

    /**
     * Converts a Page of MemberSummaryVo to a Page of MemberResponseDtos.
     * @param voPage The page of VOs from the application layer.
     * @return The page of DTOs for the presentation layer.
     */
    public Page<MemberResponseDto> toMemberResponseDtoPage(Page<MemberSummaryVo> voPage) {
        if (voPage == null) return null;
        return voPage.map(this::toMemberResponseDto);
    }
}
