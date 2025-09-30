package org.certis.studyplatform.blog.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.blog.domain.repository.BlogRedisRepository;
import org.certis.studyplatform.blog.domain.repository.BlogViewQueryRepository;
import org.certis.studyplatform.blog.domain.vo.BlogIdVo;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogViewApplicationService {

    private final BlogRedisRepository redisRepository;
    private final BlogViewQueryRepository queryRepository;

    /**
     * ViewCount 조회 (Redis 우선, 없으면 BlogViewEntity에서 조회)
     */
    public Integer getViewCountWithFallback(BlogIdVo blogIdVo) {
        try {
            // 1. Redis에서 조회 시도
            Long redisViewCount = redisRepository.getViewCount(blogIdVo);
            if (redisViewCount != null && redisViewCount >= 0) {
                log.debug("Domain: ViewCount retrieved from Redis for blog: {} = {}", blogIdVo.value(), redisViewCount);
                return redisViewCount.intValue();
            }
        } catch (Exception e) {
            log.warn("Domain: Redis view count retrieval failed for blog: {}, falling back to DB", blogIdVo.value(), e);
        }

        try {
            // 2. BlogViewEntity에서 조회 (fallback)
            Integer dbViewCount = queryRepository.getViewCount(blogIdVo);
            log.debug("Domain: ViewCount retrieved from DB for blog: {} = {}", blogIdVo.value(), dbViewCount);

            // Redis에 동기화
            try {
                redisRepository.initializeStats(blogIdVo);
            } catch (Exception ex) {
                log.warn("Domain: Failed to sync DB view count to Redis for blog: {}", blogIdVo.value(), ex);
            }

            return dbViewCount != null ? dbViewCount : 0;
        } catch (Exception e) {
            log.error("Domain: DB view count retrieval failed for blog: {}", blogIdVo.value(), e);
            return 0;
        }
    }

    /**
     * Redis에서 조회수 증가 (중복 방지)
     */
    public void incrementViewCountInRedis(Long blogId, Long viewerId) {
        try {
            BlogIdVo blogIdVo = BlogIdVo.of(blogId);

            if (!redisRepository.isViewedByMember(blogIdVo, viewerId)) {
                redisRepository.addView(blogIdVo, viewerId);
                log.debug("Domain: View count increased in Redis for blog: {} by viewer: {}", blogId, viewerId);
            } else {
                log.debug("Domain: View already counted for blog: {} by viewer: {}", blogId, viewerId);
            }
        } catch (Exception e) {
            log.error("Domain: Redis failed for view increment - blogId: {}, viewerId: {}", blogId, viewerId, e);
        }
    }

    public void initializeStats(BlogIdVo blogIdVo) {
        redisRepository.initializeStats(blogIdVo);
    }

    public void deleteStats(BlogIdVo blogIdVo) {
        redisRepository.deleteStats(blogIdVo);
    }
}
