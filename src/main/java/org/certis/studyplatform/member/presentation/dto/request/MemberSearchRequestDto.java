package org.certis.studyplatform.member.presentation.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.domain.MemberRole;


/**
 * 회원 검색 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberSearchRequestDto {
    /**
     * 학년 필터 (선택적)
     */
    private MemberGrade grade;

    /**
     * 역할 필터 (선택적)
     */
    private MemberRole role;

    /**
     * 검색 키워드 (선택적)
     * 이름, 전공, 기술 스택에서 검색
     */
    private String keyword;

    /**
     * 페이지 번호 (기본값 0)
     */
    @Min(0)
    private int page = 0;

    /**
     * 페이지 크기 (기본값 10)
     */
    @Min(1)
    private int size = 10;
}