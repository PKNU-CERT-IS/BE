package org.certis.studyplatform.member.application.mapper;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.member.application.object.command.CreateMemberCommand;
import org.certis.studyplatform.member.application.object.command.DeleteMemberCommand;
import org.certis.studyplatform.member.application.object.command.UpdateMemberCommand;
import org.certis.studyplatform.member.presentation.dto.request.MemberCreateRequestDto;
import org.certis.studyplatform.member.presentation.dto.request.MemberUpdateRequestDto;
import org.springframework.stereotype.Component;

/**
 * Member Application Command Mapper
 *
 * ✅ DTO → Command Object 변환 담당
 * ✅ Application Layer의 Command 전용 매퍼
 * ✅ 네이밍 컨벤션: MemberApplicationCommandMapper
 */
@Component
@RequiredArgsConstructor
public class MemberApplicationCommandMapper {

    /**
     * DTO → CreateMemberCommand 변환
     */
    public CreateMemberCommand toCreateMemberCommand(MemberCreateRequestDto requestDto) {
        return new CreateMemberCommand(
            requestDto.getName(),
            requestDto.getStudentNumber(),
            requestDto.getGrade(),
            requestDto.getRole(),
            requestDto.getMajor(),
            requestDto.getDescription(),
            requestDto.getSkills(),
            requestDto.getEmail(),
            requestDto.getProfileImage(),
                requestDto.getBirthday(),
                requestDto.getGender()
        );
    }

    /**
     * DTO → MemberUpdateCommand 변환 (기본 정보 수정)
     */
    public UpdateMemberCommand toMemberUpdateCommand(Long memberId, MemberUpdateRequestDto requestDto) {
        return new UpdateMemberCommand(
            memberId,
            requestDto.getName(),
            null, // 기본 정보 수정에서는 프로필 이미지 제외
            requestDto.getGrade(),
            requestDto.getRole(),
            requestDto.getMajor(),
            requestDto.getDescription(),
            requestDto.getSkills()
        );
    }

    /**
     * DTO → DeleteMemberCommand 변환
     */
    public DeleteMemberCommand toDeleteMemberCommand(Long memberId) {
        return new DeleteMemberCommand(memberId);
    }
}
