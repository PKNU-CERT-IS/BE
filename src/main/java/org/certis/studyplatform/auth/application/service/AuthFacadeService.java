package org.certis.studyplatform.auth.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.application.object.command.CreateAuthCommand;
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
import org.certis.studyplatform.auth.presentation.dto.request.RegisterRequestDto;
import org.certis.studyplatform.auth.presentation.dto.response.RefreshAccessTokenResponseDto;
import org.certis.studyplatform.auth.presentation.dto.response.TokenRequestDto;
import org.certis.studyplatform.member.application.command.GetMemberTokenInfoQuery;
import org.certis.studyplatform.member.application.command.MemberCommandService;
import org.certis.studyplatform.member.application.object.command.CreateMemberCommand;
import org.certis.studyplatform.member.application.query.MemberQueryService;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.MemberCreatedVo;
import org.certis.studyplatform.member.domain.vo.MemberTokenInfoVo;
import org.certis.studyplatform.member.domain.vo.MemberVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacadeService {

    private final AuthQueryService authQueryService;
    private final AuthCommandService authCommandService;
    private final MemberCommandService memberCommandService;
    private final MemberQueryService  memberQueryService;

   // 로그인
    @Transactional
    public TokenRequestDto login(LoginRequestDto requestDto) {
        log.info("로그인 시도: accountNumber={}", requestDto.getAccountNumber());

        ValidateCredentialsQuery validateCredentialsQuery =
                ValidateCredentialsQuery.of(requestDto.getAccountNumber(),requestDto.getPassword());

        AuthInfoVo authInfoVo = authQueryService.validateCredentials(validateCredentialsQuery);

        GetMemberTokenInfoQuery getMemberTokenInfoQuery = GetMemberTokenInfoQuery.of(authInfoVo.memberId());

        MemberTokenInfoVo infoVo = memberQueryService.getMemberTokenInfo(getMemberTokenInfoQuery);

        GenerateTokenCommand command =
                GenerateTokenCommand.of(
                        infoVo.memberId(),
                        infoVo.studentNumber(),
                        infoVo.email(),
                        infoVo.name(),
                        infoVo.role()
                );

        // 2. 토큰 생성 및 Redis 저장
        TokenInfoVo tokenInfoVo = authCommandService.executeLogin(command);

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
    public RefreshAccessTokenResponseDto refreshAccessToken(Long memberId,String username,String name,String email,MemberRole currentRole) {
        log.info("토큰 갱신 시도: memberId={}", memberId);

        ValidateRefreshTokenQuery validateRefreshTokenQuery = ValidateRefreshTokenQuery.of(memberId);

        // 1. RefreshToken 검증
        RefreshTokenVo refreshToken = authQueryService.validateRefreshToken(validateRefreshTokenQuery);

        RefreshTokenCommand refreshTokenCommand  = RefreshTokenCommand.of(memberId,username,email,name,currentRole);

        // 2. 새 AccessToken 생성
        AccessTokenVo newAccessToken = authCommandService.refreshAccessToken(refreshTokenCommand);

        RefreshAccessTokenResponseDto responseDto = new RefreshAccessTokenResponseDto(newAccessToken.value());
        log.info("토큰 갱신 성공: memberId={}", memberId);
        return responseDto;
    }

    @Transactional
    public void register(RegisterRequestDto requestDto) {
        // 1. DTO → Command 변환
        CreateMemberCommand createMemberCommand = CreateMemberCommand.createMemberCommandForNewAuthMember(requestDto);

        // 2. Member 생성 → ID 획득
        MemberCreatedVo member = memberCommandService.createMember(createMemberCommand);

        // 3. Auth 생성 (Member ID 포함)
        CreateAuthCommand authCmdWithId = CreateAuthCommand.of(
                member.id().value(), requestDto.getAccountNumber(), requestDto.getPassword()
        );
        authCommandService.createAuth(authCmdWithId);
    }

}