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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.impl.DSL.*;

/**
 * 간단한 Board 동기화 테스트
 * 핵심 기능만 테스트하여 컴파일 에러를 방지합니다.
 */
@SpringBootTest
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SimpleBoardSyncTest {

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
        // 테스트 데이터 정리
        dsl.execute("TRUNCATE TABLE board RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE board_like RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE board_view RESTART IDENTITY CASCADE");
        
        // 시퀀스 리셋
        dsl.execute("ALTER SEQUENCE board_id_seq RESTART WITH 1");
        dsl.execute("ALTER SEQUENCE member_id_seq RESTART WITH 1");
        
        // Redis 초기화 (테스트 간 격리를 위해)
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        boardRedisRepository.deleteStats(boardIdVo);

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

        // 테스트용 게시글 생성
        dsl.insertInto(table("board"))
                .set(field("id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("title"), "간단한 동기화 테스트 게시글")
                .set(field("content"), "테스트 내용")
                .set(field("description"), "테스트 설명")
                .set(field("category"), "TECH")
                .set(field("created_at"), OffsetDateTime.now())
                .set(field("updated_at"), OffsetDateTime.now())
                .execute();
    }

    @Test
    @DisplayName("기본 동기화: Redis 통계가 RDB로 동기화된다")
    void basic_sync_transfers_redis_stats_to_rdb() {
        // given: Redis에 통계 데이터 존재
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        boardRedisRepository.initializeStats(boardIdVo);
        
        // 고유한 멤버 ID로 좋아요와 조회수 추가 (테스트 간 격리)
        Long uniqueLikerId = System.currentTimeMillis() % 10000 + 1000; // 고유한 ID 생성
        Long uniqueViewerId = System.currentTimeMillis() % 10000 + 2000; // 고유한 ID 생성
        
        boardRedisRepository.addLike(boardIdVo, uniqueLikerId);
        boardRedisRepository.addView(boardIdVo, uniqueViewerId);

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
    }

    @Test
    @DisplayName("Redis 재빌드 시나리오: Redis가 비어있을 때 기존 RDB 값이 보존된다")
    void redis_rebuild_preserves_rdb_values_when_redis_empty() {
        // given: RDB에만 통계 데이터 존재 (Redis 재빌드로 인한 데이터 손실 시뮬레이션)
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);
        
        // RDB에 기존 통계 생성 (과거 동기화로 누적된 데이터)
        dsl.insertInto(table("board_like"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("like_number"), 100)
                .set(field("updated_at"), OffsetDateTime.now())
                .execute();
        
        dsl.insertInto(table("board_view"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("view_number"), 200)
                .set(field("updated_at"), OffsetDateTime.now())
                .execute();

        // Redis는 완전히 비어있음 (재빌드 후 상태)

        // when: 동기화 실행
        boardSyncService.syncStatsFromRedisToDatabase();

        // then: 기존 RDB 데이터가 보존됨 (데이터 손실 없음)
        Long dbLikeCount = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long dbViewCount = boardQueryRepository.getViewCountFromDB(boardIdVo);
        
        assertThat(dbLikeCount).isEqualTo(100L); // 기존 값 보존
        assertThat(dbViewCount).isEqualTo(200L); // 기존 값 보존
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
                .set(field("updated_at"), OffsetDateTime.now())
                .execute();
        
        dsl.insertInto(table("board_view"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("view_number"), 10)
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
                .set(field("updated_at"), OffsetDateTime.now())
                .execute();
        
        dsl.insertInto(table("board_view"))
                .set(field("board_id"), TEST_BOARD_ID)
                .set(field("view_number"), 10)
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
        
        // 고유한 멤버 ID로 좋아요와 조회수 추가 (테스트 간 격리)
        Long uniqueLikerId = System.currentTimeMillis() % 10000 + 3000; // 고유한 ID 생성
        Long uniqueViewerId = System.currentTimeMillis() % 10000 + 4000; // 고유한 ID 생성
        
        boardRedisRepository.addLike(boardIdVo, uniqueLikerId);
        boardRedisRepository.addView(boardIdVo, uniqueViewerId);

        // when: 동기화 실행
        boardSyncService.syncStatsFromRedisToDatabase();

        // then: Redis가 초기화되어 다음 날 통계를 위해 준비됨
        Long redisLikeCount = boardRedisRepository.getLikeCount(boardIdVo);
        Long redisViewCount = boardRedisRepository.getViewCount(boardIdVo);
        
        // 초기화 후에는 0이 되어야 함
        assertThat(redisLikeCount).isEqualTo(0L);
        assertThat(redisViewCount).isEqualTo(0L);
    }
}
