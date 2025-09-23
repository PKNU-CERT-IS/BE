package org.certis.studyplatform.board.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.board.domain.model.vo.BoardIdVo;
import org.certis.studyplatform.board.domain.repository.BoardRedisRepository;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RKeys;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class BoardRedisRepositoryImpl implements BoardRedisRepository {

    private final RedissonClient redissonClient;

    private static final String LIKE_COUNT_PREFIX = "board:like:count:";
    private static final String LIKE_MEMBERS_PREFIX = "board:like:members:";
    private static final String VIEW_COUNT_PREFIX = "board:view:count:";
    private static final String VIEW_MEMBERS_PREFIX = "board:view:members:";

    @Override
    public void initializeStats(BoardIdVo boardIdVo) {
        try {
            String boardId = boardIdVo.value().toString();

            RAtomicLong likeCount = redissonClient.getAtomicLong(LIKE_COUNT_PREFIX + boardId);
            RSet<Long> likeMembers = redissonClient.getSet(LIKE_MEMBERS_PREFIX + boardId);

            likeCount.set(0L);
            likeMembers.clear();

            RAtomicLong viewCount = redissonClient.getAtomicLong(VIEW_COUNT_PREFIX + boardId);
            RSet<Long> viewMembers = redissonClient.getSet(VIEW_MEMBERS_PREFIX + boardId);

            viewCount.set(0L);
            viewMembers.clear();

        } catch (Exception e) {
            log.error("❌ Redis: Failed to initialize stats for board: {}", boardIdVo.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "Redis 통계 초기화에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public void deleteStats(BoardIdVo boardIdVo) {
        try {
            String boardId = boardIdVo.value().toString();

            RKeys keys = redissonClient.getKeys();

            keys.delete(LIKE_COUNT_PREFIX + boardId);
            keys.delete(LIKE_MEMBERS_PREFIX + boardId);
            keys.delete(VIEW_COUNT_PREFIX + boardId);
            keys.delete(VIEW_MEMBERS_PREFIX + boardId);

        } catch (Exception e) {
            log.error("❌ Redis: Failed to delete stats for board: {}", boardIdVo.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "Redis 통계 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public void addLike(BoardIdVo boardId, Long memberId) {
        try {
            String boardIdStr = boardId.value().toString();

            RAtomicLong likeCount = redissonClient.getAtomicLong(LIKE_COUNT_PREFIX + boardIdStr);
            RSet<Long> likeMembers = redissonClient.getSet(LIKE_MEMBERS_PREFIX + boardIdStr);

            if (!likeMembers.contains(memberId)) {
                likeMembers.add(memberId);
                likeCount.incrementAndGet();
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
            String boardIdStr = boardId.value().toString();

            RAtomicLong likeCount = redissonClient.getAtomicLong(LIKE_COUNT_PREFIX + boardIdStr);
            RSet<Long> likeMembers = redissonClient.getSet(LIKE_MEMBERS_PREFIX + boardIdStr);

            if (likeMembers.contains(memberId)) {
                likeMembers.remove(memberId);
                likeCount.decrementAndGet();
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
            String boardIdStr = boardId.value().toString();
            RSet<Long> likeMembers = redissonClient.getSet(LIKE_MEMBERS_PREFIX + boardIdStr);

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
            String boardIdStr = boardId.value().toString();
            RAtomicLong likeCount = redissonClient.getAtomicLong(LIKE_COUNT_PREFIX + boardIdStr);

            return likeCount.get();

        } catch (Exception e) {
            log.error("❌ Redis: Failed to get like count for board: {}", boardId.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "좋아요 수 조회에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public void addView(BoardIdVo boardId, Long viewerId) {
        try {
            String boardIdStr = boardId.value().toString();

            RAtomicLong viewCount = redissonClient.getAtomicLong(VIEW_COUNT_PREFIX + boardIdStr);
            RSet<Long> viewMembers = redissonClient.getSet(VIEW_MEMBERS_PREFIX + boardIdStr);

            if (!viewMembers.contains(viewerId)) {
                viewMembers.add(viewerId);
                viewCount.incrementAndGet();
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
            String boardIdStr = boardId.value().toString();
            RSet<Long> viewMembers = redissonClient.getSet(VIEW_MEMBERS_PREFIX + boardIdStr);

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
            String boardIdStr = boardId.value().toString();
            RAtomicLong viewCount = redissonClient.getAtomicLong(VIEW_COUNT_PREFIX + boardIdStr);

            return viewCount.get();

        } catch (Exception e) {
            log.error("❌ Redis: Failed to get view count for board: {}", boardId.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "조회수 조회에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public void incrementViewCount(BoardIdVo boardId) {
        try {
            String boardIdStr = boardId.value().toString();
            RAtomicLong viewCount = redissonClient.getAtomicLong(VIEW_COUNT_PREFIX + boardIdStr);
            
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
            String boardIdStr = boardId.value().toString();
            RAtomicLong likeCount = redissonClient.getAtomicLong(LIKE_COUNT_PREFIX + boardIdStr);
            
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
            String boardIdStr = boardId.value().toString();
            RAtomicLong viewCount = redissonClient.getAtomicLong(VIEW_COUNT_PREFIX + boardIdStr);
            
            viewCount.set(count);
            
            log.debug("✅ Redis: Set view count for board {} to {}", boardId.value(), count);
        } catch (Exception e) {
            log.error("❌ Redis: Failed to set view count for board: {}", boardId.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_REDIS_ERROR,
                    "조회수 설정에 실패했습니다: " + e.getMessage());
        }
    }
}