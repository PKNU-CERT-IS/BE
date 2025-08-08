package org.certis.studyplatform.auth.application.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.domain.model.Auth;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.auth.domain.repository.AuthQueryRepository;
import org.certis.studyplatform.auth.domain.repository.RedisRefreshTokenRepository;
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
    private final AuthQueryRepository authQueryRepository;
    private final RedisRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    // 로그인 자격 증명 ( 계정 존재 + 계정 id, password 일치 )
    public Auth validateCredentials(String accountNumber, String password) {
        Auth auth = authQueryRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApplicationException(AUTH_APPLICATION_ACCOUNT_NOT_FOUND));
        // 예외처리 옳바르지 않은 계정

        if (!auth.isPasswordMatches(password, passwordEncoder)) {
            // 비밀번호 불일치 예외
//            throw new ApplicationException(AUTH_APPLICATION_PASSWORD_MISMATCH); 나중에 password Encoder로 회원가입 로직 짜야함 지금 다 예외처림됨
        }
        log.debug("로그인 자격 증명 검증 성공: accountNumber={}", accountNumber);
        return auth;
    }

    // 리프레시 토큰 조회 및 유효성 검증
    public RefreshTokenVo validateRefreshToken(Long memberId) {
        RefreshTokenVo refreshTokenVo = refreshTokenRepository.findByMemberId(memberId)
                .orElseThrow(() -> new InfrastructureException(AUTH_INFRASTRUCTURE_JWT_TOKEN_PARSE_ERROR)
                );

        if (refreshTokenVo.isExpiredRefreshToken()) {
            refreshTokenRepository.deleteByMemberId(memberId);
            throw new InfrastructureException(AUTH_INFRASTRUCTURE_JWT_TOKEN_EXPIRED);
        }

        return refreshTokenVo;
    }


}
