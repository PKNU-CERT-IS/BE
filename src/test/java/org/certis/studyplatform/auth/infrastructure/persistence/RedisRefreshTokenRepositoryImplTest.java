package org.certis.studyplatform.auth.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.member.domain.vo.MemberIdVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * RedisRefreshTokenRepositoryImpl 최적화 테스트
 * - Redis 트래픽 최적화 검증
 * - 만료 토큰 자동 삭제 검증
 * - 에러 처리 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("🔐 RedisRefreshTokenRepository 최적화 테스트")
class RedisRefreshTokenRepositoryImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisRefreshTokenRepositoryImpl repository;
    private ObjectMapper objectMapper;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final String TEST_TOKEN = "test.refresh.token";
    private static final String TEST_KEY = "refresh_token:" + TEST_MEMBER_ID;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        repository = new RedisRefreshTokenRepositoryImpl(redisTemplate);
    }

    @Test
    @DisplayName("토큰 저장 성공 - TTL 설정 검증")
    void save_Success_TTLSet() {
        // Given
        RefreshTokenVo refreshToken = new RefreshTokenVo(
                TEST_TOKEN,
                LocalDateTime.now().plusDays(1),
                TEST_MEMBER_ID
        );
        Duration ttl = Duration.ofDays(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        repository.save(refreshToken, ttl);

        // Then
        verify(valueOperations, times(1)).set(TEST_KEY, refreshToken, ttl);
    }

    @Test
    @DisplayName("토큰 조회 성공 - 유효한 토큰")
    void findByMemberId_Success_ValidToken() {
        // Given
        RefreshTokenVo refreshToken = new RefreshTokenVo(
                TEST_TOKEN,
                LocalDateTime.now().plusDays(1),
                TEST_MEMBER_ID
        );
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(TEST_KEY)).thenReturn(refreshToken);

        // When
        Optional<RefreshTokenVo> result = repository.findByMemberId(MemberIdVo.of(TEST_MEMBER_ID));

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualTo(TEST_TOKEN);
        assertThat(result.get().memberId()).isEqualTo(TEST_MEMBER_ID);
        
        // Redis 호출 횟수 검증 (1회만 호출되어야 함)
        verify(valueOperations, times(1)).get(TEST_KEY);
    }

    @Test
    @DisplayName("토큰 조회 성공 - 만료된 토큰 자동 삭제")
    void findByMemberId_Success_ExpiredTokenAutoDeleted() {
        // Given
        RefreshTokenVo expiredToken = new RefreshTokenVo(
                TEST_TOKEN,
                LocalDateTime.now().minusDays(1), // 만료된 토큰
                TEST_MEMBER_ID
        );
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(TEST_KEY)).thenReturn(expiredToken);

        // When
        Optional<RefreshTokenVo> result = repository.findByMemberId(MemberIdVo.of(TEST_MEMBER_ID));

        // Then
        assertThat(result).isEmpty();
        
        // 만료된 토큰이 자동으로 삭제되었는지 확인
        verify(redisTemplate, times(1)).delete(TEST_KEY);
        verify(valueOperations, times(1)).get(TEST_KEY);
    }

    @Test
    @DisplayName("토큰 조회 실패 - 토큰 없음")
    void findByMemberId_Failure_TokenNotFound() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(TEST_KEY)).thenReturn(null);

        // When
        Optional<RefreshTokenVo> result = repository.findByMemberId(MemberIdVo.of(TEST_MEMBER_ID));

        // Then
        assertThat(result).isEmpty();
        verify(valueOperations, times(1)).get(TEST_KEY);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("토큰 조회 실패 - Redis 오류 시 안전한 처리")
    void findByMemberId_Failure_RedisErrorSafeHandling() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(TEST_KEY)).thenThrow(new RuntimeException("Redis 연결 오류"));

        // When
        Optional<RefreshTokenVo> result = repository.findByMemberId(MemberIdVo.of(TEST_MEMBER_ID));

        // Then
        assertThat(result).isEmpty(); // 오류 시 빈 결과 반환하여 서비스 중단 방지
        verify(valueOperations, times(1)).get(TEST_KEY);
    }

    @Test
    @DisplayName("토큰 삭제 성공")
    void deleteByMemberId_Success() {
        // Given
        when(redisTemplate.delete(TEST_KEY)).thenReturn(true);

        // When
        repository.deleteByMemberId(MemberIdVo.of(TEST_MEMBER_ID));

        // Then
        verify(redisTemplate, times(1)).delete(TEST_KEY);
    }

    @Test
    @DisplayName("토큰 삭제 실패 - Redis 오류")
    void deleteByMemberId_Failure_RedisError() {
        // Given
        when(redisTemplate.delete(TEST_KEY)).thenThrow(new RuntimeException("Redis 연결 오류"));

        // When & Then
        assertThatThrownBy(() -> repository.deleteByMemberId(MemberIdVo.of(TEST_MEMBER_ID)))
                .isInstanceOf(org.certis.studyplatform.exception.InfrastructureException.class);
    }

    @Test
    @DisplayName("토큰 존재 확인 성공")
    void existsByMemberId_Success() {
        // Given
        when(redisTemplate.hasKey(TEST_KEY)).thenReturn(true);

        // When
        boolean exists = repository.existsByMemberId(TEST_MEMBER_ID);

        // Then
        assertThat(exists).isTrue();
        verify(redisTemplate, times(1)).hasKey(TEST_KEY);
    }

    @Test
    @DisplayName("토큰 존재 확인 실패")
    void existsByMemberId_Failure() {
        // Given
        when(redisTemplate.hasKey(TEST_KEY)).thenReturn(false);

        // When
        boolean exists = repository.existsByMemberId(TEST_MEMBER_ID);

        // Then
        assertThat(exists).isFalse();
        verify(redisTemplate, times(1)).hasKey(TEST_KEY);
    }
}
