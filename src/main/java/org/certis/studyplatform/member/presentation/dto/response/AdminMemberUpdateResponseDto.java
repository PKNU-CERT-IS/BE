package org.certis.studyplatform.member.presentation.dto.response;

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
public class AdminMemberUpdateResponseDto {
    private Long memberId;
    private MemberRole newRole;
    private MemberGrade newGrade;
}
