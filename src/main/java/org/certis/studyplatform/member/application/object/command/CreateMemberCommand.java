package org.certis.studyplatform.member.application.object.command;

import org.certis.studyplatform.auth.presentation.dto.request.RegisterRequestDto;
import org.certis.studyplatform.member.domain.MemberRole;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Create Member Command
 *
 * Application Layer → Domain Service로 전달되는 Command Object
 *
 * 특징:
 * - Primitive/Reference Type으로 구성 (VO 변환 전)
 * - Domain Service에서 VO로 변환하여 비즈니스 검증 수행
 * - 불변 Record 구조로 데이터 무결성 보장
 *
 * 데이터 흐름:
 * 1. Controller → Facade Service (DTO → Command 변환)
 * 2. Facade → Command Service (Command 그대로 전달)
 * 3. Command Service → Domain Service (Command 그대로 전달)
 * 4. Domain Service에서 Command → VO 변환 (비즈니스 검증 수행)
 */
public record CreateMemberCommand(
        String name,               // 필수
        String studentNumber,      // 필수
        String grade,              // 필수
        MemberRole role,           // 필수
        String major,              // 필수
        String description,        // 선택
        List<String> skills,       // 선택
        String email,              // 필수 (ERD에서 NOT NULL)
        String phoneNumber,        // 필수 (ERD에서 NOT NULL)
        String profileImage,       // 선택
        OffsetDateTime birthday,   // 필수
        String gender              // 필수
) {

    public static CreateMemberCommand createMemberCommandForNewAuthMember(RegisterRequestDto requestDto) {
        return new CreateMemberCommand(
                requestDto.getName(),
                requestDto.getStudentNumber(),
                requestDto.getGrade(),
                MemberRole.NONE,                // 회원가입 시 기본 Role (추후 승인되면 변경)
                requestDto.getMajor(),
                null,                           // description (선택값)
                null,                           // skills (회원가입 시점엔 선택적)
                requestDto.getEmail(),          // ✅ email (필수)
                requestDto.getPhoneNumber(),    // ✅ phoneNumber (필수)
                null,                           // profileImage (선택값)
                requestDto.getBirthday(),
                requestDto.getGender()
        );
    }
}