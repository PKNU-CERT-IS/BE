package org.certis.studyplatform.board.infrastructure.sync;

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
 * 일관성 검증 테스트
 * Redis와 RDB 간 데이터 일관성 검증이 올바르게 작동하는지 검증합니다.
 */
@SpringBootTest
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("test")
@Transactional
class ConsistencyValidationTest {

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
                    .set(field("title"), "일관성 검증 테스트 게시글 " + boardId)
                    .set(field("content"), "테스트 내용 " + boardId)
                    .set(field("description"), "테스트 설명 " + boardId)
                    .set(field("category"), "TECH")
                    .set(field("created_at"), OffsetDateTime.now())
                    .set(field("updated_at"), OffsetDateTime.now())
                    .execute();
        }
    }

    @Test
    @DisplayName("일관성 검증 1: Redis와 RDB가 일치할 때 검증이 통과한다")
    void consistency_validation1_passes_when_redis_and_rdb_match() {
        // given: Redis와 RDB에 동일한 데이터 존재
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 통계 생성
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 100)
                .execute();
        
        dsl.insertInto(table("board_view"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("view_number"), 200)
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
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 50)
                .execute();
        
        dsl.insertInto(table("board_view"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("view_number"), 100)
                .execute();

        // Redis는 초기화하지 않음 (TTL 만료 시뮬레이션)

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
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 100)
                .execute();
        
        dsl.insertInto(table("board_view"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("view_number"), 200)
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
        BoardIdVo boardIdVo2 = BoardIdVo.of(TEST_BOARD_ID_2);
        BoardIdVo boardIdVo3 = BoardIdVo.of(TEST_BOARD_ID_3);

        // 게시글 1: Redis와 RDB 일치
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 100)
                .execute();
        
        boardRedisRepository.initializeStats(boardIdVo1);
        boardRedisRepository.setLikeCount(boardIdVo1, 100L);

        // 게시글 2: Redis TTL 만료 (정상 상황)
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID_2)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 200)
                .execute();
        // Redis 초기화하지 않음

        // 게시글 3: Redis와 RDB 불일치
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID_3)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 300)
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
        // given: RDB에만 데이터 존재, Redis는 완전히 비어있음
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 통계 생성
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 75)
                .execute();
        
        dsl.insertInto(table("board_view"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("view_number"), 150)
                .execute();

        // Redis는 완전히 비어있음 (초기화하지 않음)

        // when: 일관성 검증 실행
        boolean isConsistent = boardDomainService.validateStatsConsistency();

        // then: Redis가 비어있는 것은 정상 상황
        assertThat(isConsistent).isTrue();
    }

    @Test
    @DisplayName("일관성 검증 6: Redis에 일부 데이터만 있을 때 올바르게 처리한다")
    void consistency_validation6_handles_partial_redis_data_correctly() {
        // given: Redis에 일부 데이터만 존재
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 통계 생성
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 100)
                .execute();
        
        dsl.insertInto(table("board_view"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("view_number"), 200)
                .execute();

        // Redis에 좋아요만 설정 (조회수는 설정하지 않음)
        boardRedisRepository.initializeStats(boardIdVo);
        boardRedisRepository.setLikeCount(boardIdVo, 100L);
        // viewCount는 설정하지 않음 (0으로 유지)

        // when: 일관성 검증 실행
        boolean isConsistent = boardDomainService.validateStatsConsistency();

        // then: 좋아요는 일치하므로 검증 통과
        assertThat(isConsistent).isTrue();
    }

    @Test
    @DisplayName("일관성 검증 7: 대용량 데이터에서도 정확하게 검증한다")
    void consistency_validation7_validates_correctly_even_with_large_data() {
        // given: 대용량 게시글 데이터 생성
        int boardCount = 20;
        
        for (int i = 1; i <= boardCount; i++) {
            // 게시글 생성
            dsl.insertInto(table("board"))
                    .set(field("id"), (long) i)
                    .set(field("member_id"), TEST_MEMBER_ID)
                    .set(field("title"), "대용량 일관성 테스트 게시글 " + i)
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
            
            // Redis에 동일한 통계 설정
            BoardIdVo boardIdVo = BoardIdVo.of((long) i);
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
        // given: 존재하지 않는 게시글에 대한 Redis 접근 시도
        BoardIdVo invalidBoardIdVo = BoardIdVo.of(999L);
        
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
        BoardIdVo boardIdVo2 = BoardIdVo.of(TEST_BOARD_ID_2);
        BoardIdVo boardIdVo3 = BoardIdVo.of(TEST_BOARD_ID_3);

        // 게시글 1: Redis와 RDB 일치 (정상)
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 100)
                .execute();
        
        boardRedisRepository.initializeStats(boardIdVo1);
        boardRedisRepository.setLikeCount(boardIdVo1, 100L);

        // 게시글 2: Redis TTL 만료 (정상)
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID_2)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 200)
                .execute();
        // Redis 초기화하지 않음

        // 게시글 3: Redis와 RDB 불일치 (문제)
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID_3)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 300)
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
        // given: 중간 규모 데이터로 성능 테스트
        int boardCount = 30;
        
        for (int i = 1; i <= boardCount; i++) {
            // 게시글 생성
            dsl.insertInto(table("board"))
                    .set(field("id"), (long) i)
                    .set(field("member_id"), TEST_MEMBER_ID)
                    .set(field("title"), "성능 테스트 게시글 " + i)
                    .set(field("content"), "성능 테스트 내용 " + i)
                    .set(field("description"), "성능 테스트 설명 " + i)
                    .set(field("category"), "TECH")
                    .set(field("created_at"), OffsetDateTime.now())
                    .set(field("updated_at"), OffsetDateTime.now())
                    .execute();
            
            // RDB에 통계 생성
            dsl.insertInto(table("board_like"))
                    .set(field("board_id"), (long) i)
                    .set(field("member_id"), TEST_MEMBER_ID)
                    .set(field("like_number"), i)
                    .execute();
            
            dsl.insertInto(table("board_view"))
                    .set(field("board_id"), (long) i)
                    .set(field("view_number"), i * 2)
                    .execute();
            
            // Redis에 동일한 통계 설정
            BoardIdVo boardIdVo = BoardIdVo.of((long) i);
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
