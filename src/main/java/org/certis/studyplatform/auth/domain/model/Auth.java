package org.certis.studyplatform.auth.domain.model;

import lombok.Builder;
import lombok.Getter;
import org.certis.studyplatform.auth.domain.model.vo.AccountNumberVo;
import org.certis.studyplatform.auth.domain.model.vo.EncodedPasswordVo;
import org.certis.studyplatform.member.domain.vo.RoleVo;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Getter
@Builder
public class Auth {
    private final Long memberId;
    private final AccountNumberVo accountNumberVo;
    private final EncodedPasswordVo encodedPasswordVo;
    private final RoleVo roleVo;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;


    public static Auth createAuthData(Long memberId,
                                      String accountNumber,
                                      String encodedPassword,
                                      RoleVo roleVo) {
        return Auth.builder()
                .memberId(memberId)
                .accountNumberVo(AccountNumberVo.of(accountNumber))
                .encodedPasswordVo(EncodedPasswordVo.of(encodedPassword))
                .roleVo(roleVo)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deletedAt(null)
                .build();
    }

    // 비밀번호검증
    public boolean isPasswordMatches(String password, PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(password, this.encodedPasswordVo.encodedPassword());
    }
}


