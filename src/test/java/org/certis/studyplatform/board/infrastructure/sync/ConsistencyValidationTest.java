package org.certis.studyplatform.board.infrastructure.sync;

import org.certis.studyplatform.board.domain.model.vo.BoardIdVo;
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

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.certis.generated.jooq.tables.Board;
import org.certis.generated.jooq.tables.BoardLike;
import org.certis.generated.jooq.tables.BoardView;
import org.certis.generated.jooq.tables.Member;

/**
 * 일관성 검증 테스트
 * Redis와 RDB 간 데이터 일관성 검증이 올바르게 작동하는지 검증합니다.
 */
@SpringBootTest
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("redis-test")
// @Transactional  // 트랜잭션 롤백 제거로 동기화 결과 확인 가능
class ConsistencyValidationTest {

    @Autowired private BoardDomainService boardDomainService;
    @Autowired private BoardRedisRepository boardRedisRepository;
    @Autowired private DSLContext dsl;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final Long TEST_BOARD_ID = 1L;
    private static final Long TEST_BOARD_ID_2 = 2L;
    private static final Long TEST_BOARD_ID_3 = 3L;

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
            for (Long boardId : List.of(TEST_BOARD_ID, TEST_BOARD_ID_2, TEST_BOARD_ID_3)) {
                try {
                    boardRedisRepository.deleteStats(BoardIdVo.of(boardId));
                } catch (Exception e) {
                    // Redis 키가 없을 수 있음 (정상)
                }
            }
        } catch (Exception e) {
            // Redis 정리 실패 시 로그만 출력
            System.out.println("Redis cleanup failed: " + e.getMessage());
        }

        // 테스트용 멤버 생성
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

        // 테스트용 게시글 3개 생성
        for (Long boardId : List.of(TEST_BOARD_ID, TEST_BOARD_ID_2, TEST_BOARD_ID_3)) {
            try {
                dsl.insertInto(Board.BOARD)
                        .set(Board.BOARD.ID, boardId)
                        .set(Board.BOARD.MEMBER_ID, TEST_MEMBER_ID)
                        .set(Board.BOARD.TITLE, "일관성 검증 테스트 게시글 " + boardId)
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
    @DisplayName("일관성 검증 1: Redis와 RDB가 일치할 때 검증이 통과한다")
    void consistency_validation1_passes_when_redis_and_rdb_match() {
        // given: Redis와 RDB에 동일한 데이터 존재
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 통계 생성 (올바른 jOOQ 테이블 사용)
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

        // Redis에 동일한 통계 설정
        boardRedisRepository.initializeStats(boardIdVo);
        boardRedisRepository.setLikeCount(boardIdVo, 100L);
        boardRedisRepository.setViewCount(boardIdVo, 200L);

        // when: 일관성 검증 실행
        boolean isConsistent = boardDomainService.validateStatsConsistency();

        // then: 일관성 검증 통과
        assertThat(isConsistent).isTrue();
    }

    @Test
    @DisplayName("일관성 검증 2: Redis TTL 만료 시 검증이 통과한다")
    void consistency_validation2_passes_when_redis_ttl_expired() {
        // given: RDB에만 데이터 존재 (Redis TTL 만료)
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 통계 생성
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

        // Redis를 0으로 설정하여 TTL 만료 시뮬레이션
        boardRedisRepository.initializeStats(boardIdVo);
        boardRedisRepository.setLikeCount(boardIdVo, 0L);
        boardRedisRepository.setViewCount(boardIdVo, 0L);

        // when: 일관성 검증 실행
        boolean isConsistent = boardDomainService.validateStatsConsistency();

        // then: TTL 만료는 정상 상황으로 처리됨
        assertThat(isConsistent).isTrue();
    }

    @Test
    @DisplayName("일관성 검증 3: Redis와 RDB가 불일치할 때 검증이 실패한다")
    void consistency_validation3_fails_when_redis_and_rdb_mismatch() {
        // given: Redis와 RDB에 다른 데이터 존재
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 통계 생성
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

        // Redis에 다른 통계 설정
        boardRedisRepository.initializeStats(boardIdVo);
        boardRedisRepository.setLikeCount(boardIdVo, 150L); // 다른 값
        boardRedisRepository.setViewCount(boardIdVo, 250L); // 다른 값

        // when: 일관성 검증 실행
        boolean isConsistent = boardDomainService.validateStatsConsistency();

        // then: 일관성 검증 실패
        assertThat(isConsistent).isFalse();
    }

    @Test
    @DisplayName("일관성 검증 4: 여러 게시글의 복합 상황을 올바르게 처리한다")
    void consistency_validation4_handles_multiple_boards_complex_situation() {
        // given: 여러 게시글의 다양한 상황
        BoardIdVo boardIdVo1 = BoardIdVo.of(TEST_BOARD_ID);
        BoardIdVo boardIdVo3 = BoardIdVo.of(TEST_BOARD_ID_3);

        // 게시글 1: Redis와 RDB 일치
        dsl.insertInto(BoardLike.BOARD_LIKE)
                .set(BoardLike.BOARD_LIKE.BOARD_ID, TEST_BOARD_ID)
                .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, 100)
                .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                .execute();
        
        boardRedisRepository.initializeStats(boardIdVo1);
        boardRedisRepository.setLikeCount(boardIdVo1, 100L);

        // 게시글 2: Redis TTL 만료 (정상 상황)
        dsl.insertInto(BoardLike.BOARD_LIKE)
                .set(BoardLike.BOARD_LIKE.BOARD_ID, TEST_BOARD_ID_2)
                .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, 200)
                .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                .execute();
        // Redis 초기화하지 않음

        // 게시글 3: Redis와 RDB 불일치
        dsl.insertInto(BoardLike.BOARD_LIKE)
                .set(BoardLike.BOARD_LIKE.BOARD_ID, TEST_BOARD_ID_3)
                .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, 300)
                .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                .execute();
        
        boardRedisRepository.initializeStats(boardIdVo3);
        boardRedisRepository.setLikeCount(boardIdVo3, 350L); // 다른 값

        // when: 일관성 검증 실행
        boolean isConsistent = boardDomainService.validateStatsConsistency();

        // then: 불일치가 있으므로 검증 실패
        assertThat(isConsistent).isFalse();
    }

    @Test
    @DisplayName("일관성 검증 5: Redis가 완전히 비어있을 때 검증이 통과한다")
    void consistency_validation5_passes_when_redis_completely_empty() {
        
        // RDB에 통계 생성
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

        // Redis는 완전히 비어있음 (초기화하지 않음) - 실제 TTL 만료 상황을 시뮬레이션

        // when: 일관성 검증 실행
        boolean isConsistent = boardDomainService.validateStatsConsistency();

        // then: Redis가 비어있는 것은 정상 상황
        assertThat(isConsistent).isTrue();
    }

    @Test
    @DisplayName("일관성 검증 6: Redis와 RDB 데이터가 부분적으로 불일치하면 검증이 실패한다")
    void consistency_validation6_detects_partial_data_inconsistency() {
        // given: Redis에 일부 데이터만 존재
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 통계 생성
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

        // Redis에 좋아요만 설정 (조회수는 설정하지 않음)
        boardRedisRepository.initializeStats(boardIdVo);
        boardRedisRepository.setLikeCount(boardIdVo, 100L);
        // viewCount는 초기값 0으로 유지

        // when: 일관성 검증 실행
        boolean isConsistent = boardDomainService.validateStatsConsistency();

        // then: 조회수가 불일치하므로 검증 실패해야 함
        // Redis viewCount: 0, RDB viewCount: 200 으로 불일치
        assertThat(isConsistent).isFalse();
    }

    @Test
    @DisplayName("일관성 검증 7: 대용량 데이터에서도 정확하게 검증한다")
    void consistency_validation7_validates_correctly_even_with_large_data() {
        // given: 대용량 게시글 데이터 생성 (기존 게시글들과 충돌 방지를 위해 ID 90부터 시작)
        int boardCount = 20;
        int startId = 90;
        
        for (int i = 0; i < boardCount; i++) {
            long boardId = startId + i;
            // 게시글 생성
            dsl.insertInto(Board.BOARD)
                    .set(Board.BOARD.ID, boardId)
                    .set(Board.BOARD.MEMBER_ID, TEST_MEMBER_ID)
                    .set(Board.BOARD.TITLE, "대용량 일관성 테스트 게시글 " + i)
                    .set(Board.BOARD.CONTENT, "대용량 테스트 내용 " + i)
                    .set(Board.BOARD.DESCRIPTION, "대용량 테스트 설명 " + i)
                    .set(Board.BOARD.CATEGORY, "TECH")
                    .set(Board.BOARD.CREATED_AT, OffsetDateTime.now())
                    .set(Board.BOARD.UPDATED_AT, OffsetDateTime.now())
                    .execute();
            
            // RDB에 통계 생성
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
            
            // Redis에 동일한 통계 설정
            BoardIdVo boardIdVo = BoardIdVo.of(boardId);
            boardRedisRepository.initializeStats(boardIdVo);
            boardRedisRepository.setLikeCount(boardIdVo, (long) i * 10);
            boardRedisRepository.setViewCount(boardIdVo, (long) i * 20);
        }

        // when: 일관성 검증 실행
        boolean isConsistent = boardDomainService.validateStatsConsistency();

        // then: 모든 데이터가 일치하므로 검증 통과
        assertThat(isConsistent).isTrue();
    }

    @Test
    @DisplayName("일관성 검증 8: 에러 발생 시에도 안전하게 처리한다")
    void consistency_validation8_handles_errors_safely() {
        
        // RDB에는 존재하지 않는 게시글
        // Redis 접근 시 예외 발생 가능

        // when: 일관성 검증 실행
        boolean isConsistent = boardDomainService.validateStatsConsistency();

        // then: 에러가 발생해도 안전하게 처리됨
        // 존재하지 않는 게시글은 검증 대상에서 제외되므로 통과
        assertThat(isConsistent).isTrue();
    }

    @Test
    @DisplayName("일관성 검증 9: 혼합 상황에서 정확한 판단을 한다")
    void consistency_validation9_makes_accurate_judgment_in_mixed_situations() {
        // given: 혼합 상황 - 일부는 일치, 일부는 불일치, 일부는 TTL 만료
        BoardIdVo boardIdVo1 = BoardIdVo.of(TEST_BOARD_ID);
        BoardIdVo boardIdVo3 = BoardIdVo.of(TEST_BOARD_ID_3);

        // 게시글 1: Redis와 RDB 일치 (정상)
        dsl.insertInto(BoardLike.BOARD_LIKE)
                .set(BoardLike.BOARD_LIKE.BOARD_ID, TEST_BOARD_ID)
                .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, 100)
                .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                .execute();
        
        boardRedisRepository.initializeStats(boardIdVo1);
        boardRedisRepository.setLikeCount(boardIdVo1, 100L);

        // 게시글 2: Redis TTL 만료 (정상)
        dsl.insertInto(BoardLike.BOARD_LIKE)
                .set(BoardLike.BOARD_LIKE.BOARD_ID, TEST_BOARD_ID_2)
                .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, 200)
                .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                .execute();
        // Redis 초기화하지 않음

        // 게시글 3: Redis와 RDB 불일치 (문제)
        dsl.insertInto(BoardLike.BOARD_LIKE)
                .set(BoardLike.BOARD_LIKE.BOARD_ID, TEST_BOARD_ID_3)
                .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, 300)
                .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                .execute();
        
        boardRedisRepository.initializeStats(boardIdVo3);
        boardRedisRepository.setLikeCount(boardIdVo3, 350L); // 다른 값

        // when: 일관성 검증 실행
        boolean isConsistent = boardDomainService.validateStatsConsistency();

        // then: 하나라도 불일치가 있으면 실패
        assertThat(isConsistent).isFalse();
    }

    @Test
    @DisplayName("일관성 검증 10: 성능이 안정적으로 유지된다")
    void consistency_validation10_maintains_stable_performance() {
        // given: 중간 규모 데이터로 성능 테스트 (기존 게시글들과 충돌 방지를 위해 ID 70부터 시작)
        int boardCount = 30;
        int startId = 70;
        
        for (int i = 0; i < boardCount; i++) {
            long boardId = startId + i;
            // 게시글 생성
            dsl.insertInto(Board.BOARD)
                    .set(Board.BOARD.ID, boardId)
                    .set(Board.BOARD.MEMBER_ID, TEST_MEMBER_ID)
                    .set(Board.BOARD.TITLE, "성능 테스트 게시글 " + i)
                    .set(Board.BOARD.CONTENT, "성능 테스트 내용 " + i)
                    .set(Board.BOARD.DESCRIPTION, "성능 테스트 설명 " + i)
                    .set(Board.BOARD.CATEGORY, "TECH")
                    .set(Board.BOARD.CREATED_AT, OffsetDateTime.now())
                    .set(Board.BOARD.UPDATED_AT, OffsetDateTime.now())
                    .execute();
            
            // RDB에 통계 생성
            dsl.insertInto(BoardLike.BOARD_LIKE)
                    .set(BoardLike.BOARD_LIKE.BOARD_ID, boardId)
                    .set(BoardLike.BOARD_LIKE.MEMBER_ID, TEST_MEMBER_ID)
                    .set(BoardLike.BOARD_LIKE.LIKE_NUMBER, i)
                    .set(BoardLike.BOARD_LIKE.UPDATED_AT, OffsetDateTime.now())
                    .execute();
            
            dsl.insertInto(BoardView.BOARD_VIEW)
                    .set(BoardView.BOARD_VIEW.BOARD_ID, boardId)
                    .set(BoardView.BOARD_VIEW.VIEW_NUMBER, i * 2)
                    .set(BoardView.BOARD_VIEW.UPDATED_AT, OffsetDateTime.now())
                    .execute();
            
            // Redis에 동일한 통계 설정
            BoardIdVo boardIdVo = BoardIdVo.of(boardId);
            boardRedisRepository.initializeStats(boardIdVo);
            boardRedisRepository.setLikeCount(boardIdVo, (long) i);
            boardRedisRepository.setViewCount(boardIdVo, (long) i * 2);
        }

        // when: 일관성 검증 실행 (성능 측정)
        long startTime = System.currentTimeMillis();
        boolean isConsistent = boardDomainService.validateStatsConsistency();
        long duration = System.currentTimeMillis() - startTime;

        // then: 성능이 안정적으로 유지됨
        assertThat(isConsistent).isTrue();
        assertThat(duration).isLessThan(2000); // 2초 이내 완료
    }
}
