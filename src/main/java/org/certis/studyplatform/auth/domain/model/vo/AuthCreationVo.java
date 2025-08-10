package org.certis.studyplatform.auth.domain.model.vo;

public record AuthCreationVo(
        Long memberId,
        AccountNumberVo accountNumber,
        EncodedPasswordVo encodedPassword
) {
    public static AuthCreationVo  of( Long memberId,
                                     AccountNumberVo accountNumber,
                                     EncodedPasswordVo encodedPassword){
        return new AuthCreationVo(memberId,accountNumber,encodedPassword);
    }
}