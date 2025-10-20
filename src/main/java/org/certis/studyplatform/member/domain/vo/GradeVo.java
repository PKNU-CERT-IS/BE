package org.certis.studyplatform.member.domain.vo;

import jakarta.persistence.Embeddable;
import org.certis.studyplatform.member.domain.MemberGrade;

/**
 * 학년 Value Object
 *
 * 비즈니스 규칙 검증 포함 - 도메인 요구사항 충족
 * 유효한 학년 값 관리
 */
@Embeddable
public record GradeVo(MemberGrade grade) {

    public static GradeVo of(MemberGrade grade) {
        return new GradeVo(grade);
    }

}
