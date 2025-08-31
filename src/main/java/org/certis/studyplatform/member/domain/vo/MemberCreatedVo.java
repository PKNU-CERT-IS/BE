package org.certis.studyplatform.member.domain.vo;

/**
 * 회원 생성 결과 Value Object
 *
 * 회원 생성 성공 후 반환되는 정보를 담는 불변 객체
 * 최소한의 정보만 포함하여 응답 크기 최적화
 */
public record MemberCreatedVo(
        MemberIdVo id,                    // 생성된 회원 ID
        StudentNumberVo studentNumber     // 학번 (확인용)
) {

    public MemberCreatedVo {
        if (id == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다");
        }
        if (studentNumber == null) {
            throw new IllegalArgumentException("학번은 필수입니다");
        }
    }

    /**
     * 정적 팩토리 메서드
     */
    public static MemberCreatedVo of(MemberIdVo id, StudentNumberVo studentNumber) {
        return new MemberCreatedVo(id, studentNumber);
    }

    /**
     * 정적 팩토리 메서드 (Long, String 값으로)
     */
    public static MemberCreatedVo of(Long id, String studentNumber) {
        return new MemberCreatedVo(MemberIdVo.of(id), new StudentNumberVo(studentNumber));
    }

    // =================================================================
    // Getter 메서드들
    // =================================================================

    /**
     * 회원 ID VO 반환
     */
    public MemberIdVo getId() {
        return id;
    }

    /**
     * 학번 VO 반환
     */
    public StudentNumberVo getStudentNumber() {
        return studentNumber;
    }

    /**
     * 생성된 회원 ID 반환
     */
    public Long getMemberId() {
        return id.value();
    }

    /**
     * 학번 문자열 반환
     */
    public String getStudentNumberValue() {
        return studentNumber.value();
    }
}