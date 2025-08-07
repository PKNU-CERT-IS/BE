package org.certis.studyplatform.auth.application.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.domain.model.Auth;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.auth.domain.repository.AuthQueryRepository;
import org.certis.studyplatform.auth.domain.repository.RedisRefreshTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 읽기 전용
public class AuthQueryService {
    private final AuthQueryRepository authQueryRepository;
    private final RedisRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    // 로그인 자격 증명 ( 계정 존재 + 계정 id, password 일치 )
    public Auth validateCredentials(String accountNumber, String passwword) {
        Auth auth = authQueryRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException()); // 예외처리 옳바르지 않은 계정

        if (!auth.isPasswordMatches(passwword, passwordEncoder)) {
            // 비밀번호 불일치 예외
        }

        log.debug("로그인 자격 증명 검증 성공: accountNumber={}", accountNumber);
        return auth;
    }

    // 리프레시 토큰 조회 및 유효성 검증
    public RefreshTokenVo validateRefreshToken(Long memberId) {
        RefreshTokenVo refreshTokenVo = refreshTokenRepository.findByMemberId(memberId)
                .orElseThrow(() -> new RuntimeException() // 리프레시토큰을 찾을수 없습니다.
                );

        if (refreshTokenVo.isExpiredRefreshToken()) {
            // 만료된 토큰은 Redis에서 삭제
            refreshTokenRepository.deleteByMemberId(memberId);
            // 만료 예외 처리
        }

        return refreshTokenVo;
    }
}
