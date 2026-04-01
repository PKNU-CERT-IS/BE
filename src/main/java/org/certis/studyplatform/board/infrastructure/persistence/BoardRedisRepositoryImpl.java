package org.certis.studyplatform.board.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.board.domain.model.vo.BoardIdVo;
import org.certis.studyplatform.board.domain.model.vo.BoardRedisDeltaVo;
import org.certis.studyplatform.board.domain.repository.BoardRedisRepository;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBatch;
import org.redisson.api.RFuture;
import org.redisson.api.RKeys;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
@Slf4j
public class BoardRedisRepositoryImpl implements BoardRedisRepository {

    private final RedissonClient redissonClient;

    // TTL 설정 (24시간 동기화와 호환되도록 조정)
    private static final int LIKE_CACHE_TTL_HOURS = 25; // 좋아요 관련 캐시 25시간 (동기화 후 1시간 여유)
    private static final int VIEW_CACHE_TTL_HOURS = 25; // 조회수 관련 캐시 25시간 (동기화 후 1시간 여유)
    private static final int MEMBERS_CACHE_TTL_HOURS = 2; // 사용자 목록 캐시 2시간 (중복 방지용)

    @Override
    public void initializeStats(BoardIdVo boardIdVo) {
        try {
            RAtomicLong likeCount = redissonClient.getAtomicLong(BoardStatRedisKeys.likeActive(boardIdVo.value()));
            RAtomicLong likeFlush = redissonClient.getAtomicLong(BoardStatRedisKeys.likeFlush(boardIdVo.value()));
            RSet<Long> likeMembers = redissonClient.getSet(BoardStatRedisKeys.likeMembers(boardIdVo.value()));

            likeCount.set(0L);
            likeFlush.set(0L);
            likeMembers.clear();
            likeCount.expire(Duration.ofHours(LIKE_CACHE_TTL_HOURS));
            likeFlush.expire(Duration.ofHours(LIKE_CACHE_TTL_HOURS));
            likeMembers.expire(Duration.ofHours(MEMBERS_CACHE_TTL_HOURS));

            RAtomicLong viewCount = redissonClient.getAtomicLong(BoardStatRedisKeys.viewActive(boardIdVo.value()));
            RAtomicLong viewFlush = redissonClient.getAtomicLong(BoardStatRedisKeys.viewFlush(boardIdVo.value()));
            RSet<Long> viewMembers = redissonClient.getSet(BoardStatRedisKeys.viewMembers(boardIdVo.value()));

            viewCount.set(0L);
            viewFlush.set(0L);
            viewMembers.clear();
            viewCount.expire(Duration.ofHours(VIEW_CACHE_TTL_HOURS));
            viewFlush.expire(Duration.ofHours(VIEW_CACHE_TTL_HOURS));
            viewMembers.expire(Duration.ofHours(MEMBERS_CACHE_TTL_HOURS));

        } catch (Exception e) {
            log.error("❌ Redis: Failed to initialize stats for board: {}", boardIdVo.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "Redis 통계 초기화에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public void deleteStats(BoardIdVo boardIdVo) {
        try {
            RKeys keys = redissonClient.getKeys();

            keys.delete(BoardStatRedisKeys.likeActive(boardIdVo.value()));
            keys.delete(BoardStatRedisKeys.likeFlush(boardIdVo.value()));
            keys.delete(BoardStatRedisKeys.likeMembers(boardIdVo.value()));
            keys.delete(BoardStatRedisKeys.viewActive(boardIdVo.value()));
            keys.delete(BoardStatRedisKeys.viewFlush(boardIdVo.value()));
            keys.delete(BoardStatRedisKeys.viewMembers(boardIdVo.value()));

        } catch (Exception e) {
            log.error("❌ Redis: Failed to delete stats for board: {}", boardIdVo.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "Redis 통계 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public void addLike(BoardIdVo boardId, Long memberId) {
        try {
            RAtomicLong likeCount = redissonClient.getAtomicLong(BoardStatRedisKeys.likeActive(boardId.value()));
            RSet<Long> likeMembers = redissonClient.getSet(BoardStatRedisKeys.likeMembers(boardId.value()));

            // 배치 작업으로 최적화: 한 번의 Redis 호출로 처리
            if (likeMembers.add(memberId)) {
                likeCount.incrementAndGet();
                // TTL 갱신으로 불필요한 만료/재생성 방지
                likeCount.expire(Duration.ofHours(LIKE_CACHE_TTL_HOURS));
                likeMembers.expire(Duration.ofHours(MEMBERS_CACHE_TTL_HOURS));
                log.debug("✅ Redis: Added like for board: {} by member: {}", boardId.value(), memberId);
            } else {
                log.debug("Redis: Member {} already liked board: {}", memberId, boardId.value());
            }

        } catch (Exception e) {
            log.error("❌ Redis: Failed to add like - board: {}, member: {}", boardId.value(), memberId, e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "좋아요 추가에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public void removeLike(BoardIdVo boardId, Long memberId) {
        try {
            RAtomicLong likeCount = redissonClient.getAtomicLong(BoardStatRedisKeys.likeActive(boardId.value()));
            RSet<Long> likeMembers = redissonClient.getSet(BoardStatRedisKeys.likeMembers(boardId.value()));

            if (likeMembers.contains(memberId)) {
                likeMembers.remove(memberId);
                likeCount.decrementAndGet();
                // 활발한 키의 TTL을 갱신하여 불필요한 만료/재생성 방지
                likeCount.expire(Duration.ofHours(LIKE_CACHE_TTL_HOURS));
                likeMembers.expire(Duration.ofHours(MEMBERS_CACHE_TTL_HOURS));
            }

        } catch (Exception e) {
            log.error("❌ Redis: Failed to remove like - board: {}, member: {}", boardId.value(), memberId, e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "좋아요 제거에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public boolean isLikedByMember(BoardIdVo boardId, Long memberId) {
        try {
            RSet<Long> likeMembers = redissonClient.getSet(BoardStatRedisKeys.likeMembers(boardId.value()));

            return likeMembers.contains(memberId);

        } catch (Exception e) {
            log.error("❌ Redis: Failed to check like status - board: {}, member: {}", boardId.value(), memberId, e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "좋아요 상태 확인에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public Long getLikeCount(BoardIdVo boardId) {
        try {
            BoardRedisDeltaVo delta = getDeltaSnapshot(boardId);
            return delta.totalLikeDelta();

        } catch (Exception e) {
            log.error("❌ Redis: Failed to get like count for board: {}", boardId.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "좋아요 수 조회에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public Map<Long, Long> getLikeCounts(List<BoardIdVo> boardIds) {
        return batchReadDeltas(boardIds, true);
    }

    @Override
    public void addView(BoardIdVo boardId, Long viewerId) {
        try {
            RAtomicLong viewCount = redissonClient.getAtomicLong(BoardStatRedisKeys.viewActive(boardId.value()));
            RSet<Long> viewMembers = redissonClient.getSet(BoardStatRedisKeys.viewMembers(boardId.value()));

            // 단일 호출로 중복 체크 및 추가를 수행하고, 추가된 경우에만 카운트 증가
            if (viewMembers.add(viewerId)) {
                viewCount.incrementAndGet();
                // TTL 갱신
                viewCount.expire(Duration.ofHours(VIEW_CACHE_TTL_HOURS));
                viewMembers.expire(Duration.ofHours(MEMBERS_CACHE_TTL_HOURS));
            }

        } catch (Exception e) {
            log.error("❌ Redis: Failed to add view - board: {}, viewer: {}", boardId.value(), viewerId, e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "조회수 추가에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public boolean isViewedByMember(BoardIdVo boardId, Long viewerId) {
        try {
            RSet<Long> viewMembers = redissonClient.getSet(BoardStatRedisKeys.viewMembers(boardId.value()));

            return viewMembers.contains(viewerId);

        } catch (Exception e) {
            log.error("❌ Redis: Failed to check view status - board: {}, viewer: {}", boardId.value(), viewerId, e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "조회 상태 확인에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public Long getViewCount(BoardIdVo boardId) {
        try {
            BoardRedisDeltaVo delta = getDeltaSnapshot(boardId);
            return delta.totalViewDelta();

        } catch (Exception e) {
            log.error("❌ Redis: Failed to get view count for board: {}", boardId.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "조회수 조회에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public Map<Long, Long> getViewCounts(List<BoardIdVo> boardIds) {
        return batchReadDeltas(boardIds, false);
    }

    @Override
    public void incrementViewCount(BoardIdVo boardId) {
        try {
            RAtomicLong viewCount = redissonClient.getAtomicLong(BoardStatRedisKeys.viewActive(boardId.value()));
            
            viewCount.incrementAndGet();
            
            log.debug("✅ Redis: View count incremented for board: {}", boardId.value());

        } catch (Exception e) {
            log.error("❌ Redis: Failed to increment view count for board: {}", boardId.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "조회수 증가에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public void setLikeCount(BoardIdVo boardId, Long count) {
        try {
            RAtomicLong likeCount = redissonClient.getAtomicLong(BoardStatRedisKeys.likeActive(boardId.value()));
            
            likeCount.set(count);
            
            log.debug("✅ Redis: Set like count for board {} to {}", boardId.value(), count);
        } catch (Exception e) {
            log.error("❌ Redis: Failed to set like count for board: {}", boardId.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "좋아요 수 설정에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public void setViewCount(BoardIdVo boardId, Long count) {
        try {
            RAtomicLong viewCount = redissonClient.getAtomicLong(BoardStatRedisKeys.viewActive(boardId.value()));
            
            viewCount.set(count);
            
            log.debug("✅ Redis: Set view count for board {} to {}", boardId.value(), count);
        } catch (Exception e) {
            log.error("❌ Redis: Failed to set view count for board: {}", boardId.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "조회수 설정에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public BoardRedisDeltaVo getDeltaSnapshot(BoardIdVo boardId) {
        try {
            RAtomicLong activeLike = redissonClient.getAtomicLong(BoardStatRedisKeys.likeActive(boardId.value()));
            RAtomicLong flushLike = redissonClient.getAtomicLong(BoardStatRedisKeys.likeFlush(boardId.value()));
            RAtomicLong activeView = redissonClient.getAtomicLong(BoardStatRedisKeys.viewActive(boardId.value()));
            RAtomicLong flushView = redissonClient.getAtomicLong(BoardStatRedisKeys.viewFlush(boardId.value()));

            return BoardRedisDeltaVo.of(
                    activeLike.get(),
                    activeView.get(),
                    flushLike.get(),
                    flushView.get()
            );
        } catch (Exception e) {
            log.error("❌ Redis: Failed to get delta snapshot for board: {}", boardId.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "Redis delta snapshot 조회에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public BoardRedisDeltaVo rotateActiveDeltaToFlush(BoardIdVo boardId) {
        try {
            RAtomicLong activeLike = redissonClient.getAtomicLong(BoardStatRedisKeys.likeActive(boardId.value()));
            RAtomicLong activeView = redissonClient.getAtomicLong(BoardStatRedisKeys.viewActive(boardId.value()));
            RAtomicLong flushLike = redissonClient.getAtomicLong(BoardStatRedisKeys.likeFlush(boardId.value()));
            RAtomicLong flushView = redissonClient.getAtomicLong(BoardStatRedisKeys.viewFlush(boardId.value()));

            long drainedLike = activeLike.getAndSet(0L);
            long drainedView = activeView.getAndSet(0L);
            long nextFlushLike = flushLike.addAndGet(drainedLike);
            long nextFlushView = flushView.addAndGet(drainedView);

            activeLike.expire(Duration.ofHours(LIKE_CACHE_TTL_HOURS));
            activeView.expire(Duration.ofHours(VIEW_CACHE_TTL_HOURS));
            flushLike.expire(Duration.ofHours(LIKE_CACHE_TTL_HOURS));
            flushView.expire(Duration.ofHours(VIEW_CACHE_TTL_HOURS));

            return BoardRedisDeltaVo.of(0L, 0L, nextFlushLike, nextFlushView);
        } catch (Exception e) {
            log.error("❌ Redis: Failed to rotate active delta to flush for board: {}", boardId.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "Redis delta rotate에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public void clearFlushStats(BoardIdVo boardId) {
        try {
            RKeys keys = redissonClient.getKeys();
            keys.delete(
                    BoardStatRedisKeys.likeFlush(boardId.value()),
                    BoardStatRedisKeys.viewFlush(boardId.value())
            );
        } catch (Exception e) {
            log.error("❌ Redis: Failed to clear flush stats for board: {}", boardId.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "Redis flush stats 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    private Map<Long, Long> batchReadDeltas(List<BoardIdVo> boardIds, boolean like) {
        Map<Long, Long> activeCounts = batchReadCountsWithPrefix(
                boardIds,
                like ? BoardStatRedisKeys.likeActivePrefix() : BoardStatRedisKeys.viewActivePrefix()
        );
        Map<Long, Long> flushCounts = batchReadCountsWithPrefix(
                boardIds,
                like ? BoardStatRedisKeys.likeFlushPrefix() : BoardStatRedisKeys.viewFlushPrefix()
        );

        Map<Long, Long> combined = new LinkedHashMap<>();
        for (BoardIdVo boardId : boardIds) {
            Long boardKey = boardId.value();
            combined.put(
                    boardKey,
                    activeCounts.getOrDefault(boardKey, 0L) + flushCounts.getOrDefault(boardKey, 0L)
            );
        }
        return combined;
    }

    private Map<Long, Long> batchReadCountsWithPrefix(List<BoardIdVo> boardIds, String keyPrefix) {
        if (boardIds.isEmpty()) {
            return Map.of();
        }

        try {
            RBatch batch = redissonClient.createBatch();
            Map<Long, RFuture<Long>> futures = new LinkedHashMap<>();

            for (BoardIdVo boardId : boardIds) {
                futures.put(
                        boardId.value(),
                        batch.getAtomicLong(keyPrefix + boardId.value()).getAsync()
                );
            }

            batch.execute();

            Map<Long, Long> counts = new LinkedHashMap<>();
            for (Map.Entry<Long, RFuture<Long>> entry : futures.entrySet()) {
                Long value = entry.getValue().get();
                counts.put(entry.getKey(), value != null ? value : 0L);
            }
            return counts;
        } catch (Exception e) {
            log.error("❌ Redis: Failed to batch read board counts - prefix: {}, size: {}", keyPrefix, boardIds.size(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "Redis 통계 일괄 조회에 실패했습니다: " + e.getMessage());
        }
    }
}
