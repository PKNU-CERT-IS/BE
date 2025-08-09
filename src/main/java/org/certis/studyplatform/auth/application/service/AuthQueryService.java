package org.certis.studyplatform.auth.application.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.application.object.query.ValidateCredentialsQuery;
import org.certis.studyplatform.auth.application.object.query.ValidateRefreshTokenQuery;
import org.certis.studyplatform.auth.domain.model.vo.AuthInfoVo;
import org.certis.studyplatform.auth.domain.model.vo.RawPasswordVo;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.auth.domain.service.AuthDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 읽기 전용
public class AuthQueryService {
    private final AuthDomainService authDomainService;

    // 로그인 자격 증명 ( 계정 존재 + 계정 id, password 일치 )
    public AuthInfoVo validateCredentials(ValidateCredentialsQuery validateCredentialsQuery) {
        AuthInfoVo auth = authDomainService.findAuthByAccountNumber(validateCredentialsQuery);

        RawPasswordVo rawPasswordVo = RawPasswordVo.of(validateCredentialsQuery.rawPassword());

        authDomainService.validatePassword(auth,rawPasswordVo);

        log.debug("로그인 자격 증명 검증 성공: accountNumber={}", validateCredentialsQuery.accountNumber());
        return auth;
    }

    // 리프레시 토큰 조회 및 유효성 검증
    public RefreshTokenVo validateRefreshToken(ValidateRefreshTokenQuery validateRefreshTokenQuery) {
       RefreshTokenVo refreshTokenVo= authDomainService.validateRefreshToken(validateRefreshTokenQuery);

        return refreshTokenVo;
    }


}
