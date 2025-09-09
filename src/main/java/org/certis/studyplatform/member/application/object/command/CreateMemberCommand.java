package org.certis.studyplatform.member.application.object.command;

import org.certis.studyplatform.auth.presentation.dto.request.RegisterRequestDto;
import org.certis.studyplatform.member.domain.MemberGrade;
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
        String name,                    // → NameVo (2-50자, 한글/영문/공백)
        String studentNumber,           // → StudentNumberVo (6-20자, 숫자만)
        MemberGrade grade,                   // → GradeVo (문자열 → MemberGrade 변환은 도메인에서 수행)
        MemberRole role,                    // → RoleVo (2-100자, 다국어)
        String major,                   // → MajorVo (2-100자, 특수문자 포함)
        String description,             // → String (선택적, 2000자 이하)
        List<String> skills,            // → SkillsVo (1-20개, 중복제거, 각 50자 이하)
        String email,                   // → EmailVo (선택적, 이메일 형식)
        String phoneNumber,
        String profileImage,             // → ProfileImageVo (선택적, URL 형식)
        OffsetDateTime birthday,
        String gender
) {
    /**
     * 생성 시점 기본 검증 (Domain 검증 전 빠른 실패)
     * - null 체크 등 기본적인 검증만 수행
     * - 상세한 비즈니스 검증은 VO 생성 시점에서 수행
     */
    public CreateMemberCommand {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("이름은 필수입니다");
        }
        if (studentNumber == null || studentNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("학번은 필수입니다");
        }
        if (grade == null ) {
            throw new IllegalArgumentException("학년은 필수입니다");
        }
        if (major == null || major.trim().isEmpty()) {
            throw new IllegalArgumentException("전공은 필수입니다");
        }
//        if (skills == null || skills.isEmpty()) { 회원가입시 skill은 null
//            throw new IllegalArgumentException("기술 스택은 최소 1개 이상 필요합니다");
//        }

        if (birthday == null) {
            throw new IllegalArgumentException("생년월일은 필수입니다");
        }
        if (gender == null || gender.trim().isEmpty()) {
            throw new IllegalArgumentException("성별은 필수입니다");
        }
    }

    public static CreateMemberCommand createMemberCommandForNewAuthMember(RegisterRequestDto requestDto) {
        return new CreateMemberCommand(
                requestDto.getName(),
                requestDto.getStudentNumber(),
                 MemberGrade.fromGradeString(requestDto.getGrade()),
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