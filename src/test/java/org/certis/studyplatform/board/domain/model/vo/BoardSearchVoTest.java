package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Board Search VO 테스트")
class BoardSearchVoTest {

    @Test
    @DisplayName("유효한 검색 조건으로 생성 성공")
    void givenValidParams_whenCreateBoardSearchVo_thenSuccess() {
        // Given
        String search = "검색어";
        String category = "STUDY";
        int page = 0;
        int size = 10;

        // When
        BoardSearchVo result = BoardSearchVo.of(search, category, page, size);

        // Then
        assertThat(result.search()).isEqualTo(search);
        assertThat(result.category()).isEqualTo(category);
        assertThat(result.page()).isEqualTo(page);
        assertThat(result.size()).isEqualTo(size);
    }

    @Test
    @DisplayName("100자 초과 검색어로 생성 시 예외")
    void givenTooLongSearch_whenCreateBoardSearchVo_thenThrowException() {
        // Given
        String tooLongSearch = "검".repeat(101);

        // When & Then
        assertThatThrownBy(() -> BoardSearchVo.of(tooLongSearch, "STUDY", 0, 10))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_INVALID_PAGE);
    }

    @Test
    @DisplayName("음수 페이지로 생성 시 예외")
    void givenNegativePage_whenCreateBoardSearchVo_thenThrowException() {
        // Given
        int negativePage = -1;

        // When & Then
        assertThatThrownBy(() -> BoardSearchVo.of("검색어", "STUDY", negativePage, 10))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_INVALID_PAGE);
    }

    @Test
    @DisplayName("잘못된 사이즈로 생성 시 예외")
    void givenInvalidSize_whenCreateBoardSearchVo_thenThrowException() {
        // Given
        int sizeZero = 0;
        int sizeTooBig = 101;

        // When & Then - size가 0이면 예외
        assertThatThrownBy(() -> BoardSearchVo.of("검색어", "STUDY", 0, sizeZero))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_INVALID_PAGE_SIZE);

        // When & Then - size가 100 초과면 예외
        assertThatThrownBy(() -> BoardSearchVo.of("검색어", "STUDY", 0, sizeTooBig))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_INVALID_PAGE_SIZE);
    }

    @Test
    @DisplayName("null 검색어와 카테고리는 허용")
    void givenNullValues_whenCreateBoardSearchVo_thenSuccess() {
        // Given
        String nullSearch = null;
        String nullCategory = null;

        // When
        BoardSearchVo result = BoardSearchVo.of(nullSearch, nullCategory, 0, 10);

        // Then
        assertThat(result.search()).isNull();
        assertThat(result.category()).isNull();
    }
}
