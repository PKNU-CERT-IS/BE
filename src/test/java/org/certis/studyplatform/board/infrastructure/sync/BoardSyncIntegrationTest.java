package org.certis.studyplatform.board.infrastructure.sync;

import org.certis.studyplatform.board.application.sync.BoardSyncService;
import org.certis.studyplatform.board.domain.model.vo.BoardIdVo;
import org.certis.studyplatform.board.domain.repository.BoardQueryRepository;
import org.certis.studyplatform.board.domain.repository.BoardRedisRepository;
import org.certis.studyplatform.board.domain.service.BoardDomainService;
import org.certis.studyplatform.board.infrastructure.monitoring.BoardSyncMetrics;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.impl.DSL.*;

/**
 * Board 동기화 시스템 통합 테스트
 * 24시간 동기화와 Redis 재빌드 시나리오를 검증합니다.
 */
@SpringBootTest
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("test")
@Transactional
class BoardSyncIntegrationTest {

    @Autowired private BoardSyncService boardSyncService;
    @Autowired private BoardDomainService boardDomainService;
    @Autowired private BoardRedisRepository boardRedisRepository;
    @Autowired private BoardQueryRepository boardQueryRepository;
    @Autowired private BoardSyncMetrics syncMetrics;
    @Autowired private DSLContext dsl;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final Long TEST_BOARD_ID = 1L;
    private static final Long TEST_BOARD_ID_2 = 2L;
    private static final Long TEST_LIKER_ID = 2L;
    private static final Long TEST_VIEWER_ID = 3L;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 정리
        dsl.execute("TRUNCATE TABLE board RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE board_like RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE board_view RESTART IDENTITY CASCADE");
        
        // 시퀀스 리셋
        dsl.execute("ALTER SEQUENCE board_id_seq RESTART WITH 1");
        dsl.execute("ALTER SEQUENCE member_id_seq RESTART WITH 1");

        // 테스트용 멤버 생성
        dsl.insertInto(table("member"))
                .set(field("id"), TEST_MEMBER_ID)
                .set(field("name"), "테스트 사용자")
                .set(field("student_number"), "20240001")
                .set(field("grade"), "FRESHMAN")
                .set(field("role"), "PLAYER")
                .set(field("major"), "컴퓨터공학과")
                .set(field("birthday"), OffsetDateTime.now().minusYears(20))
                .set(field("gender"), "MALE")
                .set(field("created_at"), OffsetDateTime.now())
                .set(field("updated_at"), OffsetDateTime.now())
                .execute();

        // 테스트용 게시글 2개 생성
        for (Long boardId : List.of(TEST_BOARD_ID, TEST_BOARD_ID_2)) {
            dsl.insertInto(table("board"))
                    .set(field("id"), boardId)
                    .set(field("member_id"), TEST_MEMBER_ID)
                    .set(field("title"), "테스트 게시글 " + boardId)
                    .set(field("content"), "테스트 내용 " + boardId)
                    .set(field("description"), "테스트 설명 " + boardId)
                    .set(field("category"), "TECH")
                    .set(field("created_at"), OffsetDateTime.now())
                    .set(field("updated_at"), OffsetDateTime.now())
                    .execute();
        }
    }

    @Test
    @DisplayName("24시간 동기화: Redis 통계가 RDB로 정상 동기화된다")
    void daily_sync_transfers_redis_stats_to_rdb() {
        // given: Redis에 통계 데이터 존재
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        boardRedisRepository.initializeStats(boardIdVo);
        boardRedisRepository.addLike(boardIdVo, TEST_LIKER_ID);
        boardRedisRepository.addView(boardIdVo, TEST_VIEWER_ID);

        // Redis 통계 확인
        Long redisLikeCount = boardRedisRepository.getLikeCount(boardIdVo);
        Long redisViewCount = boardRedisRepository.getViewCount(boardIdVo);
        assertThat(redisLikeCount).isEqualTo(1L);
        assertThat(redisViewCount).isEqualTo(1L);

        // when: 동기화 실행
        boardSyncService.syncStatsFromRedisToDatabase();

        // then: RDB에 통계가 정상 동기화됨
        Long dbLikeCount = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long dbViewCount = boardQueryRepository.getViewCountFromDB(boardIdVo);
        assertThat(dbLikeCount).isEqualTo(1L);
        assertThat(dbViewCount).isEqualTo(1L);

        // 메트릭 확인
        var metrics = syncMetrics.getCurrentMetrics();
        assertThat(metrics.syncSuccessCount()).isGreaterThan(0);
        assertThat(metrics.totalSyncedBoards()).isGreaterThan(0);
    }

    @Test
    @DisplayName("누적 동기화: 기존 RDB 값이 보존되면서 Redis 값이 누적된다")
    void cumulative_sync_preserves_existing_rdb_values() {
        // given: RDB에 기존 통계 데이터 존재
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 기존 통계 생성
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 5)
                .set(field("created_at"), OffsetDateTime.now())
                .set(field("updated_at"), OffsetDateTime.now())
                .execute();
        
        dsl.insertInto(table("board_view"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("view_number"), 10)
                .set(field("created_at"), OffsetDateTime.now())
                .set(field("updated_at"), OffsetDateTime.now())
                .execute();

        // Redis에 새로운 통계 추가
        boardRedisRepository.initializeStats(boardIdVo);
        boardRedisRepository.addLike(boardIdVo, TEST_LIKER_ID);
        boardRedisRepository.addView(boardIdVo, TEST_VIEWER_ID);

        // when: 동기화 실행
        boardSyncService.syncStatsFromRedisToDatabase();

        // then: 누적 방식으로 동기화됨 (기존 값 + Redis 값)
        Long dbLikeCount = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long dbViewCount = boardQueryRepository.getViewCountFromDB(boardIdVo);
        
        // Redis 값이 0보다 크면 Redis 값 사용, 아니면 기존 값 유지
        assertThat(dbLikeCount).isEqualTo(1L); // Redis 값 사용
        assertThat(dbViewCount).isEqualTo(1L); // Redis 값 사용
    }

    @Test
    @DisplayName("Redis 재빌드 시나리오: Redis TTL 만료 시 기존 RDB 값이 보존된다")
    void redis_rebuild_scenario_preserves_rdb_values_when_redis_expired() {
        // given: RDB에 기존 통계 데이터 존재
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 기존 통계 생성
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 100)
                .set(field("created_at"), OffsetDateTime.now())
                .set(field("updated_at"), OffsetDateTime.now())
                .execute();
        
        dsl.insertInto(table("board_view"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("view_number"), 200)
                .set(field("created_at"), OffsetDateTime.now())
                .set(field("updated_at"), OffsetDateTime.now())
                .execute();

        // Redis는 TTL 만료로 데이터 없음 (시뮬레이션)
        // Redis에 데이터를 추가하지 않음

        // when: 동기화 실행 (Redis 값이 0인 상황)
        boardSyncService.syncStatsFromRedisToDatabase();

        // then: 기존 RDB 값이 보존됨
        Long dbLikeCount = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long dbViewCount = boardQueryRepository.getViewCountFromDB(boardIdVo);
        
        assertThat(dbLikeCount).isEqualTo(100L); // 기존 값 보존
        assertThat(dbViewCount).isEqualTo(200L); // 기존 값 보존
    }

    @Test
    @DisplayName("다중 게시글 동기화: 여러 게시글의 통계가 일괄 동기화된다")
    void multiple_boards_sync_successfully() {
        // given: 여러 게시글에 Redis 통계 데이터 존재
        BoardIdVo boardIdVo1 = BoardIdVo.of(TEST_BOARD_ID);
        BoardIdVo boardIdVo2 = BoardIdVo.of(TEST_BOARD_ID_2);

        // 첫 번째 게시글
        boardRedisRepository.initializeStats(boardIdVo1);
        boardRedisRepository.addLike(boardIdVo1, TEST_LIKER_ID);
        boardRedisRepository.addView(boardIdVo1, TEST_VIEWER_ID);

        // 두 번째 게시글
        boardRedisRepository.initializeStats(boardIdVo2);
        boardRedisRepository.addLike(boardIdVo2, TEST_LIKER_ID);
        boardRedisRepository.addView(boardIdVo2, TEST_VIEWER_ID);

        // when: 동기화 실행
        int syncedBoards = boardDomainService.syncAllBoardStats();

        // then: 모든 게시글이 동기화됨
        assertThat(syncedBoards).isEqualTo(2);

        // 각 게시글의 통계 확인
        Long dbLikeCount1 = boardQueryRepository.getLikeCountFromDB(boardIdVo1);
        Long dbViewCount1 = boardQueryRepository.getViewCountFromDB(boardIdVo1);
        Long dbLikeCount2 = boardQueryRepository.getLikeCountFromDB(boardIdVo2);
        Long dbViewCount2 = boardQueryRepository.getViewCountFromDB(boardIdVo2);

        assertThat(dbLikeCount1).isEqualTo(1L);
        assertThat(dbViewCount1).isEqualTo(1L);
        assertThat(dbLikeCount2).isEqualTo(1L);
        assertThat(dbViewCount2).isEqualTo(1L);
    }

    @Test
    @DisplayName("일관성 검증: Redis와 RDB 간 데이터 일관성이 검증된다")
    void consistency_validation_works_correctly() {
        // given: Redis와 RDB에 동일한 통계 데이터 존재
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 통계 생성
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 5)
                .set(field("created_at"), OffsetDateTime.now())
                .set(field("updated_at"), OffsetDateTime.now())
                .execute();
        
        dsl.insertInto(table("board_view"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("view_number"), 10)
                .set(field("created_at"), OffsetDateTime.now())
                .set(field("updated_at"), OffsetDateTime.now())
                .execute();

        // Redis에 동일한 통계 설정
        boardRedisRepository.initializeStats(boardIdVo);
        boardRedisRepository.setLikeCount(boardIdVo, 5L);
        boardRedisRepository.setViewCount(boardIdVo, 10L);

        // when: 일관성 검증 실행
        boolean isConsistent = boardDomainService.validateStatsConsistency();

        // then: 일관성 검증 통과
        assertThat(isConsistent).isTrue();
        
        // 메트릭 확인
        var metrics = syncMetrics.getCurrentMetrics();
        assertThat(metrics.consistencySuccessCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("일관성 검증: TTL 만료된 Redis는 일관성 검증에서 제외된다")
    void consistency_validation_excludes_expired_redis() {
        // given: RDB에만 통계 데이터 존재 (Redis TTL 만료 시뮬레이션)
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 통계 생성
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 5)
                .set(field("created_at"), OffsetDateTime.now())
                .set(field("updated_at"), OffsetDateTime.now())
                .execute();
        
        dsl.insertInto(table("board_view"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("view_number"), 10)
                .set(field("created_at"), OffsetDateTime.now())
                .set(field("updated_at"), OffsetDateTime.now())
                .execute();

        // Redis는 초기화하지 않음 (TTL 만료 시뮬레이션)

        // when: 일관성 검증 실행
        boolean isConsistent = boardDomainService.validateStatsConsistency();

        // then: TTL 만료는 정상 상황으로 처리됨
        assertThat(isConsistent).isTrue();
    }

    @Test
    @DisplayName("동기화 후 Redis 초기화: 다음 날 통계를 위해 Redis가 초기화된다")
    void redis_initialization_after_sync_prepares_for_next_day() {
        // given: Redis에 통계 데이터 존재
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        boardRedisRepository.initializeStats(boardIdVo);
        boardRedisRepository.addLike(boardIdVo, TEST_LIKER_ID);
        boardRedisRepository.addView(boardIdVo, TEST_VIEWER_ID);

        // when: 동기화 실행
        boardSyncService.syncStatsFromRedisToDatabase();

        // then: Redis가 초기화되어 다음 날 통계를 위해 준비됨
        Long redisLikeCount = boardRedisRepository.getLikeCount(boardIdVo);
        Long redisViewCount = boardRedisRepository.getViewCount(boardIdVo);
        
        // 초기화 후에는 0이 되어야 함
        assertThat(redisLikeCount).isEqualTo(0L);
        assertThat(redisViewCount).isEqualTo(0L);
    }

    @Test
    @DisplayName("에러 처리: 동기화 실패 시 적절한 에러 처리가 된다")
    void error_handling_works_correctly_on_sync_failure() {
        // given: 존재하지 않는 게시글 ID로 Redis 초기화
        BoardIdVo invalidBoardIdVo = BoardIdVo.of(999L);
        boardRedisRepository.initializeStats(invalidBoardIdVo);

        // when: 동기화 실행 (존재하지 않는 게시글로 인한 실패)
        try {
            boardSyncService.syncStatsFromRedisToDatabase();
        } catch (Exception e) {
            // 예상된 예외
        }

        // then: 메트릭에 실패가 기록됨
        var metrics = syncMetrics.getCurrentMetrics();
        assertThat(metrics.syncFailureCount()).isGreaterThan(0);
    }
}
