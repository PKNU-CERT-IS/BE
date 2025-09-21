package org.certis.studyplatform.blog.domain.service;

import org.certis.studyplatform.blog.domain.repository.BlogRedisRepository;
import org.certis.studyplatform.blog.domain.repository.BlogViewQueryRepository;
import org.certis.studyplatform.blog.domain.vo.BlogIdVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BlogViewDomainService 단위 테스트
 *
 * 🎯 테스트 특징:
 * - Redis 우선 조회, DB fallback 테스트
 * - 조회수 증가 로직 테스트
 * - 중복 조회 방지 테스트
 * - Redis 실패 시 fallback 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("📊 BlogViewDomainService 단위 테스트")
class BlogViewDomainServiceTest {

    @Mock
    private BlogRedisRepository blogRedisRepository;

    @Mock
    private BlogViewQueryRepository blogViewQueryRepository;

    private BlogViewDomainService blogViewDomainService;

    // 테스트 상수
    private static final Long TEST_BLOG_ID = 1L;
    private static final Long TEST_VIEWER_ID = 100L;
    private static final Integer TEST_DB_VIEW_COUNT = 50;
    private static final Long TEST_REDIS_VIEW_COUNT = 75L;

    @BeforeEach
    void setUp() {
        blogViewDomainService = new BlogViewDomainService(blogRedisRepository, blogViewQueryRepository);
    }

    @Test
    @DisplayName("조회수 조회 - Redis에서 성공적으로 조회")
    void getViewCountWithFallback_RedisSuccess() {
        // Given: Redis에서 조회수 조회 성공
        BlogIdVo blogIdVo = BlogIdVo.of(TEST_BLOG_ID);
        when(blogRedisRepository.getViewCount(blogIdVo)).thenReturn(TEST_REDIS_VIEW_COUNT);

        // When: 조회수 조회
        Integer result = blogViewDomainService.getViewCountWithFallback(blogIdVo);

        // Then: Redis에서 조회한 값이 반환되었는지 확인
        assertThat(result).isEqualTo(TEST_REDIS_VIEW_COUNT.intValue());
        verify(blogRedisRepository).getViewCount(blogIdVo);
        verify(blogViewQueryRepository, never()).getViewCount(any());
    }

    @Test
    @DisplayName("조회수 조회 - Redis 실패 시 DB에서 조회")
    void getViewCountWithFallback_RedisFailure_DbFallback() {
        // Given: Redis 조회 실패, DB 조회 성공
        BlogIdVo blogIdVo = BlogIdVo.of(TEST_BLOG_ID);
        when(blogRedisRepository.getViewCount(blogIdVo))
                .thenThrow(new RuntimeException("Redis connection failed"));
        when(blogViewQueryRepository.getViewCount(blogIdVo)).thenReturn(TEST_DB_VIEW_COUNT);
        doNothing().when(blogRedisRepository).initializeStats(blogIdVo);

        // When: 조회수 조회
        Integer result = blogViewDomainService.getViewCountWithFallback(blogIdVo);

        // Then: DB에서 조회한 값이 반환되었는지 확인
        assertThat(result).isEqualTo(TEST_DB_VIEW_COUNT);
        verify(blogRedisRepository).getViewCount(blogIdVo);
        verify(blogViewQueryRepository).getViewCount(blogIdVo);
        verify(blogRedisRepository).initializeStats(blogIdVo);
    }

    @Test
    @DisplayName("조회수 조회 - Redis와 DB 모두 실패 시 0 반환")
    void getViewCountWithFallback_BothFail_ReturnsZero() {
        // Given: Redis와 DB 모두 조회 실패
        BlogIdVo blogIdVo = BlogIdVo.of(TEST_BLOG_ID);
        when(blogRedisRepository.getViewCount(blogIdVo))
                .thenThrow(new RuntimeException("Redis connection failed"));
        when(blogViewQueryRepository.getViewCount(blogIdVo))
                .thenThrow(new RuntimeException("DB connection failed"));

        // When: 조회수 조회
        Integer result = blogViewDomainService.getViewCountWithFallback(blogIdVo);

        // Then: 0이 반환되었는지 확인
        assertThat(result).isEqualTo(0);
        verify(blogRedisRepository).getViewCount(blogIdVo);
        verify(blogViewQueryRepository).getViewCount(blogIdVo);
    }

    @Test
    @DisplayName("조회수 증가 - 중복 조회가 아닌 경우")
    void incrementViewCountInRedis_NewViewer() {
        // Given: 새로운 조회자
        when(blogRedisRepository.isViewedByMember(any(BlogIdVo.class), eq(TEST_VIEWER_ID)))
                .thenReturn(false);
        doNothing().when(blogRedisRepository).addView(any(BlogIdVo.class), eq(TEST_VIEWER_ID));

        // When: 조회수 증가
        blogViewDomainService.incrementViewCountInRedis(TEST_BLOG_ID, TEST_VIEWER_ID);

        // Then: Redis에 조회수 증가가 호출되었는지 확인
        verify(blogRedisRepository).isViewedByMember(any(BlogIdVo.class), eq(TEST_VIEWER_ID));
        verify(blogRedisRepository).addView(any(BlogIdVo.class), eq(TEST_VIEWER_ID));
    }

    @Test
    @DisplayName("조회수 증가 - 이미 조회한 사용자인 경우")
    void incrementViewCountInRedis_ExistingViewer() {
        // Given: 이미 조회한 사용자
        when(blogRedisRepository.isViewedByMember(any(BlogIdVo.class), eq(TEST_VIEWER_ID)))
                .thenReturn(true);

        // When: 조회수 증가
        blogViewDomainService.incrementViewCountInRedis(TEST_BLOG_ID, TEST_VIEWER_ID);

        // Then: 조회수 증가가 호출되지 않았는지 확인
        verify(blogRedisRepository).isViewedByMember(any(BlogIdVo.class), eq(TEST_VIEWER_ID));
        verify(blogRedisRepository, never()).addView(any(BlogIdVo.class), any());
    }

    @Test
    @DisplayName("조회수 증가 - Redis 실패 시 예외 처리")
    void incrementViewCountInRedis_RedisFailure_HandlesException() {
        // Given: Redis 조회 실패
        when(blogRedisRepository.isViewedByMember(any(BlogIdVo.class), eq(TEST_VIEWER_ID)))
                .thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then: 예외가 발생해도 정상적으로 처리되는지 확인
        assertThatCode(() -> 
            blogViewDomainService.incrementViewCountInRedis(TEST_BLOG_ID, TEST_VIEWER_ID)
        ).doesNotThrowAnyException();

        verify(blogRedisRepository).isViewedByMember(any(BlogIdVo.class), eq(TEST_VIEWER_ID));
    }

    @Test
    @DisplayName("통계 초기화")
    void initializeStats() {
        // Given: BlogIdVo
        BlogIdVo blogIdVo = BlogIdVo.of(TEST_BLOG_ID);
        doNothing().when(blogRedisRepository).initializeStats(blogIdVo);

        // When: 통계 초기화
        blogViewDomainService.initializeStats(blogIdVo);

        // Then: Redis 초기화가 호출되었는지 확인
        verify(blogRedisRepository).initializeStats(blogIdVo);
    }

    @Test
    @DisplayName("통계 삭제")
    void deleteStats() {
        // Given: BlogIdVo
        BlogIdVo blogIdVo = BlogIdVo.of(TEST_BLOG_ID);
        doNothing().when(blogRedisRepository).deleteStats(blogIdVo);

        // When: 통계 삭제
        blogViewDomainService.deleteStats(blogIdVo);

        // Then: Redis 삭제가 호출되었는지 확인
        verify(blogRedisRepository).deleteStats(blogIdVo);
    }

    @Test
    @DisplayName("조회수 조회 - Redis에서 null 반환 시 DB fallback")
    void getViewCountWithFallback_RedisNull_DbFallback() {
        // Given: Redis에서 null 반환, DB 조회 성공
        BlogIdVo blogIdVo = BlogIdVo.of(TEST_BLOG_ID);
        when(blogRedisRepository.getViewCount(blogIdVo)).thenReturn(null);
        when(blogViewQueryRepository.getViewCount(blogIdVo)).thenReturn(TEST_DB_VIEW_COUNT);
        doNothing().when(blogRedisRepository).initializeStats(blogIdVo);

        // When: 조회수 조회
        Integer result = blogViewDomainService.getViewCountWithFallback(blogIdVo);

        // Then: DB에서 조회한 값이 반환되었는지 확인
        assertThat(result).isEqualTo(TEST_DB_VIEW_COUNT);
        verify(blogRedisRepository).getViewCount(blogIdVo);
        verify(blogViewQueryRepository).getViewCount(blogIdVo);
        verify(blogRedisRepository).initializeStats(blogIdVo);
    }

    @Test
    @DisplayName("조회수 조회 - Redis에서 음수 반환 시 DB fallback")
    void getViewCountWithFallback_RedisNegative_DbFallback() {
        // Given: Redis에서 음수 반환, DB 조회 성공
        BlogIdVo blogIdVo = BlogIdVo.of(TEST_BLOG_ID);
        when(blogRedisRepository.getViewCount(blogIdVo)).thenReturn(-1L);
        when(blogViewQueryRepository.getViewCount(blogIdVo)).thenReturn(TEST_DB_VIEW_COUNT);
        doNothing().when(blogRedisRepository).initializeStats(blogIdVo);

        // When: 조회수 조회
        Integer result = blogViewDomainService.getViewCountWithFallback(blogIdVo);

        // Then: DB에서 조회한 값이 반환되었는지 확인
        assertThat(result).isEqualTo(TEST_DB_VIEW_COUNT);
        verify(blogRedisRepository).getViewCount(blogIdVo);
        verify(blogViewQueryRepository).getViewCount(blogIdVo);
        verify(blogRedisRepository).initializeStats(blogIdVo);
    }
}
