package org.certis.studyplatform.board.infrastructure.sync;

import org.certis.generated.jooq.tables.Board;
import org.certis.generated.jooq.tables.Member;
import org.certis.studyplatform.board.application.object.query.SearchBoardsQuery;
import org.certis.studyplatform.board.application.sync.BoardSyncService;
import org.certis.studyplatform.board.domain.model.vo.BoardIdVo;
import org.certis.studyplatform.board.domain.model.vo.BoardRedisDeltaVo;
import org.certis.studyplatform.board.domain.model.vo.BoardSummaryVo;
import org.certis.studyplatform.board.domain.repository.BoardQueryRepository;
import org.certis.studyplatform.board.domain.repository.BoardRedisRepository;
import org.certis.studyplatform.board.domain.service.BoardDomainService;
import org.certis.studyplatform.board.infrastructure.persistence.BoardCommandRepositoryImpl;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.reset;

@SpringBootTest
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("redis-test")
class BoardSync10000LoadSimulationTest {

    private static final long AUTHOR_ID = 1L;
    private static final long BOARD_ID = 1L;
    private static final long SECOND_BOARD_ID = 2L;
    private static final int LOGICAL_USERS = 10_000;
    private static final long ONE_MILLION_DELTA = 1_000_000L;
    private static final long MIXED_TOTAL_DELTA = ONE_MILLION_DELTA + LOGICAL_USERS;
    private static final long MIXED_PER_BOARD_DELTA = (ONE_MILLION_DELTA / 2) + (LOGICAL_USERS / 2);
    private static final Path REPORT_PATH = Path.of("output", "board-sync-10000-load-results.md");
    private static final Path ONE_MILLION_REPORT_PATH = Path.of("output", "board-sync-1000000-load-results.md");
    private static final Path MIXED_REPORT_PATH = Path.of("output", "board-sync-mixed-10000-users-1000000-delta-results.md");

    @Autowired private BoardSyncService boardSyncService;
    @Autowired private BoardDomainService boardDomainService;
    @Autowired private BoardRedisRepository boardRedisRepository;
    @Autowired private BoardQueryRepository boardQueryRepository;
    @Autowired private DSLContext dsl;

    @SpyBean private BoardCommandRepositoryImpl boardCommandRepository;

    @BeforeEach
    void setUp() throws Exception {
        reset(boardCommandRepository);

        truncate("board_view");
        truncate("board_like");
        truncate("board");
        truncate("member");
        restartSequence("board_id_seq");
        restartSequence("member_id_seq");

        try {
            boardRedisRepository.deleteStats(BoardIdVo.of(BOARD_ID));
        } catch (Exception ignored) {
        }
        try {
            boardRedisRepository.deleteStats(BoardIdVo.of(SECOND_BOARD_ID));
        } catch (Exception ignored) {
        }

        createAuthor();
        createBoard(BOARD_ID, "load-test-board-1");
        createBoard(SECOND_BOARD_ID, "load-test-board-2");

        Files.createDirectories(REPORT_PATH.getParent());
    }

    @Test
    @DisplayName("10,000명 가정 게시판 동기화 3개 시나리오 부하 시뮬레이션")
    void simulate_board_sync_under_10000_logical_users() throws Exception {
        StringBuilder report = new StringBuilder();
        report.append("# Board Sync 10,000 Load Simulation\n\n");
        report.append("- logical users: `10,000`\n");
        report.append("- profile: `redis-test`\n");
        report.append("- scope: `post_sync_display`, `sync_while_writing`, `partial_failure_retention`\n\n");

        ScenarioResult postSync = runPostSyncDisplayScenario();
        appendScenario(report, postSync);

        setUp();
        ScenarioResult overlap = runSyncWhileWritingScenario();
        appendScenario(report, overlap);

        setUp();
        ScenarioResult partialFailure = runPartialFailureRetentionScenario();
        appendScenario(report, partialFailure);

        Files.writeString(
                REPORT_PATH,
                report.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    @Test
    @DisplayName("1,000,000건 규모 게시판 동기화 3개 시나리오 부하 시뮬레이션")
    void simulate_board_sync_under_1000000_delta_volume() throws Exception {
        StringBuilder report = new StringBuilder();
        report.append("# Board Sync 1,000,000 Delta Volume Simulation\n\n");
        report.append("- simulated delta volume: `1,000,000`\n");
        report.append("- profile: `redis-test`\n");
        report.append("- note: unique user set 부하가 아니라 counter/delta volume 기준 시뮬레이션\n");
        report.append("- scope: `post_sync_display`, `sync_while_writing`, `partial_failure_retention`\n\n");

        ScenarioResult postSync = runPostSyncDisplayMillionScenario();
        appendScenario(report, postSync);

        setUp();
        ScenarioResult overlap = runSyncWhileWritingMillionScenario();
        appendScenario(report, overlap);

        setUp();
        ScenarioResult partialFailure = runPartialFailureRetentionMillionScenario();
        appendScenario(report, partialFailure);

        Files.writeString(
                ONE_MILLION_REPORT_PATH,
                report.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    @Test
    @DisplayName("10,000명 논리 사용자와 1,000,000 delta volume이 동시에 겹치는 게시판 동기화 시뮬레이션")
    void simulate_board_sync_under_mixed_logical_users_and_delta_volume() throws Exception {
        StringBuilder report = new StringBuilder();
        report.append("# Board Sync Mixed Load Simulation\n\n");
        report.append("- logical users: `10,000`\n");
        report.append("- base delta volume: `1,000,000`\n");
        report.append("- profile: `redis-test`\n");
        report.append("- note: direct delta seed와 실제 addLike/addView 호출을 같은 시나리오에 겹쳐 검증\n");
        report.append("- scope: `post_sync_display_mixed`, `sync_while_writing_mixed`, `partial_failure_retention_mixed`\n\n");

        ScenarioResult postSync = runPostSyncDisplayMixedScenario();
        appendScenario(report, postSync);

        setUp();
        ScenarioResult overlap = runSyncWhileWritingMixedScenario();
        appendScenario(report, overlap);

        setUp();
        ScenarioResult partialFailure = runPartialFailureRetentionMixedScenario();
        appendScenario(report, partialFailure);

        Files.writeString(
                MIXED_REPORT_PATH,
                report.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private ScenarioResult runPostSyncDisplayScenario() throws Exception {
        BoardIdVo boardId = BoardIdVo.of(BOARD_ID);

        long injectMs = runConcurrentUsers(LOGICAL_USERS, userId -> {
            boardRedisRepository.addLike(boardId, userId);
            boardRedisRepository.addView(boardId, userId);
        });

        long syncStarted = System.nanoTime();
        boardSyncService.syncStatsFromRedisToDatabase();
        long syncMs = toMillis(syncStarted);

        long readStarted = System.nanoTime();
        BoardSummaryVo summary = fetchBoardSummary(BOARD_ID);
        long readMs = toMillis(readStarted);

        Long dbLike = boardQueryRepository.getLikeCountFromDB(boardId);
        Long dbView = boardQueryRepository.getViewCountFromDB(boardId);
        BoardRedisDeltaVo delta = boardRedisRepository.getDeltaSnapshot(boardId);

        assertThat(summary.likeCount()).isEqualTo((long) LOGICAL_USERS);
        assertThat(summary.viewCount()).isEqualTo((long) LOGICAL_USERS);
        assertThat(dbLike).isEqualTo((long) LOGICAL_USERS);
        assertThat(dbView).isEqualTo((long) LOGICAL_USERS);
        assertThat(delta.totalLikeDelta()).isZero();
        assertThat(delta.totalViewDelta()).isZero();

        return new ScenarioResult(
                "post_sync_display",
                "sync 직후에도 표시값이 누적 total과 일치하는지 검증",
                LOGICAL_USERS,
                injectMs,
                syncMs,
                readMs,
                List.of(
                        "display like/view = 10,000 / 10,000",
                        "db like/view = 10,000 / 10,000",
                        "redis delta like/view = 0 / 0"
                )
        );
    }

    private ScenarioResult runSyncWhileWritingScenario() throws Exception {
        BoardIdVo boardId = BoardIdVo.of(BOARD_ID);
        int beforeSyncUsers = LOGICAL_USERS / 2;
        int duringSyncUsers = LOGICAL_USERS - beforeSyncUsers;

        long preloadMs = runConcurrentUsers(beforeSyncUsers, userId -> {
            boardRedisRepository.addLike(boardId, userId);
            boardRedisRepository.addView(boardId, userId);
        });

        CountDownLatch updateEntered = new CountDownLatch(1);
        CountDownLatch releaseUpdate = new CountDownLatch(1);

        doAnswer(invocation -> {
            updateEntered.countDown();
            if (!releaseUpdate.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out while waiting to release update");
            }
            return invocation.callRealMethod();
        }).when(boardCommandRepository).updateBoardStats(any());

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        long syncStarted = System.nanoTime();
        CompletableFuture<Integer> syncFuture = CompletableFuture.supplyAsync(
                () -> boardDomainService.syncBoardStats(List.of(BOARD_ID)),
                executor
        );

        assertThat(updateEntered.await(30, TimeUnit.SECONDS)).isTrue();

        long overlapWriteMs = runConcurrentUsers(duringSyncUsers, offset -> {
            long userId = beforeSyncUsers + offset;
            boardRedisRepository.addLike(boardId, userId);
            boardRedisRepository.addView(boardId, userId);
        });

        releaseUpdate.countDown();
        assertThat(syncFuture.get(30, TimeUnit.SECONDS)).isEqualTo(1);
        long syncMs = toMillis(syncStarted);
        executor.shutdown();

        BoardRedisDeltaVo delta = boardRedisRepository.getDeltaSnapshot(boardId);
        BoardSummaryVo summary = fetchBoardSummary(BOARD_ID);
        Long dbLike = boardQueryRepository.getLikeCountFromDB(boardId);
        Long dbView = boardQueryRepository.getViewCountFromDB(boardId);

        assertThat(dbLike).isEqualTo((long) beforeSyncUsers);
        assertThat(dbView).isEqualTo((long) beforeSyncUsers);
        assertThat(delta.activeLikeCount()).isEqualTo((long) duringSyncUsers);
        assertThat(delta.activeViewCount()).isEqualTo((long) duringSyncUsers);
        assertThat(delta.flushLikeCount()).isZero();
        assertThat(delta.flushViewCount()).isZero();
        assertThat(summary.likeCount()).isEqualTo((long) LOGICAL_USERS);
        assertThat(summary.viewCount()).isEqualTo((long) LOGICAL_USERS);

        doCallRealMethod().when(boardCommandRepository).updateBoardStats(any());

        return new ScenarioResult(
                "sync_while_writing",
                "sync 중 write가 겹쳐도 in-flight delta가 유실되지 않는지 검증",
                LOGICAL_USERS,
                preloadMs + overlapWriteMs,
                syncMs,
                0L,
                List.of(
                        "db like/view after sync = 5,000 / 5,000",
                        "redis active like/view after sync = 5,000 / 5,000",
                        "display like/view = 10,000 / 10,000"
                )
        );
    }

    private ScenarioResult runPartialFailureRetentionScenario() throws Exception {
        BoardIdVo successBoard = BoardIdVo.of(BOARD_ID);
        BoardIdVo failedBoard = BoardIdVo.of(SECOND_BOARD_ID);
        int perBoardUsers = LOGICAL_USERS / 2;

        AtomicLong successSeedMs = new AtomicLong();
        AtomicLong failureSeedMs = new AtomicLong();

        successSeedMs.set(runConcurrentUsers(perBoardUsers, userId -> {
            boardRedisRepository.addLike(successBoard, userId);
            boardRedisRepository.addView(successBoard, userId);
        }));
        failureSeedMs.set(runConcurrentUsers(perBoardUsers, userId -> {
            long failedUserId = perBoardUsers + userId;
            boardRedisRepository.addLike(failedBoard, failedUserId);
            boardRedisRepository.addView(failedBoard, failedUserId);
        }));

        doAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof org.certis.studyplatform.board.domain.model.vo.BoardStatsUpdateVo vo
                    && vo.boardId().equals(SECOND_BOARD_ID)) {
                throw new IllegalStateException("Simulated board sync failure");
            }
            return invocation.callRealMethod();
        }).when(boardCommandRepository).updateBoardStats(any());

        long syncStarted = System.nanoTime();
        int synced = boardDomainService.syncBoardStats(List.of(BOARD_ID, SECOND_BOARD_ID));
        long syncMs = toMillis(syncStarted);

        BoardRedisDeltaVo successDelta = boardRedisRepository.getDeltaSnapshot(successBoard);
        BoardRedisDeltaVo failedDelta = boardRedisRepository.getDeltaSnapshot(failedBoard);
        Long successDbLike = boardQueryRepository.getLikeCountFromDB(successBoard);
        Long successDbView = boardQueryRepository.getViewCountFromDB(successBoard);
        Long failedDbLike = boardQueryRepository.getLikeCountFromDB(failedBoard);
        Long failedDbView = boardQueryRepository.getViewCountFromDB(failedBoard);

        assertThat(synced).isEqualTo(1);
        assertThat(successDbLike).isEqualTo((long) perBoardUsers);
        assertThat(successDbView).isEqualTo((long) perBoardUsers);
        assertThat(successDelta.totalLikeDelta()).isZero();
        assertThat(successDelta.totalViewDelta()).isZero();
        assertThat(failedDbLike).isZero();
        assertThat(failedDbView).isZero();
        assertThat(failedDelta.flushLikeCount()).isEqualTo((long) perBoardUsers);
        assertThat(failedDelta.flushViewCount()).isEqualTo((long) perBoardUsers);

        doCallRealMethod().when(boardCommandRepository).updateBoardStats(any());

        return new ScenarioResult(
                "partial_failure_retention",
                "일부 board 동기화 실패 시 flush 데이터가 남아 재시도 가능한지 검증",
                LOGICAL_USERS,
                successSeedMs.get() + failureSeedMs.get(),
                syncMs,
                0L,
                List.of(
                        "synced boards = 1 / 2",
                        "success board db like/view = 5,000 / 5,000",
                        "failed board flush like/view retained = 5,000 / 5,000"
                )
        );
    }

    private ScenarioResult runPostSyncDisplayMillionScenario() {
        BoardIdVo boardId = BoardIdVo.of(BOARD_ID);

        long injectStarted = System.nanoTime();
        boardRedisRepository.setLikeCount(boardId, ONE_MILLION_DELTA);
        boardRedisRepository.setViewCount(boardId, ONE_MILLION_DELTA);
        long injectMs = toMillis(injectStarted);

        long syncStarted = System.nanoTime();
        boardSyncService.syncStatsFromRedisToDatabase();
        long syncMs = toMillis(syncStarted);

        long readStarted = System.nanoTime();
        BoardSummaryVo summary = fetchBoardSummary(BOARD_ID);
        long readMs = toMillis(readStarted);

        Long dbLike = boardQueryRepository.getLikeCountFromDB(boardId);
        Long dbView = boardQueryRepository.getViewCountFromDB(boardId);
        BoardRedisDeltaVo delta = boardRedisRepository.getDeltaSnapshot(boardId);

        assertThat(summary.likeCount()).isEqualTo(ONE_MILLION_DELTA);
        assertThat(summary.viewCount()).isEqualTo(ONE_MILLION_DELTA);
        assertThat(dbLike).isEqualTo(ONE_MILLION_DELTA);
        assertThat(dbView).isEqualTo(ONE_MILLION_DELTA);
        assertThat(delta.totalLikeDelta()).isZero();
        assertThat(delta.totalViewDelta()).isZero();

        return new ScenarioResult(
                "post_sync_display_1m",
                "1,000,000건 delta가 sync 직후에도 누적 total과 일치하는지 검증",
                (int) ONE_MILLION_DELTA,
                injectMs,
                syncMs,
                readMs,
                List.of(
                        "display like/view = 1,000,000 / 1,000,000",
                        "db like/view = 1,000,000 / 1,000,000",
                        "redis delta like/view = 0 / 0"
                )
        );
    }

    private ScenarioResult runSyncWhileWritingMillionScenario() throws Exception {
        BoardIdVo boardId = BoardIdVo.of(BOARD_ID);
        long beforeSyncDelta = ONE_MILLION_DELTA / 2;
        long duringSyncDelta = ONE_MILLION_DELTA - beforeSyncDelta;

        long preloadStarted = System.nanoTime();
        boardRedisRepository.setLikeCount(boardId, beforeSyncDelta);
        boardRedisRepository.setViewCount(boardId, beforeSyncDelta);
        long preloadMs = toMillis(preloadStarted);

        CountDownLatch updateEntered = new CountDownLatch(1);
        CountDownLatch releaseUpdate = new CountDownLatch(1);

        doAnswer(invocation -> {
            updateEntered.countDown();
            if (!releaseUpdate.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out while waiting to release update");
            }
            return invocation.callRealMethod();
        }).when(boardCommandRepository).updateBoardStats(any());

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        long syncStarted = System.nanoTime();
        CompletableFuture<Integer> syncFuture = CompletableFuture.supplyAsync(
                () -> boardDomainService.syncBoardStats(List.of(BOARD_ID)),
                executor
        );

        assertThat(updateEntered.await(30, TimeUnit.SECONDS)).isTrue();

        long overlapStarted = System.nanoTime();
        boardRedisRepository.setLikeCount(boardId, duringSyncDelta);
        boardRedisRepository.setViewCount(boardId, duringSyncDelta);
        long overlapMs = toMillis(overlapStarted);

        releaseUpdate.countDown();
        assertThat(syncFuture.get(30, TimeUnit.SECONDS)).isEqualTo(1);
        long syncMs = toMillis(syncStarted);
        executor.shutdown();

        BoardRedisDeltaVo delta = boardRedisRepository.getDeltaSnapshot(boardId);
        BoardSummaryVo summary = fetchBoardSummary(BOARD_ID);
        Long dbLike = boardQueryRepository.getLikeCountFromDB(boardId);
        Long dbView = boardQueryRepository.getViewCountFromDB(boardId);

        assertThat(dbLike).isEqualTo(beforeSyncDelta);
        assertThat(dbView).isEqualTo(beforeSyncDelta);
        assertThat(delta.activeLikeCount()).isEqualTo(duringSyncDelta);
        assertThat(delta.activeViewCount()).isEqualTo(duringSyncDelta);
        assertThat(delta.flushLikeCount()).isZero();
        assertThat(delta.flushViewCount()).isZero();
        assertThat(summary.likeCount()).isEqualTo(ONE_MILLION_DELTA);
        assertThat(summary.viewCount()).isEqualTo(ONE_MILLION_DELTA);

        doCallRealMethod().when(boardCommandRepository).updateBoardStats(any());

        return new ScenarioResult(
                "sync_while_writing_1m",
                "sync 중 1,000,000건 delta가 나뉘어 들어와도 유실되지 않는지 검증",
                (int) ONE_MILLION_DELTA,
                preloadMs + overlapMs,
                syncMs,
                0L,
                List.of(
                        "db like/view after sync = 500,000 / 500,000",
                        "redis active like/view after sync = 500,000 / 500,000",
                        "display like/view = 1,000,000 / 1,000,000"
                )
        );
    }

    private ScenarioResult runPartialFailureRetentionMillionScenario() {
        BoardIdVo successBoard = BoardIdVo.of(BOARD_ID);
        BoardIdVo failedBoard = BoardIdVo.of(SECOND_BOARD_ID);
        long perBoardDelta = ONE_MILLION_DELTA / 2;

        long injectStarted = System.nanoTime();
        boardRedisRepository.setLikeCount(successBoard, perBoardDelta);
        boardRedisRepository.setViewCount(successBoard, perBoardDelta);
        boardRedisRepository.setLikeCount(failedBoard, perBoardDelta);
        boardRedisRepository.setViewCount(failedBoard, perBoardDelta);
        long injectMs = toMillis(injectStarted);

        doAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof org.certis.studyplatform.board.domain.model.vo.BoardStatsUpdateVo vo
                    && vo.boardId().equals(SECOND_BOARD_ID)) {
                throw new IllegalStateException("Simulated board sync failure");
            }
            return invocation.callRealMethod();
        }).when(boardCommandRepository).updateBoardStats(any());

        long syncStarted = System.nanoTime();
        int synced = boardDomainService.syncBoardStats(List.of(BOARD_ID, SECOND_BOARD_ID));
        long syncMs = toMillis(syncStarted);

        BoardRedisDeltaVo successDelta = boardRedisRepository.getDeltaSnapshot(successBoard);
        BoardRedisDeltaVo failedDelta = boardRedisRepository.getDeltaSnapshot(failedBoard);
        Long successDbLike = boardQueryRepository.getLikeCountFromDB(successBoard);
        Long successDbView = boardQueryRepository.getViewCountFromDB(successBoard);
        Long failedDbLike = boardQueryRepository.getLikeCountFromDB(failedBoard);
        Long failedDbView = boardQueryRepository.getViewCountFromDB(failedBoard);

        assertThat(synced).isEqualTo(1);
        assertThat(successDbLike).isEqualTo(perBoardDelta);
        assertThat(successDbView).isEqualTo(perBoardDelta);
        assertThat(successDelta.totalLikeDelta()).isZero();
        assertThat(successDelta.totalViewDelta()).isZero();
        assertThat(failedDbLike).isZero();
        assertThat(failedDbView).isZero();
        assertThat(failedDelta.flushLikeCount()).isEqualTo(perBoardDelta);
        assertThat(failedDelta.flushViewCount()).isEqualTo(perBoardDelta);

        doCallRealMethod().when(boardCommandRepository).updateBoardStats(any());

        return new ScenarioResult(
                "partial_failure_retention_1m",
                "1,000,000건 규모에서도 일부 board 실패 시 flush 데이터가 유지되는지 검증",
                (int) ONE_MILLION_DELTA,
                injectMs,
                syncMs,
                0L,
                List.of(
                        "synced boards = 1 / 2",
                        "success board db like/view = 500,000 / 500,000",
                        "failed board flush like/view retained = 500,000 / 500,000"
                )
        );
    }

    private ScenarioResult runPostSyncDisplayMixedScenario() throws Exception {
        BoardIdVo boardId = BoardIdVo.of(BOARD_ID);

        long injectStarted = System.nanoTime();
        boardRedisRepository.setLikeCount(boardId, ONE_MILLION_DELTA);
        boardRedisRepository.setViewCount(boardId, ONE_MILLION_DELTA);
        long baseInjectMs = toMillis(injectStarted);

        long userInjectMs = runConcurrentUsers(LOGICAL_USERS, userId -> {
            boardRedisRepository.addLike(boardId, userId);
            boardRedisRepository.addView(boardId, userId);
        });

        long syncStarted = System.nanoTime();
        boardSyncService.syncStatsFromRedisToDatabase();
        long syncMs = toMillis(syncStarted);

        long readStarted = System.nanoTime();
        BoardSummaryVo summary = fetchBoardSummary(BOARD_ID);
        long readMs = toMillis(readStarted);

        Long dbLike = boardQueryRepository.getLikeCountFromDB(boardId);
        Long dbView = boardQueryRepository.getViewCountFromDB(boardId);
        BoardRedisDeltaVo delta = boardRedisRepository.getDeltaSnapshot(boardId);

        assertThat(summary.likeCount()).isEqualTo(MIXED_TOTAL_DELTA);
        assertThat(summary.viewCount()).isEqualTo(MIXED_TOTAL_DELTA);
        assertThat(dbLike).isEqualTo(MIXED_TOTAL_DELTA);
        assertThat(dbView).isEqualTo(MIXED_TOTAL_DELTA);
        assertThat(delta.totalLikeDelta()).isZero();
        assertThat(delta.totalViewDelta()).isZero();

        return new ScenarioResult(
                "post_sync_display_mixed",
                "1,000,000 base delta 위에 10,000명 논리 사용자 증가가 겹친 상태에서도 sync 직후 표시값이 누적 total과 일치하는지 검증",
                (int) MIXED_TOTAL_DELTA,
                baseInjectMs + userInjectMs,
                syncMs,
                readMs,
                List.of(
                        "base delta like/view = 1,000,000 / 1,000,000",
                        "logical user delta like/view = 10,000 / 10,000",
                        "display like/view = 1,010,000 / 1,010,000",
                        "db like/view = 1,010,000 / 1,010,000",
                        "redis delta like/view = 0 / 0"
                )
        );
    }

    private ScenarioResult runSyncWhileWritingMixedScenario() throws Exception {
        BoardIdVo boardId = BoardIdVo.of(BOARD_ID);
        long beforeSyncBaseDelta = ONE_MILLION_DELTA / 2;
        long duringSyncBaseDelta = ONE_MILLION_DELTA - beforeSyncBaseDelta;
        int beforeSyncUsers = LOGICAL_USERS / 2;
        int duringSyncUsers = LOGICAL_USERS - beforeSyncUsers;
        long beforeSyncExpected = beforeSyncBaseDelta + beforeSyncUsers;
        long duringSyncExpected = duringSyncBaseDelta + duringSyncUsers;

        long preloadStarted = System.nanoTime();
        boardRedisRepository.setLikeCount(boardId, beforeSyncBaseDelta);
        boardRedisRepository.setViewCount(boardId, beforeSyncBaseDelta);
        long basePreloadMs = toMillis(preloadStarted);

        long userPreloadMs = runConcurrentUsers(beforeSyncUsers, userId -> {
            boardRedisRepository.addLike(boardId, userId);
            boardRedisRepository.addView(boardId, userId);
        });

        CountDownLatch updateEntered = new CountDownLatch(1);
        CountDownLatch releaseUpdate = new CountDownLatch(1);

        doAnswer(invocation -> {
            updateEntered.countDown();
            if (!releaseUpdate.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out while waiting to release update");
            }
            return invocation.callRealMethod();
        }).when(boardCommandRepository).updateBoardStats(any());

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        long syncStarted = System.nanoTime();
        CompletableFuture<Integer> syncFuture = CompletableFuture.supplyAsync(
                () -> boardDomainService.syncBoardStats(List.of(BOARD_ID)),
                executor
        );

        assertThat(updateEntered.await(30, TimeUnit.SECONDS)).isTrue();

        long overlapStarted = System.nanoTime();
        boardRedisRepository.setLikeCount(boardId, duringSyncBaseDelta);
        boardRedisRepository.setViewCount(boardId, duringSyncBaseDelta);
        long baseOverlapMs = toMillis(overlapStarted);

        long userOverlapMs = runConcurrentUsers(duringSyncUsers, offset -> {
            long userId = beforeSyncUsers + offset;
            boardRedisRepository.addLike(boardId, userId);
            boardRedisRepository.addView(boardId, userId);
        });

        releaseUpdate.countDown();
        assertThat(syncFuture.get(30, TimeUnit.SECONDS)).isEqualTo(1);
        long syncMs = toMillis(syncStarted);
        executor.shutdown();

        BoardRedisDeltaVo delta = boardRedisRepository.getDeltaSnapshot(boardId);
        BoardSummaryVo summary = fetchBoardSummary(BOARD_ID);
        Long dbLike = boardQueryRepository.getLikeCountFromDB(boardId);
        Long dbView = boardQueryRepository.getViewCountFromDB(boardId);

        assertThat(dbLike).isEqualTo(beforeSyncExpected);
        assertThat(dbView).isEqualTo(beforeSyncExpected);
        assertThat(delta.activeLikeCount()).isEqualTo(duringSyncExpected);
        assertThat(delta.activeViewCount()).isEqualTo(duringSyncExpected);
        assertThat(delta.flushLikeCount()).isZero();
        assertThat(delta.flushViewCount()).isZero();
        assertThat(summary.likeCount()).isEqualTo(MIXED_TOTAL_DELTA);
        assertThat(summary.viewCount()).isEqualTo(MIXED_TOTAL_DELTA);

        doCallRealMethod().when(boardCommandRepository).updateBoardStats(any());

        return new ScenarioResult(
                "sync_while_writing_mixed",
                "1,000,000 base delta와 10,000명 논리 사용자 증가가 sync 전/중에 함께 나뉘어 들어와도 in-flight delta가 유실되지 않는지 검증",
                (int) MIXED_TOTAL_DELTA,
                basePreloadMs + userPreloadMs + baseOverlapMs + userOverlapMs,
                syncMs,
                0L,
                List.of(
                        "db like/view after sync = 505,000 / 505,000",
                        "redis active like/view after sync = 505,000 / 505,000",
                        "display like/view = 1,010,000 / 1,010,000"
                )
        );
    }

    private ScenarioResult runPartialFailureRetentionMixedScenario() throws Exception {
        BoardIdVo successBoard = BoardIdVo.of(BOARD_ID);
        BoardIdVo failedBoard = BoardIdVo.of(SECOND_BOARD_ID);
        long perBoardBaseDelta = ONE_MILLION_DELTA / 2;
        int perBoardUsers = LOGICAL_USERS / 2;

        long injectStarted = System.nanoTime();
        boardRedisRepository.setLikeCount(successBoard, perBoardBaseDelta);
        boardRedisRepository.setViewCount(successBoard, perBoardBaseDelta);
        boardRedisRepository.setLikeCount(failedBoard, perBoardBaseDelta);
        boardRedisRepository.setViewCount(failedBoard, perBoardBaseDelta);
        long baseInjectMs = toMillis(injectStarted);

        long successUserInjectMs = runConcurrentUsers(perBoardUsers, userId -> {
            boardRedisRepository.addLike(successBoard, userId);
            boardRedisRepository.addView(successBoard, userId);
        });
        long failedUserInjectMs = runConcurrentUsers(perBoardUsers, userId -> {
            long failedUserId = perBoardUsers + userId;
            boardRedisRepository.addLike(failedBoard, failedUserId);
            boardRedisRepository.addView(failedBoard, failedUserId);
        });

        doAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof org.certis.studyplatform.board.domain.model.vo.BoardStatsUpdateVo vo
                    && vo.boardId().equals(SECOND_BOARD_ID)) {
                throw new IllegalStateException("Simulated board sync failure");
            }
            return invocation.callRealMethod();
        }).when(boardCommandRepository).updateBoardStats(any());

        long syncStarted = System.nanoTime();
        int synced = boardDomainService.syncBoardStats(List.of(BOARD_ID, SECOND_BOARD_ID));
        long syncMs = toMillis(syncStarted);

        BoardRedisDeltaVo successDelta = boardRedisRepository.getDeltaSnapshot(successBoard);
        BoardRedisDeltaVo failedDelta = boardRedisRepository.getDeltaSnapshot(failedBoard);
        Long successDbLike = boardQueryRepository.getLikeCountFromDB(successBoard);
        Long successDbView = boardQueryRepository.getViewCountFromDB(successBoard);
        Long failedDbLike = boardQueryRepository.getLikeCountFromDB(failedBoard);
        Long failedDbView = boardQueryRepository.getViewCountFromDB(failedBoard);

        assertThat(synced).isEqualTo(1);
        assertThat(successDbLike).isEqualTo(MIXED_PER_BOARD_DELTA);
        assertThat(successDbView).isEqualTo(MIXED_PER_BOARD_DELTA);
        assertThat(successDelta.totalLikeDelta()).isZero();
        assertThat(successDelta.totalViewDelta()).isZero();
        assertThat(failedDbLike).isZero();
        assertThat(failedDbView).isZero();
        assertThat(failedDelta.flushLikeCount()).isEqualTo(MIXED_PER_BOARD_DELTA);
        assertThat(failedDelta.flushViewCount()).isEqualTo(MIXED_PER_BOARD_DELTA);

        doCallRealMethod().when(boardCommandRepository).updateBoardStats(any());

        return new ScenarioResult(
                "partial_failure_retention_mixed",
                "1,000,000 base delta와 10,000명 논리 사용자 증가가 섞인 상태에서도 일부 board 실패 시 flush 데이터가 남아 재시도 가능한지 검증",
                (int) MIXED_TOTAL_DELTA,
                baseInjectMs + successUserInjectMs + failedUserInjectMs,
                syncMs,
                0L,
                List.of(
                        "synced boards = 1 / 2",
                        "success board db like/view = 505,000 / 505,000",
                        "failed board flush like/view retained = 505,000 / 505,000"
                )
        );
    }

    private BoardSummaryVo fetchBoardSummary(Long boardId) {
        Page<BoardSummaryVo> page = boardDomainService.searchBoards(SearchBoardsQuery.of(null, null, 0, 10));
        return page.getContent().stream()
                .filter(board -> board.id().equals(boardId))
                .findFirst()
                .orElseThrow();
    }

    private long runConcurrentUsers(int userCount, LongConsumer userAction) throws Exception {
        CountDownLatch ready = new CountDownLatch(userCount);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (long userId = 1; userId <= userCount; userId++) {
                final long finalUserId = userId;
                executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    userAction.accept(finalUserId);
                    return null;
                });
            }

            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
            long started = System.nanoTime();
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
            return toMillis(started);
        }
    }

    private void appendScenario(StringBuilder report, ScenarioResult result) {
        report.append("## ").append(result.name()).append("\n\n");
        report.append("- description: ").append(result.description()).append("\n");
        report.append("- load units: `").append(result.logicalUsers()).append("`\n");
        report.append("- write/inject duration: `").append(result.injectMs()).append(" ms`\n");
        report.append("- sync duration: `").append(result.syncMs()).append(" ms`\n");
        if (result.readMs() > 0) {
            report.append("- read duration: `").append(result.readMs()).append(" ms`\n");
        }
        for (String observation : result.observations()) {
            report.append("- ").append(observation).append("\n");
        }
        report.append("\n");
    }

    private void createAuthor() {
        dsl.insertInto(Member.MEMBER)
                .set(Member.MEMBER.ID, AUTHOR_ID)
                .set(Member.MEMBER.NAME, "load-author")
                .set(Member.MEMBER.STUDENT_NUMBER, "20240001")
                .set(Member.MEMBER.GRADE, "FRESHMAN")
                .set(Member.MEMBER.ROLE, "PLAYER")
                .set(Member.MEMBER.MAJOR, "컴퓨터공학과")
                .set(Member.MEMBER.BIRTHDAY, OffsetDateTime.now().minusYears(20))
                .set(Member.MEMBER.GENDER, "MALE")
                .set(Member.MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(Member.MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();
    }

    private void createBoard(Long boardId, String title) {
        dsl.insertInto(Board.BOARD)
                .set(Board.BOARD.ID, boardId)
                .set(Board.BOARD.MEMBER_ID, AUTHOR_ID)
                .set(Board.BOARD.TITLE, title)
                .set(Board.BOARD.CONTENT, "load-test-content-" + boardId)
                .set(Board.BOARD.DESCRIPTION, "load-test-description-" + boardId)
                .set(Board.BOARD.CATEGORY, "TECH")
                .set(Board.BOARD.CREATED_AT, OffsetDateTime.now())
                .set(Board.BOARD.UPDATED_AT, OffsetDateTime.now())
                .execute();

        boardRedisRepository.initializeStats(BoardIdVo.of(boardId));
    }

    private void truncate(String tableName) {
        try {
            dsl.execute("TRUNCATE TABLE " + tableName + " RESTART IDENTITY CASCADE");
        } catch (Exception ignored) {
        }
    }

    private void restartSequence(String sequenceName) {
        try {
            dsl.execute("ALTER SEQUENCE " + sequenceName + " RESTART WITH 1");
        } catch (Exception ignored) {
        }
    }

    private long toMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }

    private record ScenarioResult(
            String name,
            String description,
            int logicalUsers,
            long injectMs,
            long syncMs,
            long readMs,
            List<String> observations
    ) {
    }
}
