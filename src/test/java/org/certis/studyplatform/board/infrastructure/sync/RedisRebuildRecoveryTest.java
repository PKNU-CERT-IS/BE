package org.certis.studyplatform.board.infrastructure.sync;

import org.certis.studyplatform.board.application.sync.BoardSyncService;
import org.certis.studyplatform.board.domain.model.vo.BoardIdVo;
import org.certis.studyplatform.board.domain.repository.BoardQueryRepository;
import org.certis.studyplatform.board.domain.repository.BoardRedisRepository;
import org.certis.studyplatform.board.domain.service.BoardDomainService;
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
 * Redis 재빌드 시나리오 테스트
 * Redis가 완전히 재빌드되었을 때 RDB로부터 데이터를 복구하는 기능을 검증합니다.
 */
@SpringBootTest
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("test")
@Transactional
class RedisRebuildRecoveryTest {

    @Autowired private BoardSyncService boardSyncService;
    @Autowired private BoardDomainService boardDomainService;
    @Autowired private BoardRedisRepository boardRedisRepository;
    @Autowired private BoardQueryRepository boardQueryRepository;
    @Autowired private DSLContext dsl;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final Long TEST_BOARD_ID = 1L;
    private static final Long TEST_BOARD_ID_2 = 2L;
    private static final Long TEST_BOARD_ID_3 = 3L;
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

        // 테스트용 게시글 3개 생성
        for (Long boardId : List.of(TEST_BOARD_ID, TEST_BOARD_ID_2, TEST_BOARD_ID_3)) {
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
    @DisplayName("Redis 재빌드 시나리오 1: RDB에만 데이터가 있을 때 데이터 손실 없이 보존된다")
    void redis_rebuild_scenario1_preserves_rdb_data_when_redis_empty() {
        // given: RDB에만 통계 데이터 존재 (Redis 재빌드로 인한 데이터 손실 시뮬레이션)
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 기존 통계 생성 (과거 동기화로 누적된 데이터)
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 150) // 과거 누적된 좋아요 수
                .execute();
        
        dsl.insertInto(table("board_view"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("view_number"), 300) // 과거 누적된 조회수
                .execute();

        // Redis는 완전히 비어있음 (재빌드 후 상태)

        // when: 동기화 실행
        boardSyncService.syncStatsFromRedisToDatabase();

        // then: 기존 RDB 데이터가 보존됨 (데이터 손실 없음)
        Long dbLikeCount = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long dbViewCount = boardQueryRepository.getViewCountFromDB(boardIdVo);
        
        assertThat(dbLikeCount).isEqualTo(150L); // 기존 값 보존
        assertThat(dbViewCount).isEqualTo(300L); // 기존 값 보존
    }

    @Test
    @DisplayName("Redis 재빌드 시나리오 2: Redis 재빌드 후 새로운 활동이 있을 때 누적된다")
    void redis_rebuild_scenario2_accumulates_new_activity_after_rebuild() {
        // given: RDB에 기존 데이터, Redis 재빌드 후 새로운 활동
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 기존 통계 생성
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 100)
                .execute();
        
        dsl.insertInto(table("board_view"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("view_number"), 200)
                .execute();

        // Redis 재빌드 후 새로운 활동 (새로운 좋아요/조회수)
        boardRedisRepository.initializeStats(boardIdVo);
        boardRedisRepository.addLike(boardIdVo, TEST_LIKER_ID);
        boardRedisRepository.addView(boardIdVo, TEST_VIEWER_ID);

        // when: 동기화 실행
        boardSyncService.syncStatsFromRedisToDatabase();

        // then: 새로운 활동이 반영됨
        Long dbLikeCount = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long dbViewCount = boardQueryRepository.getViewCountFromDB(boardIdVo);
        
        assertThat(dbLikeCount).isEqualTo(1L); // 새로운 Redis 값 사용
        assertThat(dbViewCount).isEqualTo(1L); // 새로운 Redis 값 사용
    }

    @Test
    @DisplayName("Redis 재빌드 시나리오 3: 여러 게시글의 복합 상황을 처리한다")
    void redis_rebuild_scenario3_handles_multiple_boards_complex_situation() {
        // given: 여러 게시글의 다양한 상황
        BoardIdVo boardIdVo1 = BoardIdVo.of(TEST_BOARD_ID);
        BoardIdVo boardIdVo2 = BoardIdVo.of(TEST_BOARD_ID_2);
        BoardIdVo boardIdVo3 = BoardIdVo.of(TEST_BOARD_ID_3);

        // 게시글 1: RDB에만 데이터 (Redis 재빌드로 손실)
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 50)
                .execute();
        
        dsl.insertInto(table("board_view"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("view_number"), 100)
                .execute();

        // 게시글 2: Redis 재빌드 후 새로운 활동
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID_2)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 25)
                .execute();
        
        boardRedisRepository.initializeStats(boardIdVo2);
        boardRedisRepository.addLike(boardIdVo2, TEST_LIKER_ID);
        boardRedisRepository.addView(boardIdVo2, TEST_VIEWER_ID);

        // 게시글 3: Redis와 RDB 모두 비어있음 (새로운 게시글)
        // Redis 초기화만 수행

        // when: 동기화 실행
        int syncedBoards = boardDomainService.syncAllBoardStats();

        // then: 모든 게시글이 적절히 처리됨
        assertThat(syncedBoards).isEqualTo(3);

        // 게시글 1: 기존 RDB 값 보존
        Long dbLikeCount1 = boardQueryRepository.getLikeCountFromDB(boardIdVo1);
        Long dbViewCount1 = boardQueryRepository.getViewCountFromDB(boardIdVo1);
        assertThat(dbLikeCount1).isEqualTo(50L);
        assertThat(dbViewCount1).isEqualTo(100L);

        // 게시글 2: 새로운 Redis 값 사용
        Long dbLikeCount2 = boardQueryRepository.getLikeCountFromDB(boardIdVo2);
        Long dbViewCount2 = boardQueryRepository.getViewCountFromDB(boardIdVo2);
        assertThat(dbLikeCount2).isEqualTo(1L);
        assertThat(dbViewCount2).isEqualTo(1L);

        // 게시글 3: 0으로 초기화됨
        Long dbLikeCount3 = boardQueryRepository.getLikeCountFromDB(boardIdVo3);
        Long dbViewCount3 = boardQueryRepository.getViewCountFromDB(boardIdVo3);
        assertThat(dbLikeCount3).isEqualTo(0L);
        assertThat(dbViewCount3).isEqualTo(0L);
    }

    @Test
    @DisplayName("Redis 재빌드 시나리오 4: 연속적인 동기화에서 데이터 일관성이 유지된다")
    void redis_rebuild_scenario4_maintains_consistency_across_multiple_syncs() {
        // given: 초기 상태 - RDB에 데이터 존재
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 100)
                .execute();
        
        dsl.insertInto(table("board_view"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("view_number"), 200)
                .execute();

        // 첫 번째 동기화: Redis 비어있음 (재빌드 직후)
        boardSyncService.syncStatsFromRedisToDatabase();
        
        Long afterFirstSyncLike = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long afterFirstSyncView = boardQueryRepository.getViewCountFromDB(boardIdVo);
        assertThat(afterFirstSyncLike).isEqualTo(100L); // 기존 값 보존
        assertThat(afterFirstSyncView).isEqualTo(200L); // 기존 값 보존

        // 새로운 활동 발생
        boardRedisRepository.initializeStats(boardIdVo);
        boardRedisRepository.addLike(boardIdVo, TEST_LIKER_ID);
        boardRedisRepository.addView(boardIdVo, TEST_VIEWER_ID);

        // 두 번째 동기화: 새로운 활동 반영
        boardSyncService.syncStatsFromRedisToDatabase();
        
        Long afterSecondSyncLike = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long afterSecondSyncView = boardQueryRepository.getViewCountFromDB(boardIdVo);
        assertThat(afterSecondSyncLike).isEqualTo(1L); // 새로운 값 사용
        assertThat(afterSecondSyncView).isEqualTo(1L); // 새로운 값 사용

        // 세 번째 동기화: Redis 초기화 후 (다음 날)
        boardSyncService.syncStatsFromRedisToDatabase();
        
        Long afterThirdSyncLike = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long afterThirdSyncView = boardQueryRepository.getViewCountFromDB(boardIdVo);
        assertThat(afterThirdSyncLike).isEqualTo(1L); // 이전 값 유지
        assertThat(afterThirdSyncView).isEqualTo(1L); // 이전 값 유지
    }

    @Test
    @DisplayName("Redis 재빌드 시나리오 5: 일관성 검증이 Redis 재빌드 상황을 올바르게 처리한다")
    void redis_rebuild_scenario5_consistency_validation_handles_rebuild_correctly() {
        // given: RDB에만 데이터 존재 (Redis 재빌드로 인한 상황)
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 75)
                .execute();
        
        dsl.insertInto(table("board_view"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("view_number"), 150)
                .execute();

        // Redis는 비어있음 (재빌드 후 상태)

        // when: 일관성 검증 실행
        boolean isConsistent = boardDomainService.validateStatsConsistency();

        // then: Redis 재빌드 상황은 정상으로 처리됨
        assertThat(isConsistent).isTrue();
    }

    @Test
    @DisplayName("Redis 재빌드 시나리오 6: 대용량 데이터에서도 안정적으로 동작한다")
    void redis_rebuild_scenario6_handles_large_data_stably() {
        // given: 대용량 게시글 데이터 생성
        int boardCount = 100;
        
        for (int i = 1; i <= boardCount; i++) {
            // 게시글 생성
            dsl.insertInto(table("board"))
                    .set(field("id"), (long) i)
                    .set(field("member_id"), TEST_MEMBER_ID)
                    .set(field("title"), "대용량 테스트 게시글 " + i)
                    .set(field("content"), "대용량 테스트 내용 " + i)
                    .set(field("description"), "대용량 테스트 설명 " + i)
                    .set(field("category"), "TECH")
                    .set(field("created_at"), OffsetDateTime.now())
                    .set(field("updated_at"), OffsetDateTime.now())
                    .execute();
            
            // RDB에 통계 생성
            dsl.insertInto(table("board_like"))
                    .set(field("board_id"), (long) i)
                    .set(field("member_id"), TEST_MEMBER_ID)
                    .set(field("like_number"), i * 10)
                    .execute();
            
            dsl.insertInto(table("board_view"))
                    .set(field("board_id"), (long) i)
                    .set(field("view_number"), i * 20)
                    .execute();
        }

        // Redis는 비어있음 (재빌드 후 상태)

        // when: 동기화 실행
        long startTime = System.currentTimeMillis();
        int syncedBoards = boardDomainService.syncAllBoardStats();
        long duration = System.currentTimeMillis() - startTime;

        // then: 모든 게시글이 안정적으로 동기화됨
        assertThat(syncedBoards).isEqualTo(boardCount);
        assertThat(duration).isLessThan(5000); // 5초 이내 완료

        // 샘플 검증 (첫 번째와 마지막 게시글)
        BoardIdVo firstBoard = BoardIdVo.of(1L);
        BoardIdVo lastBoard = BoardIdVo.of((long) boardCount);
        
        Long firstLikeCount = boardQueryRepository.getLikeCountFromDB(firstBoard);
        Long firstViewCount = boardQueryRepository.getViewCountFromDB(firstBoard);
        Long lastLikeCount = boardQueryRepository.getLikeCountFromDB(lastBoard);
        Long lastViewCount = boardQueryRepository.getViewCountFromDB(lastBoard);
        
        assertThat(firstLikeCount).isEqualTo(10L);
        assertThat(firstViewCount).isEqualTo(20L);
        assertThat(lastLikeCount).isEqualTo((long) boardCount * 10);
        assertThat(lastViewCount).isEqualTo((long) boardCount * 20);
    }
}
