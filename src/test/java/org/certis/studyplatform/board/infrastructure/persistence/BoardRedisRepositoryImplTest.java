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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
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
        given(longSet.add(memberId)).willReturn(true);

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
        given(longSet.add(viewerId)).willReturn(true);

        repository.addView(boardId, viewerId);

        then(longSet).should(times(1)).add(viewerId);
        then(atomicLong).should(times(1)).incrementAndGet();
    }

    @Test
    @DisplayName("조회 추가: contains 사전조회 없이 SADD만 호출")
    void addView_NoContainsPrecheck() {
        BoardIdVo boardId = BoardIdVo.of(1L);
        Long viewerId = 10L;

        given(redissonClient.getAtomicLong(anyString())).willReturn(atomicLong);
        given(redissonClient.getSet(anyString())).willReturn(longSet);
        given(longSet.add(viewerId)).willReturn(true);

        repository.addView(boardId, viewerId);

        then(longSet).should(times(0)).contains(any());
        then(longSet).should(times(1)).add(viewerId);
        then(atomicLong).should(times(1)).incrementAndGet();
    }

    @Test
    @DisplayName("좋아요 중복 3회: 한 번만 카운트 증가")
    void addLike_ThreeTimes_IncrementsOnce() {
        BoardIdVo boardId = BoardIdVo.of(2L);
        Long memberId = 20L;

        given(redissonClient.getAtomicLong(anyString())).willReturn(atomicLong);
        given(redissonClient.getSet(anyString())).willReturn(longSet);
        // 첫 호출에만 추가되고, 이후 두 번은 이미 존재
        given(longSet.add(memberId)).willReturn(true, false, false);

        repository.addLike(boardId, memberId);
        repository.addLike(boardId, memberId);
        repository.addLike(boardId, memberId);

        then(longSet).should(times(3)).add(memberId);
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
        RAtomicLong likeActive = org.mockito.Mockito.mock(RAtomicLong.class);
        RAtomicLong likeFlush = org.mockito.Mockito.mock(RAtomicLong.class);
        RAtomicLong viewActive = org.mockito.Mockito.mock(RAtomicLong.class);
        RAtomicLong viewFlush = org.mockito.Mockito.mock(RAtomicLong.class);

        given(redissonClient.getAtomicLong(BoardStatRedisKeys.likeActive(boardId.value()))).willReturn(likeActive);
        given(redissonClient.getAtomicLong(BoardStatRedisKeys.likeFlush(boardId.value()))).willReturn(likeFlush);
        given(redissonClient.getAtomicLong(BoardStatRedisKeys.viewActive(boardId.value()))).willReturn(viewActive);
        given(redissonClient.getAtomicLong(BoardStatRedisKeys.viewFlush(boardId.value()))).willReturn(viewFlush);
        given(likeActive.get()).willReturn(3L);
        given(likeFlush.get()).willReturn(2L);
        given(viewActive.get()).willReturn(0L);
        given(viewFlush.get()).willReturn(0L);

        Long result = repository.getLikeCount(boardId);

        assertThat(result).isEqualTo(5L);
        then(likeActive).should(times(1)).get();
        then(likeFlush).should(times(1)).get();
    }

    @Test
    @DisplayName("좋아요 추가: contains 사전조회 없이 SADD만 호출")
    void addLike_NoContainsPrecheck() {
        BoardIdVo boardId = BoardIdVo.of(3L);
        Long memberId = 30L;

        given(redissonClient.getAtomicLong(anyString())).willReturn(atomicLong);
        given(redissonClient.getSet(anyString())).willReturn(longSet);
        given(longSet.add(memberId)).willReturn(true);

        repository.addLike(boardId, memberId);

        then(longSet).should(times(0)).contains(any());
        then(longSet).should(times(1)).add(memberId);
        then(atomicLong).should(times(1)).incrementAndGet();
    }

    @Test
    @DisplayName("조회 중복 100회: 한 번만 카운트 증가")
    void addView_OneHundredTimes_IncrementsOnce() {
        BoardIdVo boardId = BoardIdVo.of(4L);
        Long viewerId = 40L;

        given(redissonClient.getAtomicLong(anyString())).willReturn(atomicLong);
        given(redissonClient.getSet(anyString())).willReturn(longSet);

        final java.util.concurrent.atomic.AtomicInteger callCount = new java.util.concurrent.atomic.AtomicInteger(0);
        given(longSet.add(viewerId)).willAnswer(invocation -> callCount.getAndIncrement() == 0);

        for (int i = 0; i < 100; i++) {
            repository.addView(boardId, viewerId);
        }

        then(longSet).should(times(100)).add(viewerId);
        then(atomicLong).should(times(1)).incrementAndGet();
    }

    @Test
    @DisplayName("좋아요 중복 100회: 한 번만 카운트 증가")
    void addLike_OneHundredTimes_IncrementsOnce() {
        BoardIdVo boardId = BoardIdVo.of(5L);
        Long memberId = 50L;

        given(redissonClient.getAtomicLong(anyString())).willReturn(atomicLong);
        given(redissonClient.getSet(anyString())).willReturn(longSet);

        final java.util.concurrent.atomic.AtomicInteger callCount = new java.util.concurrent.atomic.AtomicInteger(0);
        given(longSet.add(memberId)).willAnswer(invocation -> callCount.getAndIncrement() == 0);

        for (int i = 0; i < 100; i++) {
            repository.addLike(boardId, memberId);
        }

        then(longSet).should(times(100)).add(memberId);
        then(atomicLong).should(times(1)).incrementAndGet();
    }

    @Test
    @DisplayName("조회수 조회: Redis에서 카운트 반환")
    void getViewCount_ReturnsCountFromRedis() {
        BoardIdVo boardId = BoardIdVo.of(1L);
        RAtomicLong likeActive = org.mockito.Mockito.mock(RAtomicLong.class);
        RAtomicLong likeFlush = org.mockito.Mockito.mock(RAtomicLong.class);
        RAtomicLong viewActive = org.mockito.Mockito.mock(RAtomicLong.class);
        RAtomicLong viewFlush = org.mockito.Mockito.mock(RAtomicLong.class);

        given(redissonClient.getAtomicLong(BoardStatRedisKeys.likeActive(boardId.value()))).willReturn(likeActive);
        given(redissonClient.getAtomicLong(BoardStatRedisKeys.likeFlush(boardId.value()))).willReturn(likeFlush);
        given(redissonClient.getAtomicLong(BoardStatRedisKeys.viewActive(boardId.value()))).willReturn(viewActive);
        given(redissonClient.getAtomicLong(BoardStatRedisKeys.viewFlush(boardId.value()))).willReturn(viewFlush);
        given(likeActive.get()).willReturn(0L);
        given(likeFlush.get()).willReturn(0L);
        given(viewActive.get()).willReturn(70L);
        given(viewFlush.get()).willReturn(30L);

        Long result = repository.getViewCount(boardId);

        assertThat(result).isEqualTo(100L);
        then(viewActive).should(times(1)).get();
        then(viewFlush).should(times(1)).get();
    }
}
