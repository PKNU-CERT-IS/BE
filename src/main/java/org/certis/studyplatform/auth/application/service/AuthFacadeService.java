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
import org.certis.studyplatform.member.domain.vo.MemberCreatedVo;
import org.certis.studyplatform.member.domain.vo.MemberTokenInfoVo;
import org.certis.studyplatform.shared.security.JwtTokenProvider;
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
    private final JwtTokenProvider jwtTokenProvider;

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
    public RefreshAccessTokenResponseDto refreshAccessToken(String refreshToken) {
        log.info("안전한 토큰 갱신 시작");

        // 1. RefreshToken 검증 및 사용자 ID 추출
        Long tokenMemberId  = jwtTokenProvider.getUserIdFromToken(refreshToken);
        ValidateRefreshTokenQuery validateQuery = ValidateRefreshTokenQuery.of(tokenMemberId);
        RefreshTokenVo validationResult = authQueryService.validateRefreshToken(validateQuery);

        Long verifiedMemberId = validationResult.memberId();
        log.info("RefreshToken 검증 완료: memberId={}", verifiedMemberId);

        // 2. DB에서 최신 사용자 정보 조회
        GetMemberTokenInfoQuery memberQuery = GetMemberTokenInfoQuery.of(verifiedMemberId);
        MemberTokenInfoVo memberInfo = memberQueryService.getMemberTokenInfo(memberQuery);

        // 3. 새 AccessToken 생성 Command 생성
        RefreshTokenCommand command = RefreshTokenCommand.of(
                memberInfo.memberId(),
                memberInfo.studentNumber(),
                memberInfo.email(),
                memberInfo.name(),
                memberInfo.role()
        );

        // 4. 새 AccessToken 생성 (RefreshToken 로테이션 없음)
        AccessTokenVo newAccessToken = authCommandService.refreshAccessToken(command);

        log.info("안전한 토큰 갱신 완료: memberId={}, role={}", memberInfo.memberId(), memberInfo.role());
        return new RefreshAccessTokenResponseDto(newAccessToken.value());
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