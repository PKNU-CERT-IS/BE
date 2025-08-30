package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Board Title VO 테스트")
class BoardTitleVoTest {

    @Test
    @DisplayName("유효한 제목으로 생성 성공")
    void createBoardTitle_WithValidTitle_Success() {
        // Given
        String title = "유효한 제목";

        // When
        BoardTitleVo result = BoardTitleVo.of(title);

        // Then
        assertThat(result.value()).isEqualTo(title);
    }

    @Test
    @DisplayName("null 제목으로 생성 시 예외")
    void createBoardTitle_WithNullTitle_ThrowsException() {
        // Given
        String nullTitle = null;

        // When & Then
        assertThatThrownBy(() -> BoardTitleVo.of(nullTitle))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_INVALID_TITLE);
    }

    @Test
    @DisplayName("빈 제목으로 생성 시 예외")
    void createBoardTitle_WithEmptyTitle_ThrowsException() {
        // Given
        String emptyTitle = "";

        // When & Then
        assertThatThrownBy(() -> BoardTitleVo.of(emptyTitle))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_INVALID_TITLE);
    }

    @Test
    @DisplayName("공백 제목으로 생성 시 예외")
    void createBoardTitle_WithWhitespaceTitle_ThrowsException() {
        // Given
        String whitespaceTitle = "   ";

        // When & Then
        assertThatThrownBy(() -> BoardTitleVo.of(whitespaceTitle))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_INVALID_TITLE);
    }
}

