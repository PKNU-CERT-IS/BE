package org.certis.studyplatform.auth.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.application.object.command.CreateAuthCommand;
import org.certis.studyplatform.auth.application.object.command.LogoutCommand;
import org.certis.studyplatform.auth.application.object.query.ValidateCredentialsQuery;
import org.certis.studyplatform.auth.application.object.query.ValidateRefreshTokenQuery;
import org.certis.studyplatform.auth.domain.model.vo.*;
import org.certis.studyplatform.auth.domain.repository.AuthCommandRepository;
import org.certis.studyplatform.auth.domain.repository.AuthQueryRepository;
import org.certis.studyplatform.auth.domain.repository.RedisRefreshTokenRepository;
import org.certis.studyplatform.exception.ApplicationException;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.exception.InfrastructureException;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.MemberIdVo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthDomainService {

    private final RedisRefreshTokenRepository refreshTokenRepository;
    private final AuthQueryRepository authQueryRepository;
    private final AuthCommandRepository authCommandRepository;
    private final PasswordEncoder passwordEncoder;

    public void saveRefreshToken(RefreshTokenVo refreshTokenVo) {
        // 토큰 만료 검증
        if (refreshTokenVo.isExpiredRefreshToken()) {
            log.warn("만료된 리프레시 토큰 저장 시도 방지: memberId={}, expiredAt={}",
                    refreshTokenVo.memberId(),
                    refreshTokenVo.expiredAt());
            throw new DomainException(ExceptionStatus.AUTH_DOMAIN_JWT_TOKEN_EXPIRED);
        }

        Duration ttl = Duration.between(LocalDateTime.now(),refreshTokenVo.expiredAt());

        refreshTokenRepository.save(refreshTokenVo,ttl);
        log.info("RefreshToken 저장 완료: memberId={}", refreshTokenVo.memberId());
    }

    public RefreshTokenVo validateRefreshToken(ValidateRefreshTokenQuery validateRefreshTokenQuery){
        MemberIdVo memberIdVo = MemberIdVo.of(validateRefreshTokenQuery.memberId());

        RefreshTokenVo refreshTokenVo = refreshTokenRepository.findByMemberId(memberIdVo)
                .orElseThrow(() -> new InfrastructureException(ExceptionStatus.AUTH_DOMAIN_JWT_TOKEN_PARSE_ERROR)
                );

        // 이럴수가
        LogoutCommand logoutCommand = LogoutCommand.of(validateRefreshTokenQuery.memberId());

        if (refreshTokenVo.isExpiredRefreshToken()) {
            deleteRefreshToken(logoutCommand);
            throw new DomainException(ExceptionStatus.AUTH_DOMAIN_JWT_TOKEN_EXPIRED);
        }

        return refreshTokenVo;
    }

    public void deleteRefreshToken(LogoutCommand logoutCommand){
        MemberIdVo memberIdVo = MemberIdVo.of(logoutCommand.memberId());

        refreshTokenRepository.deleteByMemberId(memberIdVo);
    }

    public AuthInfoVo findAuthByAccountNumber(ValidateCredentialsQuery validateCredentialsQuery){
        AccountNumberVo accountNumberVo = AccountNumberVo.of(validateCredentialsQuery.accountNumber());

        // 계정이 존재하지 않을 시 예외 처리
        AuthInfoVo authInfoVo =  authQueryRepository.findByAccountNumber(accountNumberVo)
                .orElseThrow(() -> new ApplicationException(ExceptionStatus.AUTH_DOMAIN_ACCOUNT_NOT_FOUND));

        // 계정의 상태가 NONE 상태일 시 회원 로그인 차단
        if(authInfoVo.role() == MemberRole.NONE){
            throw new DomainException(ExceptionStatus.AUTH_DOMAIN_ACCOUNT_NOT_APPROVED);
        }

        return  authInfoVo;
    }

    public void validatePassword(AuthInfoVo authInfoVo, RawPasswordVo rawPasswordVo) {
        if (!authInfoVo.isPasswordMatches(rawPasswordVo.value(), passwordEncoder)) {
            // 비밀번호 불일치 예외
            throw new ApplicationException(ExceptionStatus.AUTH_APPLICATION_PASSWORD_MISMATCH);
        }
    }

    public void createAuth(CreateAuthCommand createAuthCommand){
        AccountNumberVo accountNumberVo = AccountNumberVo.of(createAuthCommand.accountNumber());
        RawPasswordVo rawPasswordVo = RawPasswordVo.of(createAuthCommand.password());


        // 계정번호 중복 체크
        validateAccountNumberDuplication(accountNumberVo);
        log.debug("✅ Account number duplication check passed");


        // 비밀번호 암호화
        log.debug("🔐 Encoding password...");
        EncodedPasswordVo encodedPasswordVo = encodePassword(rawPasswordVo);
        log.debug("✅ Password encoded successfully");



        log.debug("🔗 Creating AuthCreationVo...");
        AuthCreationVo authCreationVo = AuthCreationVo.of(
                createAuthCommand.memberId(),
                accountNumberVo,
                encodedPasswordVo
        );
        log.debug("✅ AuthCreationVo created successfully");

        // 회원가입 정보 저장
        authCommandRepository.save(authCreationVo);
    }


    private void validateAccountNumberDuplication(AccountNumberVo accountNumberVo) {
        if (authQueryRepository.existsByAccountNumber(accountNumberVo)) {
            log.warn("❌ Duplicate account number detected: {}", accountNumberVo.accountNumber());
            throw new DomainException(ExceptionStatus.AUTH_DOMAIN_DUPLICATE_ACCOUNT_NUMBER,
                    "이미 존재하는 계정번호입니다: " + accountNumberVo.accountNumber());
        }
    }

    // 비밀번호 암호화
    private EncodedPasswordVo encodePassword(RawPasswordVo rawPasswordVo) {
        String encodedPassword = passwordEncoder.encode(rawPasswordVo.value());
        return EncodedPasswordVo.of(encodedPassword);
    }
}
