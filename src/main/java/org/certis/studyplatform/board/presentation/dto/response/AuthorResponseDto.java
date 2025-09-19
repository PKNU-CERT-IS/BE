package org.certis.studyplatform.board.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.member.domain.MemberRole;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthorResponseDto {

    private Long memberId;
    private String name;
    private MemberRole role;
}
