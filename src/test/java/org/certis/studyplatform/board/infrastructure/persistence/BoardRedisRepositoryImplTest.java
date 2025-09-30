package org.certis.studyplatform.board.infrastructure.persistence;

import org.certis.studyplatform.board.domain.model.vo.BoardIdVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoardRedisRepositoryImpl 단위 테스트")
class BoardRedisRepositoryImplTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RAtomicLong atomicLong;

    @Mock
    private RSet<Object> longSet;

    @InjectMocks
    private BoardRedisRepositoryImpl repository;

    @Test
    @DisplayName("좋아요 추가: 멤버 미포함이면 추가 및 카운트 증가")
    void addLike_WhenNotContains_AddsAndIncrements() {
        BoardIdVo boardId = BoardIdVo.of(1L);
        Long memberId = 10L;

        given(redissonClient.getAtomicLong(anyString())).willReturn(atomicLong);
        given(redissonClient.getSet(anyString())).willReturn(longSet);
        given(longSet.contains(memberId)).willReturn(false);

        repository.addLike(boardId, memberId);

        then(longSet).should(times(1)).add(memberId);
        then(atomicLong).should(times(1)).incrementAndGet();
    }

    @Test
    @DisplayName("좋아요 제거: 멤버 포함이면 제거 및 카운트 감소")
    void removeLike_WhenContains_RemovesAndDecrements() {
        BoardIdVo boardId = BoardIdVo.of(1L);
        Long memberId = 10L;

        given(redissonClient.getAtomicLong(anyString())).willReturn(atomicLong);
        given(redissonClient.getSet(anyString())).willReturn(longSet);
        given(longSet.contains(memberId)).willReturn(true);

        repository.removeLike(boardId, memberId);

        then(longSet).should(times(1)).remove(memberId);
        then(atomicLong).should(times(1)).decrementAndGet();
    }

    @Test
    @DisplayName("조회 추가: 미조회 사용자면 추가 및 카운트 증가")
    void addView_WhenNotViewed_AddsAndIncrements() {
        BoardIdVo boardId = BoardIdVo.of(1L);
        Long viewerId = 10L;

        given(redissonClient.getAtomicLong(anyString())).willReturn(atomicLong);
        given(redissonClient.getSet(anyString())).willReturn(longSet);
        given(longSet.contains(viewerId)).willReturn(false);

        repository.addView(boardId, viewerId);

        then(longSet).should(times(1)).add(viewerId);
        then(atomicLong).should(times(1)).incrementAndGet();
    }

    @Test
    @DisplayName("조회수 증가: 단순 카운트 증가")
    void incrementViewCount_Increments() {
        BoardIdVo boardId = BoardIdVo.of(1L);

        given(redissonClient.getAtomicLong(anyString())).willReturn(atomicLong);

        repository.incrementViewCount(boardId);

        then(atomicLong).should(times(1)).incrementAndGet();
    }

    @Test
    @DisplayName("좋아요 수 조회: Redis에서 카운트 반환")
    void getLikeCount_ReturnsCountFromRedis() {
        BoardIdVo boardId = BoardIdVo.of(1L);
        Long expectedCount = 5L;

        given(redissonClient.getAtomicLong(anyString())).willReturn(atomicLong);
        given(atomicLong.get()).willReturn(expectedCount);

        repository.getLikeCount(boardId);

        then(atomicLong).should(times(1)).get();
    }

    @Test
    @DisplayName("조회수 조회: Redis에서 카운트 반환")
    void getViewCount_ReturnsCountFromRedis() {
        BoardIdVo boardId = BoardIdVo.of(1L);
        Long expectedCount = 100L;

        given(redissonClient.getAtomicLong(anyString())).willReturn(atomicLong);
        given(atomicLong.get()).willReturn(expectedCount);

        repository.getViewCount(boardId);

        then(atomicLong).should(times(1)).get();
    }
}
