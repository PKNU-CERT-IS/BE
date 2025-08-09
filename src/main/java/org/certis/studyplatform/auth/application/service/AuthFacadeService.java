package org.certis.studyplatform.auth.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.application.object.command.GenerateTokenCommand;
import org.certis.studyplatform.auth.application.object.command.LogoutCommand;
import org.certis.studyplatform.auth.application.object.command.RefreshTokenCommand;
import org.certis.studyplatform.auth.application.object.query.ValidateCredentialsQuery;
import org.certis.studyplatform.auth.application.object.query.ValidateRefreshTokenQuery;
import org.certis.studyplatform.auth.domain.model.vo.AccessTokenVo;
import org.certis.studyplatform.auth.domain.model.vo.AuthInfoVo;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.auth.domain.model.vo.TokenInfoVo;
import org.certis.studyplatform.auth.presentation.dto.request.LoginRequestDto;
import org.certis.studyplatform.auth.presentation.dto.response.RefreshAccessTokenResponseDto;
import org.certis.studyplatform.auth.presentation.dto.response.TokenRequestDto;
import org.certis.studyplatform.member.domain.MemberRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacadeService {

    private final AuthQueryService authQueryService;
    private final AuthCommandService authCommandService;

   // 로그인
    @Transactional
    public TokenRequestDto login(LoginRequestDto requestDto) {
        log.info("로그인 시도: accountNumber={}", requestDto.getAccountNumber());

        ValidateCredentialsQuery validateCredentialsQuery =
                ValidateCredentialsQuery.of(requestDto.getAccountNumber(),requestDto.getPassword());

        AuthInfoVo authInfoVo = authQueryService.validateCredentials(validateCredentialsQuery);

        GenerateTokenCommand generateTokenCommand =
                GenerateTokenCommand.of(authInfoVo.memberId(),authInfoVo.role());

        // 2. 토큰 생성 및 Redis 저장
        TokenInfoVo tokenInfoVo = authCommandService.executeLogin(generateTokenCommand);

        log.info("로그인 성공: memberId={}",authInfoVo.memberId());

        // 이후 매핑
        TokenRequestDto tokenRequestDto = new TokenRequestDto(tokenInfoVo.accessToken(),
                tokenInfoVo.refreshToken(),
                tokenInfoVo.memberId(),
                tokenInfoVo.refreshExpiredAt(),
                tokenInfoVo.role());

        return tokenRequestDto;
    }

    // 로그아웃
    @Transactional
    public void logout(Long memberId) {
        log.info("로그아웃 시도: memberId={}", memberId);

        LogoutCommand logoutCommand = LogoutCommand.of(memberId);

        // 레디스에서 토큰 정보 삭제
        authCommandService.executeLogout(logoutCommand);

        log.info("로그아웃 성공: memberId={}", memberId);
    }

   // accessToken 갱신
    @Transactional
    public RefreshAccessTokenResponseDto refreshAccessToken(Long memberId, MemberRole currentRole) {
        log.info("토큰 갱신 시도: memberId={}", memberId);

        ValidateRefreshTokenQuery validateRefreshTokenQuery = ValidateRefreshTokenQuery.of(memberId);

        // 1. RefreshToken 검증
        RefreshTokenVo refreshToken = authQueryService.validateRefreshToken(validateRefreshTokenQuery);

        RefreshTokenCommand refreshTokenCommand  = RefreshTokenCommand.of(memberId,currentRole);

        // 2. 새 AccessToken 생성
        AccessTokenVo newAccessToken = authCommandService.refreshAccessToken(refreshTokenCommand);

        RefreshAccessTokenResponseDto responseDto = new RefreshAccessTokenResponseDto(newAccessToken.value());
        log.info("토큰 갱신 성공: memberId={}", memberId);
        return responseDto;
    }

}