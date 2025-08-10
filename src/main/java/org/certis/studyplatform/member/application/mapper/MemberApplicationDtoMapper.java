package org.certis.studyplatform.member.application.mapper;

import org.certis.studyplatform.member.domain.vo.MemberCreatedVo;
import org.certis.studyplatform.member.domain.vo.MemberSummaryVo;
import org.certis.studyplatform.member.domain.vo.MemberUpdatedVo;
import org.certis.studyplatform.member.domain.vo.MemberVo;
import org.certis.studyplatform.member.presentation.dto.response.MemberCreatedResponseDto;
import org.certis.studyplatform.member.presentation.dto.response.MemberUpdatedResponseDto;
import org.certis.studyplatform.member.presentation.dto.response.MemberSummaryResponseDto;
import org.certis.studyplatform.member.presentation.dto.response.MemberDetailResponseDto;
import org.certis.studyplatform.member.presentation.dto.response.MemberSearchResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Member VO to DTO 변환을 담당하는 Mapper
 * Clean Architecture: Application Layer에서 Domain VO → Presentation DTO 변환
 */
@Component
public class MemberApplicationDtoMapper {

    // ================================================================
    // COMMAND RESPONSE MAPPING
    // ================================================================

    /**
     * MemberCreatedVo → MemberCreatedResponseDto 변환
     */
    public MemberCreatedResponseDto toMemberCreatedResponseDto(MemberCreatedVo vo) {
        return MemberCreatedResponseDto.builder()
                .id(vo.getId().value())
                .studentNumber(vo.getStudentNumber().value())
                .build();
    }

    /**
     * MemberUpdatedVo → MemberUpdatedResponseDto 변환
     */
    public MemberUpdatedResponseDto toMemberUpdatedResponseDto(MemberUpdatedVo vo) {
        return MemberUpdatedResponseDto.builder()
                .id(vo.getId().value())
                .name(vo.getName().value())
                .profileImage(vo.getProfileImageValue())
                .updatedAt(vo.getUpdatedAt())
                .build();
    }

    // ================================================================
    // QUERY RESPONSE MAPPING
    // ================================================================

    /**
     * MemberSummaryVo → MemberSummaryResponseDto 변환
     */
    public MemberSummaryResponseDto toMemberSummaryResponseDto(MemberSummaryVo vo) {
        return MemberSummaryResponseDto.builder()
                .id(vo.getId().value())
                .name(vo.getName().value())
                .grade(vo.getGrade().value())
                .role(vo.getRole().toString())
                .build();
    }

    /**
     * MemberVo → MemberDetailResponseDto 변환
     * 전체 회원 상세 정보를 포함하는 응답 DTO
     */
    public MemberDetailResponseDto toMemberDetailResponseDto(MemberVo vo) {
        return MemberDetailResponseDto.builder()
                .id(vo.getId())
                .name(vo.getName())
                .studentNumber(vo.getStudentNumber())
                .profileImage(vo.getProfileImage())
                .grade(vo.getGrade())
                .role(vo.getRole())
                .skills(vo.getSkills())
                .major(vo.getMajor())
                .description(vo.getDescription())
                .createdAt(vo.getCreatedAt())
                .updatedAt(vo.getUpdatedAt())
                .build();
    }

    // ================================================================
    // COLLECTION MAPPING
    // ================================================================

    /**
     * Page<MemberSummaryVo> → MemberSearchResponseDto 변환
     * 페이징 정보를 포함한 검색 결과 변환
     */
    public MemberSearchResponseDto toMemberSearchResponseDto(Page<MemberSummaryVo> memberPage) {
        Page<MemberSummaryResponseDto> dtoPage = memberPage.map(this::toMemberSummaryResponseDto);
        return MemberSearchResponseDto.of(dtoPage);
    }
}