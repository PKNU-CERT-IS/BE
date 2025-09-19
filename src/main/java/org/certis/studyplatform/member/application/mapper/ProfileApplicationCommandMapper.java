package org.certis.studyplatform.member.application.mapper;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.member.application.object.command.UpdateProfileCommand;
import org.certis.studyplatform.member.presentation.dto.request.ProfileUpdateRequestDto;
import org.springframework.stereotype.Component;

/**
 * Profile Application Command Mapper
 *
 * ✅ DTO → Command Object 변환 담당
 * ✅ Application Layer의 Profile Command 전용 매퍼
 * ✅ 네이밍 컨벤션: ProfileApplicationCommandMapper
 */
@Component
@RequiredArgsConstructor
public class ProfileApplicationCommandMapper {

    /**
     * ProfileUpdateRequestDto → UpdateProfileCommand 변환
     */
    public UpdateProfileCommand toUpdateProfileCommand(Long memberId, ProfileUpdateRequestDto requestDto) {
        return UpdateProfileCommand.builder()
                .memberId(memberId)
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .profileImage(requestDto.getProfileImage())
                .major(requestDto.getMajor())
                .birthday(requestDto.getBirthday())
                .phoneNumber(requestDto.getPhoneNumber())
                .studentNumber(requestDto.getStudentNumber())
                .skills(requestDto.getSkills())
                .grade(requestDto.getGrade())
                .email(requestDto.getEmail())
                .githubUrl(requestDto.getGithubUrl())
                .linkedinUrl(requestDto.getLinkedinUrl())
                .build();
    }
}