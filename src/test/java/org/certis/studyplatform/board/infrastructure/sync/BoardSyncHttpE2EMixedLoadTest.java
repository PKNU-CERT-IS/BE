package org.certis.studyplatform.board.infrastructure.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.generated.jooq.tables.Board;
import org.certis.generated.jooq.tables.Member;
import org.certis.studyplatform.auth.domain.model.vo.AccessTokenVo;
import org.certis.studyplatform.board.domain.model.vo.BoardIdVo;
import org.certis.studyplatform.board.domain.model.vo.BoardRedisDeltaVo;
import org.certis.studyplatform.board.domain.repository.BoardQueryRepository;
import org.certis.studyplatform.board.domain.repository.BoardRedisRepository;
import org.certis.studyplatform.board.infrastructure.persistence.BoardCommandRepositoryImpl;
import org.certis.studyplatform.config.RedisTestRedissonConfig;
import org.certis.studyplatform.config.RedisTestServerConfig;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.shared.security.JwtTokenProvider;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.reset;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "logging.level.org.certis.studyplatform=ERROR",
                "logging.level.org.springframework=ERROR",
                "logging.level.root=ERROR",
                "spring.jpa.show-sql=false"
        }
)
@Import({
        TestEmbeddedPostgresConfig.class,
        RedisTestServerConfig.class,
        RedisTestRedissonConfig.class
})
@ActiveProfiles("redis-test")
class BoardSyncHttpE2EMixedLoadTest {

    private static final long AUTHOR_ID = 1L;
    private static final long BOARD_ID = 1L;
    private static final long SECOND_BOARD_ID = 2L;
    private static final long ADMIN_ID = 99_999L;
    private static final int LOGICAL_USERS = 10_000;
    private static final int HALF_USERS = LOGICAL_USERS / 2;
    private static final long BASE_DELTA = 1_000_000L;
    private static final long HALF_BASE_DELTA = BASE_DELTA / 2;
    private static final long MIXED_TOTAL = BASE_DELTA + LOGICAL_USERS;
    private static final long MIXED_HALF = HALF_BASE_DELTA + HALF_USERS;
    private static final int HTTP_WORKERS = 200;
    private static final Path REPORT_PATH = Path.of("output", "board-sync-http-e2e-mixed-10000-users-1000000-delta-results.md");

    @LocalServerPort
    private int port;

    @Autowired private DSLContext dsl;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BoardRedisRepository boardRedisRepository;
    @Autowired private BoardQueryRepository boardQueryRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @SpyBean private BoardCommandRepositoryImpl boardCommandRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @BeforeEach
    void setUp() throws Exception {
        reset(boardCommandRepository);

        truncate("board_view");
        truncate("board_like");
        truncate("board");
        truncate("member");
        restartSequence("board_id_seq");
        restartSequence("member_id_seq");

        clearRedis(BOARD_ID);
        clearRedis(SECOND_BOARD_ID);

        createAuthor();
        createBoard(BOARD_ID, "http-e2e-mixed-board-1");
        createBoard(SECOND_BOARD_ID, "http-e2e-mixed-board-2");

        Files.createDirectories(REPORT_PATH.getParent());
    }

    @Test
    @DisplayName("HTTP E2E를 포함한 게시판 통계 mixed load 검증")
    void measure_http_e2e_mixed_load() throws Exception {
        StringBuilder report = new StringBuilder();
        report.append("# Board Sync HTTP E2E Mixed Load Test\n\n");
        report.append("- logical users: `10,000`\n");
        report.append("- base delta volume: `1,000,000`\n");
        report.append("- mixed total load units: `1,010,000`\n");
        report.append("- http user flow: `GET /detail/{id} + POST /like/{id}`\n");
        report.append("- control flow: `POST /admin/sync + GET /search`\n");
        report.append("- profile: `redis-test`\n\n");

        HttpE2EScenarioResult postSync = runPostSyncDisplayMixedHttpScenario();
        appendScenario(report, postSync);

        setUp();
        HttpE2EScenarioResult overlap = runSyncWhileWritingMixedHttpScenario();
        appendScenario(report, overlap);

        setUp();
        HttpE2EScenarioResult partialFailure = runPartialFailureRetentionMixedHttpScenario();
        appendScenario(report, partialFailure);

        Files.writeString(
                REPORT_PATH,
                report.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private HttpE2EScenarioResult runPostSyncDisplayMixedHttpScenario() throws Exception {
        BoardIdVo boardId = BoardIdVo.of(BOARD_ID);

        long seedStarted = System.nanoTime();
        boardRedisRepository.setLikeCount(boardId, BASE_DELTA);
        boardRedisRepository.setViewCount(boardId, BASE_DELTA);
        long baseSeedMs = toMillis(seedStarted);

        long userTrafficMs = runHttpUserTraffic(BOARD_ID, 10_000L, LOGICAL_USERS);

        long syncStarted = System.nanoTime();
        postAdminSync(generateAdminToken());
        long syncMs = toMillis(syncStarted);

        long readStarted = System.nanoTime();
        SearchSnapshot snapshot = fetchSearchSnapshot(BOARD_ID);
        long readMs = toMillis(readStarted);

        Long dbLike = boardQueryRepository.getLikeCountFromDB(boardId);
        Long dbView = boardQueryRepository.getViewCountFromDB(boardId);
        BoardRedisDeltaVo delta = boardRedisRepository.getDeltaSnapshot(boardId);

        assertThat(snapshot.likeCount()).isEqualTo(MIXED_TOTAL);
        assertThat(snapshot.viewCount()).isEqualTo(MIXED_TOTAL);
        assertThat(dbLike).isEqualTo(MIXED_TOTAL);
        assertThat(dbView).isEqualTo(MIXED_TOTAL);
        assertThat(delta.totalLikeDelta()).isZero();
        assertThat(delta.totalViewDelta()).isZero();

        return new HttpE2EScenarioResult(
                "post_sync_display_http_mixed",
                "1,000,000 base delta 위에 10,000명의 실제 HTTP detail/like 요청이 겹친 상태에서 admin sync 이후 search 표시값이 누적 total과 일치하는지 검증",
                MIXED_TOTAL,
                LOGICAL_USERS * 2,
                2,
                baseSeedMs + userTrafficMs,
                syncMs,
                readMs,
                List.of(
                        "base delta like/view = 1,000,000 / 1,000,000",
                        "http detail requests = 10,000",
                        "http like requests = 10,000",
                        "search display like/view = 1,010,000 / 1,010,000",
                        "db like/view = 1,010,000 / 1,010,000",
                        "redis delta like/view = 0 / 0"
                )
        );
    }

    private HttpE2EScenarioResult runSyncWhileWritingMixedHttpScenario() throws Exception {
        BoardIdVo boardId = BoardIdVo.of(BOARD_ID);

        long preloadStarted = System.nanoTime();
        boardRedisRepository.setLikeCount(boardId, HALF_BASE_DELTA);
        boardRedisRepository.setViewCount(boardId, HALF_BASE_DELTA);
        long basePreloadMs = toMillis(preloadStarted);

        long beforeTrafficMs = runHttpUserTraffic(BOARD_ID, 20_000L, HALF_USERS);

        CountDownLatch updateEntered = new CountDownLatch(1);
        CountDownLatch releaseUpdate = new CountDownLatch(1);

        doAnswer(invocation -> {
            updateEntered.countDown();
            if (!releaseUpdate.await(120, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out while waiting to release update");
            }
            return invocation.callRealMethod();
        }).when(boardCommandRepository).updateBoardStats(any());

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        long syncStarted = System.nanoTime();
        CompletableFuture<Void> syncFuture = CompletableFuture.runAsync(() -> {
            try {
                postAdminSync(generateAdminToken());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);

        assertThat(updateEntered.await(120, TimeUnit.SECONDS)).isTrue();

        long overlapSeedStarted = System.nanoTime();
        boardRedisRepository.setLikeCount(boardId, HALF_BASE_DELTA);
        boardRedisRepository.setViewCount(boardId, HALF_BASE_DELTA);
        long overlapBaseSeedMs = toMillis(overlapSeedStarted);

        long duringTrafficMs = runHttpUserTraffic(BOARD_ID, 30_000L, HALF_USERS);

        releaseUpdate.countDown();
        syncFuture.get(120, TimeUnit.SECONDS);
        long syncMs = toMillis(syncStarted);
        executor.shutdown();

        long readStarted = System.nanoTime();
        SearchSnapshot snapshot = fetchSearchSnapshot(BOARD_ID);
        long readMs = toMillis(readStarted);

        Long dbLike = boardQueryRepository.getLikeCountFromDB(boardId);
        Long dbView = boardQueryRepository.getViewCountFromDB(boardId);
        BoardRedisDeltaVo delta = boardRedisRepository.getDeltaSnapshot(boardId);

        assertThat(dbLike).isEqualTo(MIXED_HALF);
        assertThat(dbView).isEqualTo(MIXED_HALF);
        assertThat(delta.activeLikeCount()).isEqualTo(MIXED_HALF);
        assertThat(delta.activeViewCount()).isEqualTo(MIXED_HALF);
        assertThat(delta.flushLikeCount()).isZero();
        assertThat(delta.flushViewCount()).isZero();
        assertThat(snapshot.likeCount()).isEqualTo(MIXED_TOTAL);
        assertThat(snapshot.viewCount()).isEqualTo(MIXED_TOTAL);

        doCallRealMethod().when(boardCommandRepository).updateBoardStats(any());

        return new HttpE2EScenarioResult(
                "sync_while_writing_http_mixed",
                "1,000,000 base delta와 10,000명의 실제 HTTP detail/like 요청이 sync 전/중에 함께 들어와도 admin sync 경계에서 유실되지 않는지 검증",
                MIXED_TOTAL,
                LOGICAL_USERS * 2,
                2,
                basePreloadMs + beforeTrafficMs + overlapBaseSeedMs + duringTrafficMs,
                syncMs,
                readMs,
                List.of(
                        "before-half mixed delta = 505,000 / 505,000",
                        "during-half mixed delta = 505,000 / 505,000",
                        "db like/view after sync = 505,000 / 505,000",
                        "redis active like/view after sync = 505,000 / 505,000",
                        "search display like/view = 1,010,000 / 1,010,000"
                )
        );
    }

    private HttpE2EScenarioResult runPartialFailureRetentionMixedHttpScenario() throws Exception {
        BoardIdVo successBoard = BoardIdVo.of(BOARD_ID);
        BoardIdVo failedBoard = BoardIdVo.of(SECOND_BOARD_ID);

        long seedStarted = System.nanoTime();
        boardRedisRepository.setLikeCount(successBoard, HALF_BASE_DELTA);
        boardRedisRepository.setViewCount(successBoard, HALF_BASE_DELTA);
        boardRedisRepository.setLikeCount(failedBoard, HALF_BASE_DELTA);
        boardRedisRepository.setViewCount(failedBoard, HALF_BASE_DELTA);
        long baseSeedMs = toMillis(seedStarted);

        long successTrafficMs = runHttpUserTraffic(BOARD_ID, 40_000L, HALF_USERS);
        long failedTrafficMs = runHttpUserTraffic(SECOND_BOARD_ID, 50_000L, HALF_USERS);

        doAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof org.certis.studyplatform.board.domain.model.vo.BoardStatsUpdateVo vo
                    && vo.boardId().equals(SECOND_BOARD_ID)) {
                throw new IllegalStateException("Simulated board sync failure");
            }
            return invocation.callRealMethod();
        }).when(boardCommandRepository).updateBoardStats(any());

        long syncStarted = System.nanoTime();
        postAdminSync(generateAdminToken());
        long syncMs = toMillis(syncStarted);

        long readStarted = System.nanoTime();
        Map<Long, SearchSnapshot> snapshots = fetchSearchSnapshots(List.of(BOARD_ID, SECOND_BOARD_ID));
        long readMs = toMillis(readStarted);

        BoardRedisDeltaVo successDelta = boardRedisRepository.getDeltaSnapshot(successBoard);
        BoardRedisDeltaVo failedDelta = boardRedisRepository.getDeltaSnapshot(failedBoard);
        Long successDbLike = boardQueryRepository.getLikeCountFromDB(successBoard);
        Long successDbView = boardQueryRepository.getViewCountFromDB(successBoard);
        Long failedDbLike = boardQueryRepository.getLikeCountFromDB(failedBoard);
        Long failedDbView = boardQueryRepository.getViewCountFromDB(failedBoard);

        assertThat(successDbLike).isEqualTo(MIXED_HALF);
        assertThat(successDbView).isEqualTo(MIXED_HALF);
        assertThat(successDelta.totalLikeDelta()).isZero();
        assertThat(successDelta.totalViewDelta()).isZero();
        assertThat(failedDbLike).isZero();
        assertThat(failedDbView).isZero();
        assertThat(failedDelta.flushLikeCount()).isEqualTo(MIXED_HALF);
        assertThat(failedDelta.flushViewCount()).isEqualTo(MIXED_HALF);
        assertThat(snapshots.get(BOARD_ID).likeCount()).isEqualTo(MIXED_HALF);
        assertThat(snapshots.get(BOARD_ID).viewCount()).isEqualTo(MIXED_HALF);
        assertThat(snapshots.get(SECOND_BOARD_ID).likeCount()).isEqualTo(MIXED_HALF);
        assertThat(snapshots.get(SECOND_BOARD_ID).viewCount()).isEqualTo(MIXED_HALF);

        doCallRealMethod().when(boardCommandRepository).updateBoardStats(any());

        return new HttpE2EScenarioResult(
                "partial_failure_retention_http_mixed",
                "1,000,000 base delta와 10,000명의 실제 HTTP detail/like 요청이 두 board에 분산된 상태에서 한 board sync 실패 시 mixed flush가 유지되는지 검증",
                MIXED_TOTAL,
                LOGICAL_USERS * 2,
                2,
                baseSeedMs + successTrafficMs + failedTrafficMs,
                syncMs,
                readMs,
                List.of(
                        "success board db like/view = 505,000 / 505,000",
                        "failed board db like/view = 0 / 0",
                        "failed board flush like/view retained = 505,000 / 505,000",
                        "search display success board = 505,000 / 505,000",
                        "search display failed board = 505,000 / 505,000"
                )
        );
    }

    private long runHttpUserTraffic(Long boardId, long userIdStart, int userCount) throws Exception {
        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();

        long started = System.nanoTime();
        try (ExecutorService executor = Executors.newFixedThreadPool(HTTP_WORKERS)) {
            CountDownLatch done = new CountDownLatch(userCount);

            for (long offset = 0; offset < userCount; offset++) {
                long userId = userIdStart + offset;
                executor.submit(() -> {
                    try {
                        String token = generateAccessToken(userId, "board.user." + userId, "user" + userId + "@certis.org",
                                "Board User " + userId, MemberRole.PLAYER);
                        getBoardDetail(boardId, token);
                        toggleLike(boardId, token);
                    } catch (Throwable t) {
                        failures.add(t);
                    } finally {
                        done.countDown();
                    }
                });
            }

            assertThat(done.await(180, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }

        if (!failures.isEmpty()) {
            Throwable first = failures.peek();
            throw new IllegalStateException("HTTP user traffic failed", first);
        }

        return toMillis(started);
    }

    private void getBoardDetail(Long boardId, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/board/detail/" + boardId))
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .header("Authorization", "Bearer " + token)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .withFailMessage("Board detail request failed for board %s: %s", boardId, response.body())
                .isEqualTo(200);
    }

    private void toggleLike(Long boardId, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/board/like/" + boardId))
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .withFailMessage("Board like request failed for board %s: %s", boardId, response.body())
                .isEqualTo(200);
    }

    private void postAdminSync(String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/board/admin/sync"))
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(120))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .withFailMessage("Admin sync failed: %s", response.body())
                .isEqualTo(200);
    }

    private SearchSnapshot fetchSearchSnapshot(Long boardId) throws Exception {
        return fetchSearchSnapshots(List.of(boardId)).get(boardId);
    }

    private Map<Long, SearchSnapshot> fetchSearchSnapshots(List<Long> boardIds) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/board/search?page=0&size=10"))
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .withFailMessage("Board search failed: %s", response.body())
                .isEqualTo(200);

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode content = root.path("data").path("content");
        assertThat(content.isArray()).isTrue();

        java.util.LinkedHashMap<Long, SearchSnapshot> result = new java.util.LinkedHashMap<>();
        for (JsonNode node : content) {
            long boardId = node.path("boardId").asLong();
            if (boardIds.contains(boardId)) {
                result.put(boardId, new SearchSnapshot(
                        boardId,
                        node.path("likeCount").asLong(),
                        node.path("viewCount").asLong()
                ));
            }
        }

        assertThat(result.keySet()).containsAll(boardIds);
        return result;
    }

    private String generateAdminToken() {
        return generateAccessToken(ADMIN_ID, "board.admin", "board-admin@certis.org", "Board Admin", MemberRole.ADMIN);
    }

    private String generateAccessToken(Long userId, String username, String email, String name, MemberRole role) {
        AccessTokenVo token = jwtTokenProvider.generateAccessToken(userId, username, email, name, role);
        return token.value();
    }

    private void appendScenario(StringBuilder report, HttpE2EScenarioResult result) {
        report.append("## ").append(result.name()).append("\n\n");
        report.append("- description: ").append(result.description()).append("\n");
        report.append("- load units: `").append(result.loadUnits()).append("`\n");
        report.append("- user http requests: `").append(result.userHttpRequests()).append("`\n");
        report.append("- control http requests: `").append(result.controlHttpRequests()).append("`\n");
        report.append("- write/traffic duration: `").append(result.trafficMs()).append(" ms`\n");
        report.append("- sync duration: `").append(result.syncMs()).append(" ms`\n");
        report.append("- read duration: `").append(result.readMs()).append(" ms`\n");
        for (String observation : result.observations()) {
            report.append("- ").append(observation).append("\n");
        }
        report.append("\n");
    }

    private void createAuthor() {
        dsl.insertInto(Member.MEMBER)
                .set(Member.MEMBER.ID, AUTHOR_ID)
                .set(Member.MEMBER.NAME, "http-e2e-author")
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
                .set(Board.BOARD.CONTENT, "http-e2e-content-" + boardId)
                .set(Board.BOARD.DESCRIPTION, "http-e2e-description-" + boardId)
                .set(Board.BOARD.CATEGORY, "TECH")
                .set(Board.BOARD.CREATED_AT, OffsetDateTime.now())
                .set(Board.BOARD.UPDATED_AT, OffsetDateTime.now())
                .execute();

        boardRedisRepository.initializeStats(BoardIdVo.of(boardId));
    }

    private void clearRedis(Long boardId) {
        try {
            boardRedisRepository.deleteStats(BoardIdVo.of(boardId));
        } catch (Exception ignored) {
        }
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

    private record SearchSnapshot(
            Long boardId,
            Long likeCount,
            Long viewCount
    ) {
    }

    private record HttpE2EScenarioResult(
            String name,
            String description,
            long loadUnits,
            int userHttpRequests,
            int controlHttpRequests,
            long trafficMs,
            long syncMs,
            long readMs,
            List<String> observations
    ) {
    }
}
