package org.certis.studyplatform.member.application.mapper;

import org.certis.studyplatform.member.domain.vo.MemberSearchForAdminVo;
import org.certis.studyplatform.member.presentation.dto.response.MemberDataForAdminResponseDto;
import org.springframework.stereotype.Component;

@Component
public class MemberAdminApplicationMapper {
    public MemberDataForAdminResponseDto convertToMemberDataForAdminResponseDto(MemberSearchForAdminVo vo) {
        return MemberDataForAdminResponseDto.builder()
                .memberId(vo.memberId().toLong())
                .name(vo.name())
                .role(vo.role().name())
                .major(vo.major())
                .studentNumber(vo.studentNumber())
                .activeStudies(vo.activeStudies())
                .activeProjects(vo.activeProjects())
                .penaltyPoints(vo.penaltyPoints())
                .gracePeriod(vo.gracePeriod())
                .grade(vo.grade())
                .birthday(vo.birthday() != null ? vo.birthday().toLocalDate().toString() : null)
                .phoneNumber(vo.phoneNumber())
                .email(vo.email())
                .createdAt(vo.createdAt())
                .build();
}
