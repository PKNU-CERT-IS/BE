package org.certis.studyplatform.blog.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.blog.domain.repository.BlogRedisRepository;
import org.certis.studyplatform.blog.domain.vo.BlogIdVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Blog Redis Repository Implementation
 *
 * Redis를 활용한 블로그 조회수 관리 및 중복 방지 캐시
 * - 조회수 실시간 관리
 * - 2차 캐시를 통한 중복 조회 방지
 * - 매일 자정 RDB 동기화를 위한 데이터 제공
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class BlogRedisRepositoryImpl implements BlogRedisRepository {

    private final RedisTemplate<String, String> redisTemplate;

    // Redis Key Patterns
    private static final String VIEW_COUNT_KEY_PREFIX = "blog:view:count:";
    private static final String VIEWED_MEMBERS_KEY_PREFIX = "blog:view:members:";
    private static final long VIEW_CACHE_EXPIRE_HOURS = 25; // 25시간 후 만료

    @Override
    public void initializeStats(BlogIdVo blogIdVo) {
        String viewCountKey = buildViewCountKey(blogIdVo);
        String viewedMembersKey = buildViewedMembersKey(blogIdVo);

        try {
            // 조회수 0으로 초기화 (이미 존재하지 않는 경우에만)
            redisTemplate.opsForValue().setIfAbsent(viewCountKey, "0");

            // 조회한 멤버 목록 초기화 (Set으로 관리)
            redisTemplate.delete(viewedMembersKey);

            // TTL 설정 (25시간 후 만료, 매일 동기화 이후 재설정됨)
            redisTemplate.expire(viewCountKey, VIEW_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            redisTemplate.expire(viewedMembersKey, VIEW_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

            log.debug("Redis: Initialized stats for blog: {}", blogIdVo.value());
        } catch (Exception e) {
            log.error("Redis: Failed to initialize stats for blog: {}", blogIdVo.value(), e);
            throw new RuntimeException("Redis 통계 초기화 실패", e);
        }
    }

    @Override
    public void deleteStats(BlogIdVo blogIdVo) {
        String viewCountKey = buildViewCountKey(blogIdVo);
        String viewedMembersKey = buildViewedMembersKey(blogIdVo);

        try {
            redisTemplate.delete(viewCountKey);
            redisTemplate.delete(viewedMembersKey);

            log.debug("Redis: Deleted stats for blog: {}", blogIdVo.value());
        } catch (Exception e) {
            log.error("Redis: Failed to delete stats for blog: {}", blogIdVo.value(), e);
            throw new RuntimeException("Redis 통계 삭제 실패", e);
        }
    }

    @Override
    public void addView(BlogIdVo blogIdVo, Long viewerId) {
        String viewCountKey = buildViewCountKey(blogIdVo);
        String viewedMembersKey = buildViewedMembersKey(blogIdVo);
        String viewerIdString = viewerId.toString();

        try {
            SetOperations<String, String> setOps = redisTemplate.opsForSet();

            // 이미 조회한 사용자인지 확인 (2차 캐시 역할)
            if (setOps.isMember(viewedMembersKey, viewerIdString)) {
                log.debug("Redis: User {} already viewed blog: {}", viewerId, blogIdVo.value());
                return;
            }

            // 조회수 증가
            redisTemplate.opsForValue().increment(viewCountKey);

            // 조회한 사용자 목록에 추가 (중복 방지)
            setOps.add(viewedMembersKey, viewerIdString);

            // TTL 갱신 (매번 조회할 때마다 24시간 연장)
            redisTemplate.expire(viewCountKey, VIEW_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            redisTemplate.expire(viewedMembersKey, VIEW_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

            log.debug("Redis: Added view for blog: {} by user: {}", blogIdVo.value(), viewerId);
        } catch (Exception e) {
            log.error("Redis: Failed to add view for blog: {} by user: {}", blogIdVo.value(), viewerId, e);
            throw new RuntimeException("Redis 조회수 증가 실패", e);
        }
    }

    @Override
    public boolean isViewedByMember(BlogIdVo blogIdVo, Long viewerId) {
        String viewedMembersKey = buildViewedMembersKey(blogIdVo);
        String viewerIdString = viewerId.toString();

        try {
            SetOperations<String, String> setOps = redisTemplate.opsForSet();
            boolean isViewed = setOps.isMember(viewedMembersKey, viewerIdString);

            log.debug("Redis: Blog {} viewed by user {}: {}", blogIdVo.value(), viewerId, isViewed);
            return isViewed;
        } catch (Exception e) {
            log.error("Redis: Failed to check if blog {} viewed by user: {}", blogIdVo.value(), viewerId, e);
            // 에러 발생 시 false 반환하여 조회수 증가를 허용
            return false;
        }
    }

    @Override
    public Long getViewCount(BlogIdVo blogIdVo) {
        String viewCountKey = buildViewCountKey(blogIdVo);

        try {
            String viewCountStr = redisTemplate.opsForValue().get(viewCountKey);

            if (viewCountStr == null) {
                log.debug("Redis: No view count found for blog: {}, initializing to 0", blogIdVo.value());
                // 존재하지 않으면 0으로 초기화
                initializeStats(blogIdVo);
                return 0L;
            }

            Long viewCount = Long.parseLong(viewCountStr);
            log.debug("Redis: Retrieved view count for blog: {} = {}", blogIdVo.value(), viewCount);
            return viewCount;
        } catch (Exception e) {
            log.error("Redis: Failed to get view count for blog: {}", blogIdVo.value(), e);
            throw new RuntimeException("Redis 조회수 조회 실패", e);
        }
    }

    /**
     * 배치 작업을 위한 추가 메서드들
     */

    /**
     * 모든 블로그의 조회수 정보를 한번에 조회 (배치 최적화)
     */
    public java.util.Map<Long, Long> getAllViewCounts(java.util.List<Long> blogIds) {
        java.util.Map<Long, Long> result = new java.util.HashMap<>();

        try {
            for (Long blogId : blogIds) {
                BlogIdVo blogIdVo = BlogIdVo.of(blogId);
                Long viewCount = getViewCount(blogIdVo);
                result.put(blogId, viewCount);
            }

            log.debug("Redis: Retrieved view counts for {} blogs", blogIds.size());
            return result;
        } catch (Exception e) {
            log.error("Redis: Failed to get all view counts for blogs: {}", blogIds, e);
            throw new RuntimeException("Redis 전체 조회수 조회 실패", e);
        }
    }

    /**
     * 특정 블로그의 조회한 사용자 수 조회
     */
    public Long getUniqueViewerCount(BlogIdVo blogIdVo) {
        String viewedMembersKey = buildViewedMembersKey(blogIdVo);

        try {
            SetOperations<String, String> setOps = redisTemplate.opsForSet();
            Long uniqueViewers = setOps.size(viewedMembersKey);

            log.debug("Redis: Unique viewer count for blog: {} = {}", blogIdVo.value(), uniqueViewers);
            return uniqueViewers != null ? uniqueViewers : 0L;
        } catch (Exception e) {
            log.error("Redis: Failed to get unique viewer count for blog: {}", blogIdVo.value(), e);
            return 0L;
        }
    }

    /**
     * 조회 기록 초기화 (매일 자정 배치 실행 후 호출)
     */
    public void resetViewerHistory(BlogIdVo blogIdVo) {
        String viewedMembersKey = buildViewedMembersKey(blogIdVo);

        try {
            redisTemplate.delete(viewedMembersKey);
            // TTL 재설정
            redisTemplate.expire(viewedMembersKey, VIEW_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

            log.debug("Redis: Reset viewer history for blog: {}", blogIdVo.value());
        } catch (Exception e) {
            log.error("Redis: Failed to reset viewer history for blog: {}", blogIdVo.value(), e);
        }
    }

    /**
     * 전체 캐시 상태 확인 (모니터링용)
     */
    public boolean isHealthy() {
        try {
            // 간단한 ping 테스트
            redisTemplate.opsForValue().set("health:check", "ok", Duration.ofSeconds(1));
            String result = redisTemplate.opsForValue().get("health:check");
            redisTemplate.delete("health:check");

            return "ok".equals(result);
        } catch (Exception e) {
            log.error("Redis: Health check failed", e);
            return false;
        }
    }

    // ================================================================
    // PRIVATE HELPER METHODS
    // ================================================================

    private String buildViewCountKey(BlogIdVo blogIdVo) {
        return VIEW_COUNT_KEY_PREFIX + blogIdVo.value();
    }

    private String buildViewedMembersKey(BlogIdVo blogIdVo) {
        return VIEWED_MEMBERS_KEY_PREFIX + blogIdVo.value();
    }
}