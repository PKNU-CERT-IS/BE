package org.certis.studyplatform.member.application.command;

public record GetMemberTokenInfoQuery(
        Long memberId  // 조회할 회원 ID
) {
    public static GetMemberTokenInfoQuery of(Long memberId) {
        return new GetMemberTokenInfoQuery(memberId);
    }
}
