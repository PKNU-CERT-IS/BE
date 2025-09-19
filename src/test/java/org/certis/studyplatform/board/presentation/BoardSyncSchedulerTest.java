package org.certis.studyplatform.board.presentation;
import org.certis.studyplatform.board.domain.model.vo.BoardIdVo;
import org.certis.studyplatform.board.domain.repository.BoardQueryRepository;
import org.certis.studyplatform.board.domain.repository.BoardRedisRepository;
import org.certis.studyplatform.board.infrastructure.scheduler.BoardSyncScheduler;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;

import static org.jooq.impl.DSL.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("test")
class BoardSyncSchedulerTest {

    @Autowired private BoardSyncScheduler scheduler;
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

        // 테스트용 게시글 생성
        dsl.insertInto(table("board"))
                .set(field("id"), TEST_BOARD_ID)
                .set(field("member_id"), TEST_MEMBER_ID)
                .set(field("title"), "테스트 게시글")
                .set(field("content"), "테스트 내용")
                .set(field("description"), "테스트 설명")
                .set(field("category"), "TECH") // 유효한 카테고리 사용
                .set(field("created_at"), OffsetDateTime.now())
                .set(field("updated_at"), OffsetDateTime.now())
                .execute();
    }

    @Test
    @DisplayName("동기화 스케줄러가 Redis 좋아요/조회수를 RDB로 마이그레이션 한다")
    void scheduler_syncs_redis_likes_and_views_to_rdb() {
        BoardIdVo boardIdVo = BoardIdVo.of(TEST_BOARD_ID);

        // given: Redis에 좋아요만 존재 (DB는 오래된 값일 수 있음)
        boardRedisRepository.initializeStats(boardIdVo);
        boardRedisRepository.addLike(boardIdVo, TEST_LIKER_ID);
        boardRedisRepository.addView(boardIdVo, TEST_VIEWER_ID);

        // when: 수동으로 스케줄러 실행 (00시 트리거 대체)
        scheduler.syncBoardStatsDaily();

        // then: RDB의 like 통계가 1 이상으로 반영되었는지 확인 (구체 수치는 구현에 따라 다름)
        Long dbLikeCount = boardQueryRepository.getLikeCountFromDB(boardIdVo);
        Long dbViewCount = boardQueryRepository.getViewCountFromDB(boardIdVo);
        org.assertj.core.api.Assertions.assertThat(dbLikeCount).isGreaterThanOrEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(dbViewCount).isGreaterThanOrEqualTo(1L);
    }
}


