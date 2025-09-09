package org.certis.studyplatform.member.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.domain.MemberRole;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMemberUpdateRequestDto {

    @NotNull(message = "대상 회원 ID는 필수입니다")
    private Long targetMemberId;

    @NotNull(message = "대상 회원 역할 정보는 필수입니다")
    private MemberRole newRole;

    @NotNull(message = "대상 회원 학년 정보는 필수입니다")
    private MemberGrade newGrade;
}
