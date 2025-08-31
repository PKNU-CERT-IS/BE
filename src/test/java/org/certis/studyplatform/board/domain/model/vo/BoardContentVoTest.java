package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Board Content VO 테스트")
class BoardContentVoTest {

    @Test
    @DisplayName("유효한 내용으로 생성 성공")
    void givenValidContent_whenCreateBoardContentVo_thenSuccess() {
        // Given
        String content = "게시글 내용입니다";

        // When
        BoardContentVo result = BoardContentVo.of(content);

        // Then
        assertThat(result.value()).isEqualTo(content);
    }

    @Test
    @DisplayName("null 내용으로 생성 시 예외")
    void givenNullContent_whenCreateBoardContentVo_thenThrowException() {
        // Given
        String nullContent = null;

        // When & Then
        assertThatThrownBy(() -> BoardContentVo.of(nullContent))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_INVALID_CONTENT);
    }

    @Test
    @DisplayName("빈 내용으로 생성 시 예외")
    void givenEmptyContent_whenCreateBoardContentVo_thenThrowException() {
        // Given
        String emptyContent = "";

        // When & Then
        assertThatThrownBy(() -> BoardContentVo.of(emptyContent))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_INVALID_CONTENT);
    }

    @Test
    @DisplayName("공백 내용으로 생성 시 예외")
    void givenWhitespaceContent_whenCreateBoardContentVo_thenThrowException() {
        // Given
        String whitespaceContent = "   ";

        // When & Then
        assertThatThrownBy(() -> BoardContentVo.of(whitespaceContent))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_INVALID_CONTENT);
    }
}

