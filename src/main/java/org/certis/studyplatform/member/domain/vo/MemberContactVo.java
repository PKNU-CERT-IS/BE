package org.certis.studyplatform.member.domain.vo;

public record MemberContactVo(
        MemberIdVo memberId,
        EmailVo email,
        PhoneNumberVo phoneNumber
) {
    public static MemberContactVo of(MemberIdVo memberId,
                                     EmailVo email,
                                     PhoneNumberVo phoneNumber)
    {
        return  new MemberContactVo(memberId,
                email,
                phoneNumber);
    }
}
