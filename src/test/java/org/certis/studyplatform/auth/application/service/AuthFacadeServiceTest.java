package org.certis.studyplatform.auth.application.service;

import org.certis.studyplatform.auth.application.object.command.RefreshTokenCommand;
import org.certis.studyplatform.auth.application.object.query.ValidateRefreshTokenQuery;
import org.certis.studyplatform.auth.domain.model.vo.AccessTokenVo;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.auth.domain.repository.RedisRefreshTokenRepository;
import org.certis.studyplatform.auth.presentation.dto.response.RefreshAccessTokenResponseDto;
import org.certis.studyplatform.member.application.command.GetMemberTokenInfoQuery;
import org.certis.studyplatform.member.application.query.MemberQueryService;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.MemberTokenInfoVo;
import org.certis.studyplatform.shared.security.JwtTokenProvider;
import org.certis.studyplatform.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthFacadeService 토큰 갱신 로직 테스트
 * - 토큰 로테이션 검증
 * - Redis 트래픽 최적화 검증
 * - 에러 처리 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("🔐 AuthFacadeService 토큰 갱신 테스트")
class AuthFacadeServiceTest {

    @Mock
    private AuthQueryService authQueryService;
    
    @Mock
    private AuthCommandService authCommandService;
    
    @Mock
    private MemberQueryService memberQueryService;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private RedisRefreshTokenRepository redisRefreshTokenRepository;

    @InjectMocks
    private AuthFacadeService authFacadeService;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final String TEST_STUDENT_NUMBER = "20201234";
    private static final String TEST_EMAIL = "test@certis.org";
    private static final String TEST_NAME = "김테스트";
    private static final MemberRole TEST_ROLE = MemberRole.PLAYER;
    private static final String TEST_REFRESH_TOKEN = "test.refresh.token";
    private static final String TEST_NEW_ACCESS_TOKEN = "new.access.token";

    private RefreshTokenVo mockRefreshToken;
    private AccessTokenVo mockNewAccessToken;
    private MemberTokenInfoVo mockMemberInfo;

    @BeforeEach
    void setUp() {
        // Mock 데이터 설정
        mockRefreshToken = new RefreshTokenVo(
                TEST_REFRESH_TOKEN,
                LocalDateTime.now().plusDays(1),
                TEST_MEMBER_ID
        );


        mockNewAccessToken = new AccessTokenVo(
                TEST_NEW_ACCESS_TOKEN,
                LocalDateTime.now().plusHours(1)
        );

        mockMemberInfo = new MemberTokenInfoVo(
                TEST_MEMBER_ID,
                TEST_STUDENT_NUMBER,
                TEST_EMAIL,
                TEST_NAME,
                TEST_ROLE
        );
    }

    @Test
    @DisplayName("토큰 갱신 성공 - AccessToken만 재발급 (로테이션 없음)")
    void refreshAccessToken_Success_TokenRotation() {
        // Given
        when(jwtTokenProvider.getUserIdFromToken(TEST_REFRESH_TOKEN)).thenReturn(TEST_MEMBER_ID);
        when(authQueryService.validateRefreshToken(any(ValidateRefreshTokenQuery.class)))
                .thenReturn(mockRefreshToken);
        when(memberQueryService.getMemberTokenInfo(any(GetMemberTokenInfoQuery.class)))
                .thenReturn(mockMemberInfo);
        when(authCommandService.refreshAccessToken(any(RefreshTokenCommand.class)))
                .thenReturn(mockNewAccessToken);

        // When
        RefreshAccessTokenResponseDto result = authFacadeService.refreshAccessToken(TEST_REFRESH_TOKEN);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(TEST_NEW_ACCESS_TOKEN);

        // AccessToken만 재발급, RefreshToken 로테이션/재조회 없음
        verify(authCommandService, times(1)).refreshAccessToken(any(RefreshTokenCommand.class));
        verify(authQueryService, times(1)).validateRefreshToken(any(ValidateRefreshTokenQuery.class));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 만료된 RefreshToken")
    void refreshAccessToken_Failure_ExpiredRefreshToken() {
        // Given
        when(jwtTokenProvider.getUserIdFromToken(TEST_REFRESH_TOKEN)).thenReturn(TEST_MEMBER_ID);
        when(authQueryService.validateRefreshToken(any(ValidateRefreshTokenQuery.class)))
                .thenThrow(new RuntimeException("만료된 토큰"));

        // When & Then
        assertThatThrownBy(() -> authFacadeService.refreshAccessToken(TEST_REFRESH_TOKEN))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("토큰 갱신에 실패했습니다.");

        // 토큰 갱신이 호출되지 않았는지 확인
        verify(authCommandService, never()).refreshAccessToken(any(RefreshTokenCommand.class));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 잘못된 RefreshToken")
    void refreshAccessToken_Failure_InvalidRefreshToken() {
        // Given
        when(jwtTokenProvider.getUserIdFromToken(TEST_REFRESH_TOKEN))
                .thenThrow(new RuntimeException("잘못된 토큰"));

        // When & Then
        assertThatThrownBy(() -> authFacadeService.refreshAccessToken(TEST_REFRESH_TOKEN))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("토큰 갱신에 실패했습니다.");

        // 토큰 검증이 호출되지 않았는지 확인
        verify(authQueryService, never()).validateRefreshToken(any(ValidateRefreshTokenQuery.class));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 사용자 정보 조회 실패")
    void refreshAccessToken_Failure_MemberInfoNotFound() {
        // Given
        when(jwtTokenProvider.getUserIdFromToken(TEST_REFRESH_TOKEN)).thenReturn(TEST_MEMBER_ID);
        when(authQueryService.validateRefreshToken(any(ValidateRefreshTokenQuery.class)))
                .thenReturn(mockRefreshToken);
        when(memberQueryService.getMemberTokenInfo(any(GetMemberTokenInfoQuery.class)))
                .thenThrow(new RuntimeException("사용자 정보를 찾을 수 없습니다"));

        // When & Then
        assertThatThrownBy(() -> authFacadeService.refreshAccessToken(TEST_REFRESH_TOKEN))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("토큰 갱신에 실패했습니다.");

        // 토큰 갱신이 호출되지 않았는지 확인
        verify(authCommandService, never()).refreshAccessToken(any(RefreshTokenCommand.class));
    }

    // =================================================================
    // JWT 예외 처리 테스트
    // =================================================================

    @Test
    @DisplayName("JWT 토큰 만료 예외 처리 테스트")
    void refreshAccessToken_Failure_JwtExpiredException() {
        // Given
        when(jwtTokenProvider.getUserIdFromToken(TEST_REFRESH_TOKEN))
                .thenThrow(new io.jsonwebtoken.ExpiredJwtException(null, null, "JWT expired"));

        // When & Then
        assertThatThrownBy(() -> authFacadeService.refreshAccessToken(TEST_REFRESH_TOKEN))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("토큰 갱신에 실패했습니다");

        // 토큰 검증이 호출되지 않았는지 확인
        verify(authQueryService, never()).validateRefreshToken(any(ValidateRefreshTokenQuery.class));
        verify(authCommandService, never()).refreshAccessToken(any(RefreshTokenCommand.class));
    }

    @Test
    @DisplayName("JWT 토큰 형식 오류 예외 처리 테스트")
    void refreshAccessToken_Failure_MalformedJwtException() {
        // Given
        when(jwtTokenProvider.getUserIdFromToken(TEST_REFRESH_TOKEN))
                .thenThrow(new io.jsonwebtoken.MalformedJwtException("Malformed JWT token"));

        // When & Then
        assertThatThrownBy(() -> authFacadeService.refreshAccessToken(TEST_REFRESH_TOKEN))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("토큰 갱신에 실패했습니다");

        // 토큰 검증이 호출되지 않았는지 확인
        verify(authQueryService, never()).validateRefreshToken(any(ValidateRefreshTokenQuery.class));
        verify(authCommandService, never()).refreshAccessToken(any(RefreshTokenCommand.class));
    }

    @Test
    @DisplayName("JWT 토큰 서명 오류 예외 처리 테스트")
    void refreshAccessToken_Failure_SignatureException() {
        // Given
        when(jwtTokenProvider.getUserIdFromToken(TEST_REFRESH_TOKEN))
                .thenThrow(new io.jsonwebtoken.security.SignatureException("JWT signature does not match"));

        // When & Then
        assertThatThrownBy(() -> authFacadeService.refreshAccessToken(TEST_REFRESH_TOKEN))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("토큰 갱신에 실패했습니다");

        // 토큰 검증이 호출되지 않았는지 확인
        verify(authQueryService, never()).validateRefreshToken(any(ValidateRefreshTokenQuery.class));
        verify(authCommandService, never()).refreshAccessToken(any(RefreshTokenCommand.class));
    }

    @Test
    @DisplayName("JWT 토큰 지원하지 않는 형식 예외 처리 테스트")
    void refreshAccessToken_Failure_UnsupportedJwtException() {
        // Given
        when(jwtTokenProvider.getUserIdFromToken(TEST_REFRESH_TOKEN))
                .thenThrow(new io.jsonwebtoken.UnsupportedJwtException("Unsupported JWT token"));

        // When & Then
        assertThatThrownBy(() -> authFacadeService.refreshAccessToken(TEST_REFRESH_TOKEN))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("토큰 갱신에 실패했습니다");

        // 토큰 검증이 호출되지 않았는지 확인
        verify(authQueryService, never()).validateRefreshToken(any(ValidateRefreshTokenQuery.class));
        verify(authCommandService, never()).refreshAccessToken(any(RefreshTokenCommand.class));
    }

    @Test
    @DisplayName("JWT 토큰 일반 예외 처리 테스트")
    void refreshAccessToken_Failure_GeneralJwtException() {
        // Given
        when(jwtTokenProvider.getUserIdFromToken(TEST_REFRESH_TOKEN))
                .thenThrow(new io.jsonwebtoken.JwtException("Invalid JWT token"));

        // When & Then
        assertThatThrownBy(() -> authFacadeService.refreshAccessToken(TEST_REFRESH_TOKEN))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("토큰 갱신에 실패했습니다");

        // 토큰 검증이 호출되지 않았는지 확인
        verify(authQueryService, never()).validateRefreshToken(any(ValidateRefreshTokenQuery.class));
        verify(authCommandService, never()).refreshAccessToken(any(RefreshTokenCommand.class));
    }
}
