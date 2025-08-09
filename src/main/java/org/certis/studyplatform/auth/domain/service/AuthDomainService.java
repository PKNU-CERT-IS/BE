package org.certis.studyplatform.auth.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.domain.model.Auth;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.auth.domain.repository.AuthQueryRepository;
import org.certis.studyplatform.auth.domain.repository.RedisRefreshTokenRepository;
import org.certis.studyplatform.exception.ApplicationException;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.InfrastructureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.certis.studyplatform.exception.ExceptionStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthDomainService {

    private final RedisRefreshTokenRepository refreshTokenRepository;
    private final AuthQueryRepository authQueryRepository;
    private final PasswordEncoder passwordEncoder;

    public void saveRefreshToken(RefreshTokenVo refreshTokenVo) {
        // 토큰 만료 검증
        if (refreshTokenVo.isExpiredRefreshToken()) {
            log.warn("만료된 리프레시 토큰 저장 시도 방지: memberId={}, expiredAt={}",
                    refreshTokenVo.memberId(),
                    refreshTokenVo.expiredAt());
            throw new DomainException(AUTH_DOMAIN_JWT_TOKEN_EXPIRED);
        }

        Duration ttl = Duration.between(LocalDateTime.now(),refreshTokenVo.expiredAt());

        refreshTokenRepository.save(refreshTokenVo,ttl);
        log.info("RefreshToken 저장 완료: memberId={}", refreshTokenVo.memberId());
    }

    public RefreshTokenVo validateRefreshToken(Long memberId){
        RefreshTokenVo refreshTokenVo = refreshTokenRepository.findByMemberId(memberId)
                .orElseThrow(() -> new InfrastructureException(AUTH_DOMAIN_JWT_TOKEN_PARSE_ERROR)
                );

        if (refreshTokenVo.isExpiredRefreshToken()) {
            deleteRefreshToken(memberId);
            throw new DomainException(AUTH_DOMAIN_JWT_TOKEN_EXPIRED);
        }

        return refreshTokenVo;
    }

    public void deleteRefreshToken(Long memberId){
        refreshTokenRepository.deleteByMemberId(memberId);
    }

    public Auth findAuthByAccountNumber(String accountNumber){
        return authQueryRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApplicationException(AUTH_DOMAIN_ACCOUNT_NOT_FOUND));
    }

    public void validatePassword(Auth auth,String password) {
        if (!auth.isPasswordMatches(password, passwordEncoder)) {
            // 비밀번호 불일치 예외
//            throw new ApplicationException(AUTH_APPLICATION_PASSWORD_MISMATCH); 나중에 password Encoder로 회원가입 로직 짜야함 지금 다 예외처림됨
        }
    }

}
