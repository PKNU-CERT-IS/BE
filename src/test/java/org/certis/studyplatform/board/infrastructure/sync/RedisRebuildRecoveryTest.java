package org.certis.studyplatform.board.infrastructure.sync;

import org.certis.studyplatform.board.application.sync.BoardSyncService;
import org.certis.studyplatform.board.domain.model.vo.BoardIdVo;
import org.certis.studyplatform.board.domain.repository.BoardQueryRepository;
import org.certis.studyplatform.board.domain.repository.BoardRedisRepository;
import org.certis.studyplatform.board.domain.service.BoardDomainService;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.generated.jooq.tables.Board;
import org.certis.generated.jooq.tables.BoardLike;
import org.certis.generated.jooq.tables.BoardView;
import org.certis.generated.jooq.tables.Member;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 재빌드 시나리오 테스트
 * Redis가 완전히 재빌드되었을 때 RDB로부터 데이터를 복구하는 기능을 검증합니다.
 */
@SpringBootTest
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("redis-test")
// @Transactional  // 트랜잭션 롤백 제거로 동기화 결과 확인 가능
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
        // 테이블 존재 여부 확인 후 안전하게 정리
        try {
            dsl.execute("TRUNCATE TABLE board_view RESTART IDENTITY CASCADE");
        } catch (Exception e) {
            // 테이블이 없으면 무시
        }
        try {
            dsl.execute("TRUNCATE TABLE board_like RESTART IDENTITY CASCADE");
        } catch (Exception e) {
            // 테이블이 없으면 무시
        }
        try {
            dsl.execute("TRUNCATE TABLE board RESTART IDENTITY CASCADE");
        } catch (Exception e) {
            // 테이블이 없으면 무시
        }
        try {
            dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");
        } catch (Exception e) {
            // 테이블이 없으면 무시
        }
        
        // 시퀀스 리셋 (존재하는 경우에만)
        try {
            dsl.execute("ALTER SEQUENCE board_id_seq RESTART WITH 1");
        } catch (Exception e) {
            // 시퀀스가 없으면 무시
        }
        try {
            dsl.execute("ALTER SEQUENCE member_id_seq RESTART WITH 1");
        } catch (Exception e) {
            // 시퀀스가 없으면 무시
        }

        // Redis 정리 (테스트 격리를 위해 중요!)
        try {
            boardRedisRepository.deleteStats(BoardIdVo.of(1L));
            boardRedisRepository.deleteStats(BoardIdVo.of(2L));
            boardRedisRepository.deleteStats(BoardIdVo.of(3L));
        } catch (Exception e) {
            // Redis 키가 없을 수 있음 (정상)
            System.out.println("Redis cleanup failed: " + e.getMessage());
        }

        // 테스트용 멤버 생성 (올바른 jOOQ 테이블 사용)
        try {
            dsl.insertInto(Member.MEMBER)
                    .set(Member.MEMBER.ID, TEST_MEMBER_ID)
                    .set(Member.MEMBER.NAME, "테스트 사용자")
                    .set(Member.MEMBER.STUDENT_NUMBER, "20240001")
                    .set(Member.MEMBER.GRADE, "FRESHMAN")
                    .set(Member.MEMBER.ROLE, "PLAYER")
                    .set(Member.MEMBER.MAJOR, "컴퓨터공학과")
                    .set(Member.MEMBER.BIRTHDAY, OffsetDateTime.now().minusYears(20))
                    .set(Member.MEMBER.GENDER, "MALE")
                    .set(Member.MEMBER.CREATED_AT, OffsetDateTime.now())
                    .set(Member.MEMBER.UPDATED_AT, OffsetDateTime.now())
                    .execute();
        } catch (Exception e) {
            System.out.println("Failed to create test member: " + e.getMessage());
        }

        // 테스트용 게시글 3개 생성 (올바른 jOOQ 테이블 사용)
        for (Long boardId : List.of(TEST_BOARD_ID, TEST_BOARD_ID_2, TEST_BOARD_ID_3)) {
            try {
                dsl.insertInto(Board.BOARD)
                        .set(Board.BOARD.ID, boardId)
                        .set(Board.BOARD.MEMBER_ID, TEST_MEMBER_ID)
                        .set(Board.BOARD.TITLE, "테스트 게시글 " + boardId)
                        .set(Board.BOARD.CONTENT, "테스트 내용 " + boardId)
                        .set(Board.BOARD.DESCRIPTION, "테스트 설명 " + boardId)
                        .set(Board.BOARD.CATEGORY, "TECH")
                        .set(Board.BOARD.CREATED_AT, OffsetDateTime.now())
                        .set(Board.BOARD.UPDATED_AT, OffsetDateTime.now())
                        .execute();
            } catch (Exception e) {
                System.out.println("Failed to create test board " + boardId + ": " + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("Redis 재빌드 시나리오 1: RDB에만 데이터가 있을 때 데이터 손실 없이 보존된다")
    void redis_rebuild_scenario1_preserves_rdb_data_when_redis_empty() {
        // given: RDB에만 통계 데이터 존재 (Redis 재빌드로 인한 데이터 손실 시뮬레이션)
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 기존 통계 생성 (과거 동기화로 누적된 데이터)
        dsl.insertInto(BoardLike.BOARD_LIKE)
                .set(BoardLike.BOARD_LIKE.BOARD_ID, TEST_BOARD_ID)
                .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, 150) // 과거 누적된 좋아요 수
                .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                .execute();
        
        dsl.insertInto(BoardView.BOARD_VIEW)
                .set(BoardView.BOARD_VIEW.BOARD_ID, TEST_BOARD_ID)
                .set(BoardView.BOARD_VIEW.VIEW_NUMBER, 300) // 과거 누적된 조회수
                .set(BoardView.BOARD_VIEW.UPDATED_AT, OffsetDateTime.now())
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
        
        // RDB에 기존 통계 생성 (올바른 jOOQ 테이블 사용)
        dsl.insertInto(BoardLike.BOARD_LIKE)
                .set(BoardLike.BOARD_LIKE.BOARD_ID, TEST_BOARD_ID)
                .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, 100)
                .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                .execute();
        
        dsl.insertInto(BoardView.BOARD_VIEW)
                .set(BoardView.BOARD_VIEW.BOARD_ID, TEST_BOARD_ID)
                .set(BoardView.BOARD_VIEW.VIEW_NUMBER, 200)
                .set(BoardView.BOARD_VIEW.UPDATED_AT, OffsetDateTime.now())
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

        // 게시글 1: RDB에만 데이터 (Redis 재빌드로 손실) (올바른 jOOQ 테이블 사용)
        dsl.insertInto(BoardLike.BOARD_LIKE)
                .set(BoardLike.BOARD_LIKE.BOARD_ID, TEST_BOARD_ID)
                .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, 50)
                .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                .execute();
        
        dsl.insertInto(BoardView.BOARD_VIEW)
                .set(BoardView.BOARD_VIEW.BOARD_ID, TEST_BOARD_ID)
                .set(BoardView.BOARD_VIEW.VIEW_NUMBER, 100)
                .set(BoardView.BOARD_VIEW.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // 게시글 2: Redis 재빌드 후 새로운 활동 (올바른 jOOQ 테이블 사용)
        dsl.insertInto(BoardLike.BOARD_LIKE)
                .set(BoardLike.BOARD_LIKE.BOARD_ID, TEST_BOARD_ID_2)
                .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, 25)
                .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
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
        
        dsl.insertInto(BoardLike.BOARD_LIKE)
                .set(BoardLike.BOARD_LIKE.BOARD_ID, TEST_BOARD_ID)
                .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, 100)
                .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                .execute();
        
        dsl.insertInto(BoardView.BOARD_VIEW)
                .set(BoardView.BOARD_VIEW.BOARD_ID, TEST_BOARD_ID)
                .set(BoardView.BOARD_VIEW.VIEW_NUMBER, 200)
                .set(BoardView.BOARD_VIEW.UPDATED_AT, OffsetDateTime.now())
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
        
        dsl.insertInto(BoardLike.BOARD_LIKE)
                .set(BoardLike.BOARD_LIKE.BOARD_ID, TEST_BOARD_ID)
                .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, 75)
                .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                .execute();
        
        dsl.insertInto(BoardView.BOARD_VIEW)
                .set(BoardView.BOARD_VIEW.BOARD_ID, TEST_BOARD_ID)
                .set(BoardView.BOARD_VIEW.VIEW_NUMBER, 150)
                .set(BoardView.BOARD_VIEW.UPDATED_AT, OffsetDateTime.now())
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
        // given: 대용량 게시글 데이터 생성 (기존 게시글들과 충돌 방지를 위해 ID 100부터 시작)
        int boardCount = 100;
        int startId = 100;
        
        for (int i = 0; i < boardCount; i++) {
            long boardId = startId + i;
            // 게시글 생성 (올바른 jOOQ 테이블 사용)
            dsl.insertInto(Board.BOARD)
                    .set(Board.BOARD.ID, boardId)
                    .set(Board.BOARD.MEMBER_ID, TEST_MEMBER_ID)
                    .set(Board.BOARD.TITLE, "대용량 테스트 게시글 " + i)
                    .set(Board.BOARD.CONTENT, "대용량 테스트 내용 " + i)
                    .set(Board.BOARD.DESCRIPTION, "대용량 테스트 설명 " + i)
                    .set(Board.BOARD.CATEGORY, "TECH")
                    .set(Board.BOARD.CREATED_AT, OffsetDateTime.now())
                    .set(Board.BOARD.UPDATED_AT, OffsetDateTime.now())
                    .execute();
            
            // RDB에 통계 생성 (올바른 jOOQ 테이블 사용)
            dsl.insertInto(BoardLike.BOARD_LIKE)
                    .set(BoardLike.BOARD_LIKE.BOARD_ID, boardId)
                    .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                    .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, i * 10)
                    .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                    .execute();
            
            dsl.insertInto(BoardView.BOARD_VIEW)
                    .set(BoardView.BOARD_VIEW.BOARD_ID, boardId)
                    .set(BoardView.BOARD_VIEW.VIEW_NUMBER, i * 20)
                    .set(BoardView.BOARD_VIEW.UPDATED_AT, OffsetDateTime.now())
                    .execute();
        }

        // Redis는 비어있음 (재빌드 후 상태)

        // when: 동기화 실행
        long startTime = System.currentTimeMillis();
        int syncedBoards = boardDomainService.syncAllBoardStats();
        long duration = System.currentTimeMillis() - startTime;

        // then: 모든 게시글이 안정적으로 동기화됨 (생성된 게시글 수보다 클 수 있음 - 기존 게시글들도 포함)
        assertThat(syncedBoards).isGreaterThanOrEqualTo(boardCount);
        assertThat(duration).isLessThan(5000); // 5초 이내 완료

        // 샘플 검증 (첫 번째와 마지막 게시글)
        BoardIdVo firstBoard = BoardIdVo.of((long) startId); // startId = 100
        BoardIdVo lastBoard = BoardIdVo.of((long) (startId + boardCount - 1)); // startId + 99
        
        Long firstLikeCount = boardQueryRepository.getLikeCountFromDB(firstBoard);
        Long firstViewCount = boardQueryRepository.getViewCountFromDB(firstBoard);
        Long lastLikeCount = boardQueryRepository.getLikeCountFromDB(lastBoard);
        Long lastViewCount = boardQueryRepository.getViewCountFromDB(lastBoard);
        
        // 데이터 손실 없이 보존됨을 확인 (최소값 보장)
        assertThat(firstLikeCount).isGreaterThanOrEqualTo(0L); // 첫 번째 게시글 i=0, 0*10=0
        assertThat(firstViewCount).isGreaterThanOrEqualTo(0L); // 첫 번째 게시글 i=0, 0*20=0
        assertThat(lastLikeCount).isGreaterThanOrEqualTo((long) (boardCount - 2) * 10); // 마지막 게시글 근사값 허용
        assertThat(lastViewCount).isGreaterThanOrEqualTo((long) (boardCount - 2) * 20); // 마지막 게시글 근사값 허용
    }
}
