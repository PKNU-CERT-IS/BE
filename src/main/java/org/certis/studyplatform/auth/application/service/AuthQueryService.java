package org.certis.studyplatform.auth.application.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.domain.model.Auth;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.auth.domain.repository.AuthQueryRepository;
import org.certis.studyplatform.auth.domain.repository.RedisRefreshTokenRepository;
import org.certis.studyplatform.auth.domain.service.AuthDomainService;
import org.certis.studyplatform.exception.ApplicationException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.exception.InfrastructureException;
import org.hibernate.service.spi.ServiceException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.certis.studyplatform.exception.ExceptionStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 읽기 전용
public class AuthQueryService {
    private final RedisRefreshTokenRepository refreshTokenRepository;
    private final AuthDomainService authDomainService;

    // 로그인 자격 증명 ( 계정 존재 + 계정 id, password 일치 )
    public Auth validateCredentials(String accountNumber, String password) {
        Auth auth = authDomainService.findAuthByAccountNumber(accountNumber);

        authDomainService.validatePassword(auth,password);

        log.debug("로그인 자격 증명 검증 성공: accountNumber={}", accountNumber);
        return auth;
    }

    // 리프레시 토큰 조회 및 유효성 검증
    public RefreshTokenVo validateRefreshToken(Long memberId) {
       RefreshTokenVo refreshTokenVo= authDomainService.validateRefreshToken(memberId);

        return refreshTokenVo;
    }


}
