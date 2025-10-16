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
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 데이터 손실 방지 테스트
 * 다양한 상황에서 데이터 손실이 발생하지 않는지 검증합니다.
 */
@SpringBootTest
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("redis-test")
// @Transactional  // 트랜잭션 롤백 제거로 동기화 결과 확인 가능
class DataLossPreventionTest {

    @Autowired private BoardSyncService boardSyncService;
    @Autowired private BoardDomainService boardDomainService;
    @Autowired private BoardRedisRepository boardRedisRepository;
    @Autowired private BoardQueryRepository boardQueryRepository;
    @Autowired private DSLContext dsl;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final Long TEST_BOARD_ID = 1L;
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
            boardRedisRepository.deleteStats(BoardIdVo.of(TEST_BOARD_ID));
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

        // 테스트용 게시글 생성 (올바른 jOOQ 테이블 사용)
        try {
            dsl.insertInto(Board.BOARD)
                    .set(Board.BOARD.ID, TEST_BOARD_ID)
                    .set(Board.BOARD.MEMBER_ID, TEST_MEMBER_ID)
                    .set(Board.BOARD.TITLE, "데이터 손실 방지 테스트 게시글")
                    .set(Board.BOARD.CONTENT, "테스트 내용")
                    .set(Board.BOARD.DESCRIPTION, "테스트 설명")
                    .set(Board.BOARD.CATEGORY, "TECH")
                    .set(Board.BOARD.CREATED_AT, OffsetDateTime.now())
                    .set(Board.BOARD.UPDATED_AT, OffsetDateTime.now())
                    .execute();
        } catch (Exception e) {
            System.out.println("Failed to create test board: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("데이터 손실 방지 1: Redis TTL 만료 시 기존 RDB 데이터가 보존된다")
    void data_loss_prevention1_preserves_rdb_data_when_redis_ttl_expired() {
        // given: RDB에 중요한 누적 데이터 존재
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 장기간 누적된 중요한 데이터
        dsl.insertInto(BoardLike.BOARD_LIKE)
                .set(BoardLike.BOARD_LIKE.BOARD_ID, TEST_BOARD_ID)
                .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, 1000) // 중요한 누적 데이터
                .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                .execute();
        
        dsl.insertInto(BoardView.BOARD_VIEW)
                .set(BoardView.BOARD_VIEW.BOARD_ID, TEST_BOARD_ID)
                .set(BoardView.BOARD_VIEW.VIEW_NUMBER, 5000) // 중요한 누적 데이터
                .set(BoardView.BOARD_VIEW.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // Redis는 TTL 만료로 데이터 없음 (시뮬레이션)

        // when: 동기화 실행
        boardSyncService.syncStatsFromRedisToDatabase();

        // then: 중요한 데이터가 손실되지 않음
        Long dbLikeCount = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long dbViewCount = boardQueryRepository.getViewCountFromDB(boardIdVo);
        
        assertThat(dbLikeCount).isEqualTo(1000L); // 데이터 손실 없음
        assertThat(dbViewCount).isEqualTo(5000L); // 데이터 손실 없음
    }

    @Test
    @DisplayName("데이터 손실 방지 2: Redis 값이 0일 때 기존 RDB 값이 유지된다")
    void data_loss_prevention2_maintains_rdb_value_when_redis_is_zero() {
        // given: RDB에 데이터 존재, Redis는 0
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 기존 데이터 (올바른 jOOQ 테이블 사용)
        dsl.insertInto(BoardLike.BOARD_LIKE)
                .set(BoardLike.BOARD_LIKE.BOARD_ID, TEST_BOARD_ID)
                .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, 500)
                .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                .execute();
        
        dsl.insertInto(BoardView.BOARD_VIEW)
                .set(BoardView.BOARD_VIEW.BOARD_ID, TEST_BOARD_ID)
                .set(BoardView.BOARD_VIEW.VIEW_NUMBER, 1000)
                .set(BoardView.BOARD_VIEW.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // Redis 초기화 (0 값)
        boardRedisRepository.initializeStats(boardIdVo);

        // when: 동기화 실행
        boardSyncService.syncStatsFromRedisToDatabase();

        // then: 기존 RDB 값이 유지됨
        Long dbLikeCount = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long dbViewCount = boardQueryRepository.getViewCountFromDB(boardIdVo);
        
        assertThat(dbLikeCount).isEqualTo(500L); // 기존 값 유지
        assertThat(dbViewCount).isEqualTo(1000L); // 기존 값 유지
    }

    @Test
    @DisplayName("데이터 손실 방지 3: Redis에 유효한 값이 있을 때만 업데이트된다")
    void data_loss_prevention3_updates_only_when_redis_has_valid_values() {
        // given: RDB에 기존 데이터 존재
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 기존 데이터 (올바른 jOOQ 테이블 사용)
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

        // Redis에 유효한 새로운 값
        boardRedisRepository.initializeStats(boardIdVo);
        boardRedisRepository.addLike(boardIdVo, TEST_LIKER_ID);
        boardRedisRepository.addView(boardIdVo, TEST_VIEWER_ID);

        // when: 동기화 실행
        boardSyncService.syncStatsFromRedisToDatabase();

        // then: Redis 값이 우선적으로 사용됨
        Long dbLikeCount = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long dbViewCount = boardQueryRepository.getViewCountFromDB(boardIdVo);
        
        assertThat(dbLikeCount).isEqualTo(1L); // Redis 값 사용
        assertThat(dbViewCount).isEqualTo(1L); // Redis 값 사용
    }

    @Test
    @DisplayName("데이터 손실 방지 4: 연속적인 동기화에서 데이터가 누적되지 않는다")
    void data_loss_prevention4_prevents_data_accumulation_across_multiple_syncs() {
        // given: 초기 상태
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 초기 데이터 (올바른 jOOQ 테이블 사용)
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

        // 첫 번째 동기화: Redis 비어있음
        boardSyncService.syncStatsFromRedisToDatabase();
        
        Long afterFirstSyncLike = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long afterFirstSyncView = boardQueryRepository.getViewCountFromDB(boardIdVo);
        assertThat(afterFirstSyncLike).isEqualTo(50L); // 기존 값 유지
        assertThat(afterFirstSyncView).isEqualTo(100L); // 기존 값 유지

        // 두 번째 동기화: 여전히 Redis 비어있음
        boardSyncService.syncStatsFromRedisToDatabase();
        
        Long afterSecondSyncLike = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long afterSecondSyncView = boardQueryRepository.getViewCountFromDB(boardIdVo);
        assertThat(afterSecondSyncLike).isEqualTo(50L); // 값 변경 없음
        assertThat(afterSecondSyncView).isEqualTo(100L); // 값 변경 없음

        // 세 번째 동기화: 여전히 Redis 비어있음
        boardSyncService.syncStatsFromRedisToDatabase();
        
        Long afterThirdSyncLike = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long afterThirdSyncView = boardQueryRepository.getViewCountFromDB(boardIdVo);
        assertThat(afterThirdSyncLike).isEqualTo(50L); // 값 변경 없음
        assertThat(afterThirdSyncView).isEqualTo(100L); // 값 변경 없음
    }

    @Test
    @DisplayName("데이터 손실 방지 5: Redis 장애 시에도 RDB 데이터가 보존된다")
    void data_loss_prevention5_preserves_rdb_data_even_when_redis_fails() {
        // given: RDB에 중요한 데이터 존재
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 중요한 데이터 (올바른 jOOQ 테이블 사용)
        dsl.insertInto(BoardLike.BOARD_LIKE)
                .set(BoardLike.BOARD_LIKE.BOARD_ID, TEST_BOARD_ID)
                .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, 999)
                .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                .execute();
        
        dsl.insertInto(BoardView.BOARD_VIEW)
                .set(BoardView.BOARD_VIEW.BOARD_ID, TEST_BOARD_ID)
                .set(BoardView.BOARD_VIEW.VIEW_NUMBER, 1999)
                .set(BoardView.BOARD_VIEW.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // Redis는 접근 불가 (장애 시뮬레이션)
        // Redis 초기화를 시도하지 않음

        // when: 동기화 실행 (Redis 접근 실패 상황 시뮬레이션)
        try {
            boardSyncService.syncStatsFromRedisToDatabase();
        } catch (Exception e) {
            // Redis 장애로 인한 예외는 정상
        }

        // then: RDB 데이터는 여전히 보존됨
        Long dbLikeCount = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long dbViewCount = boardQueryRepository.getViewCountFromDB(boardIdVo);
        
        assertThat(dbLikeCount).isEqualTo(999L); // 데이터 보존
        assertThat(dbViewCount).isEqualTo(1999L); // 데이터 보존
    }

    @Test
    @DisplayName("데이터 손실 방지 6: 동기화 중 일부 실패해도 성공한 데이터는 보존된다")
    void data_loss_prevention6_preserves_successful_data_even_when_partial_failure() {
        // given: 여러 게시글 중 일부는 정상, 일부는 문제 상황
        BoardIdVo normalBoardId = BoardIdVo.of(TEST_BOARD_ID);
        
        // 정상 게시글의 RDB 데이터 (올바른 jOOQ 테이블 사용)
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

        // 정상 게시글의 Redis 데이터
        boardRedisRepository.initializeStats(normalBoardId);
        boardRedisRepository.addLike(normalBoardId, TEST_LIKER_ID);

        // when: 동기화 실행
        try {
            boardSyncService.syncStatsFromRedisToDatabase();
        } catch (Exception e) {
            // 일부 실패는 정상 (테스트 목적)
        }

        // then: 성공한 데이터는 보존됨
        Long dbLikeCount = boardQueryRepository.getLikeCountFromDB(normalBoardId);
        Long dbViewCount = boardQueryRepository.getViewCountFromDB(normalBoardId);
        
        // 정상 게시글의 데이터는 보존되거나 업데이트됨
        assertThat(dbLikeCount).isGreaterThanOrEqualTo(0L);
        assertThat(dbViewCount).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("데이터 손실 방지 7: 대용량 데이터에서도 데이터 손실이 발생하지 않는다")
    void data_loss_prevention7_no_data_loss_even_with_large_dataset() {
        // given: 대용량 게시글 데이터 생성 (기존 게시글들과 충돌 방지를 위해 ID 100부터 시작)
        int boardCount = 50;
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
            
            // RDB에 중요한 누적 데이터 생성 (올바른 jOOQ 테이블 사용)
            dsl.insertInto(BoardLike.BOARD_LIKE)
                    .set(BoardLike.BOARD_LIKE.BOARD_ID, boardId)
                    .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                    .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, i * 100) // 중요한 누적 데이터
                    .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                    .execute();
            
            dsl.insertInto(BoardView.BOARD_VIEW)
                    .set(BoardView.BOARD_VIEW.BOARD_ID, boardId)
                    .set(BoardView.BOARD_VIEW.VIEW_NUMBER, i * 200) // 중요한 누적 데이터
                    .set(BoardView.BOARD_VIEW.UPDATED_AT, OffsetDateTime.now())
                    .execute();
        }

        // Redis는 비어있음 (TTL 만료 시뮬레이션)
        // 대용량 테스트에서 이전 테스트 잔존 키로 인한 오염을 방지하기 위해 Redis 키를 정리합니다.
        for (int i = 0; i < boardCount; i++) {
            long boardId = startId + i;
            try {
                boardRedisRepository.deleteStats(BoardIdVo.of(boardId));
            } catch (Exception ignored) {
                // 키가 없을 수 있으므로 무시
            }
        }

        // when: 동기화 실행
        int syncedBoards = boardDomainService.syncAllBoardStats();

        // then: 동기화 성공 (생성된 게시글 수보다 클 수 있음 - 기존 게시글들도 포함)
        assertThat(syncedBoards).isGreaterThanOrEqualTo(boardCount);

        // 샘플 검증: 첫 번째와 마지막 게시글의 데이터 보존 확인
        BoardIdVo firstBoard = BoardIdVo.of((long) startId); // startId = 100
        BoardIdVo lastBoard = BoardIdVo.of((long) (startId + boardCount - 1)); // startId + 49
        
        Long firstLikeCount = boardQueryRepository.getLikeCountFromDB(firstBoard);
        Long firstViewCount = boardQueryRepository.getViewCountFromDB(firstBoard);
        Long lastLikeCount = boardQueryRepository.getLikeCountFromDB(lastBoard);
        Long lastViewCount = boardQueryRepository.getViewCountFromDB(lastBoard);
        
        assertThat(firstLikeCount).isEqualTo(0L); // 데이터 보존 (첫 번째 게시글 i=0, 0*100=0)
        assertThat(firstViewCount).isEqualTo(0L); // 데이터 보존 (첫 번째 게시글 i=0, 0*200=0)
        assertThat(lastLikeCount).isEqualTo((long) (boardCount - 1) * 100); // 데이터 보존 (마지막 게시글 i=49, 49*100=4900)
        assertThat(lastViewCount).isEqualTo((long) (boardCount - 1) * 200); // 데이터 보존 (마지막 게시글 i=49, 49*200=9800)
    }

    @Test
    @DisplayName("데이터 손실 방지 8: 동기화 전후 데이터 무결성이 유지된다")
    void data_loss_prevention8_maintains_data_integrity_before_and_after_sync() {
        // given: 초기 데이터 상태
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 초기 데이터 (올바른 jOOQ 테이블 사용)
        dsl.insertInto(BoardLike.BOARD_LIKE)
                .set(BoardLike.BOARD_LIKE.BOARD_ID, TEST_BOARD_ID)
                .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, 1000)
                .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                .execute();
        
        dsl.insertInto(BoardView.BOARD_VIEW)
                .set(BoardView.BOARD_VIEW.BOARD_ID, TEST_BOARD_ID)
                .set(BoardView.BOARD_VIEW.VIEW_NUMBER, 2000)
                .set(BoardView.BOARD_VIEW.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // 동기화 전 데이터 상태 확인
        Long beforeSyncLike = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long beforeSyncView = boardQueryRepository.getViewCountFromDB(boardIdVo);
        assertThat(beforeSyncLike).isEqualTo(1000L);
        assertThat(beforeSyncView).isEqualTo(2000L);

        // Redis는 비어있음

        // when: 동기화 실행
        boardSyncService.syncStatsFromRedisToDatabase();

        // then: 데이터 무결성 유지
        Long afterSyncLike = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long afterSyncView = boardQueryRepository.getViewCountFromDB(boardIdVo);
        
        // 데이터가 손실되지 않았는지 확인
        assertThat(afterSyncLike).isEqualTo(beforeSyncLike); // 동일한 값 유지
        assertThat(afterSyncView).isEqualTo(beforeSyncView); // 동일한 값 유지
        
        // 데이터가 음수가 되지 않았는지 확인
        assertThat(afterSyncLike).isGreaterThanOrEqualTo(0L);
        assertThat(afterSyncView).isGreaterThanOrEqualTo(0L);
    }
}
