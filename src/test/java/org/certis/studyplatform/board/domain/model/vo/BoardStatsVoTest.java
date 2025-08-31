package org.certis.studyplatform.board.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Board Stats VO 테스트")
class BoardStatsVoTest {

    @Test
    @DisplayName("유효한 통계로 생성 성공")
    void givenValidStats_whenCreateBoardStatsVo_thenSuccess() {
        // Given
        long likeCount = 10L;
        long viewCount = 50L;

        // When
        BoardStatsVo result = BoardStatsVo.of(likeCount, viewCount, 1L, 2L);

        // Then
        assertThat(result.likeCount()).isEqualTo(likeCount);
        assertThat(result.viewCount()).isEqualTo(viewCount);
        assertThat(result.likeId()).isEqualTo(1L);
        assertThat(result.viewId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("빈 통계 생성 - empty() 팩토리 메서드")
    void givenEmptyFactoryMethod_whenCreateBoardStatsVo_thenZeroValues() {
        // When
        BoardStatsVo result = BoardStatsVo.empty();

        // Then
        assertThat(result.likeCount()).isEqualTo(0L);
        assertThat(result.viewCount()).isEqualTo(0L);
        assertThat(result.likeId()).isNull();
        assertThat(result.viewId()).isNull();
    }

}
