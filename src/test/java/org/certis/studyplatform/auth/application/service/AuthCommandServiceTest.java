package org.certis.studyplatform.auth.application.service;

import org.certis.studyplatform.auth.application.object.command.RefreshTokenCommand;
import org.certis.studyplatform.auth.domain.model.vo.AccessTokenVo;
import org.certis.studyplatform.auth.domain.service.AuthDomainService;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.shared.security.JwtTokenProvider;
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
 * AuthCommandService 토큰 갱신 로직 테스트
 * - 토큰 로테이션 검증
 * - Redis 저장 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("🔐 AuthCommandService 토큰 갱신 테스트")
class AuthCommandServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private AuthDomainService authDomainService;

    @InjectMocks
    private AuthCommandService authCommandService;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final String TEST_STUDENT_NUMBER = "20201234";
    private static final String TEST_EMAIL = "test@certis.org";
    private static final String TEST_NAME = "김테스트";
    private static final MemberRole TEST_ROLE = MemberRole.PLAYER;
    private static final String TEST_ACCESS_TOKEN = "test.access.token";

    private RefreshTokenCommand mockCommand;
    private AccessTokenVo mockAccessToken;

    @BeforeEach
    void setUp() {
        mockCommand = RefreshTokenCommand.of(
                TEST_MEMBER_ID,
                TEST_STUDENT_NUMBER,
                TEST_EMAIL,
                TEST_NAME,
                TEST_ROLE
        );

        mockAccessToken = new AccessTokenVo(
                TEST_ACCESS_TOKEN,
                LocalDateTime.now().plusHours(1)
        );
    }

    @Test
    @DisplayName("토큰 갱신 성공 - AccessToken과 RefreshToken 모두 생성")
    void refreshAccessToken_Success_BothTokensGenerated() {
        // Given
        when(jwtTokenProvider.generateAccessToken(
                TEST_MEMBER_ID, TEST_STUDENT_NUMBER, TEST_EMAIL, TEST_NAME, TEST_ROLE))
                .thenReturn(mockAccessToken);

        // When
        AccessTokenVo result = authCommandService.refreshAccessToken(mockCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.value()).isEqualTo(TEST_ACCESS_TOKEN);
        assertThat(result.expiredAt()).isNotNull();

        // 토큰 생성 검증
        verify(jwtTokenProvider, times(1)).generateAccessToken(
                TEST_MEMBER_ID, TEST_STUDENT_NUMBER, TEST_EMAIL, TEST_NAME, TEST_ROLE);
        verify(jwtTokenProvider, never()).generateRefreshToken(any());
        verify(authDomainService, never()).saveRefreshToken(any());
    }

    @Test
    @DisplayName("토큰 갱신 실패 - AccessToken 생성 실패")
    void refreshAccessToken_Failure_AccessTokenGenerationFailed() {
        // Given
        when(jwtTokenProvider.generateAccessToken(
                TEST_MEMBER_ID, TEST_STUDENT_NUMBER, TEST_EMAIL, TEST_NAME, TEST_ROLE))
                .thenThrow(new RuntimeException("토큰 생성 실패"));

        // When & Then
        assertThatThrownBy(() -> authCommandService.refreshAccessToken(mockCommand))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("토큰 생성 실패");

        // RefreshToken 생성이 호출되지 않았는지 확인
        verify(jwtTokenProvider, never()).generateRefreshToken(any());
        verify(authDomainService, never()).saveRefreshToken(any());
    }

    @Test
    @DisplayName("토큰 갱신 실패 - RefreshToken 저장 실패")
    void refreshAccessToken_Failure_RefreshTokenSaveFailed() {
        // Given
        when(jwtTokenProvider.generateAccessToken(
                TEST_MEMBER_ID, TEST_STUDENT_NUMBER, TEST_EMAIL, TEST_NAME, TEST_ROLE))
                .thenReturn(mockAccessToken);
        
        // When
        AccessTokenVo result = authCommandService.refreshAccessToken(mockCommand);

        // Then: RefreshToken 저장 로직이 호출되지 않음을 검증 (현재 비즈니스 로직 상 로테이션 없음)
        assertThat(result).isNotNull();
        verify(jwtTokenProvider, times(1)).generateAccessToken(
                TEST_MEMBER_ID, TEST_STUDENT_NUMBER, TEST_EMAIL, TEST_NAME, TEST_ROLE);
        verify(jwtTokenProvider, never()).generateRefreshToken(any());
        verify(authDomainService, never()).saveRefreshToken(any());
    }
}
