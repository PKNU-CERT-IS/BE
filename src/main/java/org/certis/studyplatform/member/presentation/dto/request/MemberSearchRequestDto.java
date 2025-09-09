package org.certis.studyplatform.member.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.certis.studyplatform.member.domain.MemberRole;

import java.util.List;

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
    private String grade;

    /**
     * 역할 필터 (선택적)
     */
    private MemberRole role;

    /**
     * 검색 키워드 (선택적)
     * 이름, 전공, 기술 스택에서 검색
     */
    private String search;
}