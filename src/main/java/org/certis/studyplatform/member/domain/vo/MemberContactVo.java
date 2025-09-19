package org.certis.studyplatform.member.domain.vo;

public record MemberContactVo(
        MemberIdVo memberId,
        EmailVo email,
        PhoneNumberVo phoneNumber,
        GithubUrlVo githubUrl,
        LinkedinUrlVo linkedinUrl
) {
    public static MemberContactVo of(MemberIdVo memberId,
                                     EmailVo email,
                                     PhoneNumberVo phoneNumber,
                                     GithubUrlVo githubUrl,
                                     LinkedinUrlVo linkedinUrl)
    {
        return  new MemberContactVo(memberId,
                email,
                phoneNumber,
                githubUrl,
                linkedinUrl);
    }
}
