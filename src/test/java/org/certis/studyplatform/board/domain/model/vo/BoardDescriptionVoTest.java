package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Board Description VO 테스트")
class BoardDescriptionVoTest {

    @Test
    @DisplayName("유효한 설명으로 생성 성공")
    void givenValidDescription_whenCreateBoardDescriptionVo_thenSuccess() {
        // Given
        String description = "게시글 설명입니다";

        // When
        BoardDescriptionVo result = BoardDescriptionVo.of(description);

        // Then
        assertThat(result.value()).isEqualTo(description);
    }

    @Test
    @DisplayName("null 설명으로 생성 시 예외")
    void givenNullDescription_whenCreateBoardDescriptionVo_thenThrowException() {
        // Given
        String nullDescription = null;

        // When & Then
        assertThatThrownBy(() -> BoardDescriptionVo.of(nullDescription))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_INVALID_DESCRIPTION);
    }

    @Test
    @DisplayName("빈 설명으로 생성 시 예외")
    void givenEmptyDescription_whenCreateBoardDescriptionVo_thenThrowException() {
        // Given
        String emptyDescription = "";

        // When & Then
        assertThatThrownBy(() -> BoardDescriptionVo.of(emptyDescription))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("공백 설명으로 생성 시 예외")
    void givenWhitespaceDescription_whenCreateBoardDescriptionVo_thenThrowException() {
        // Given
        String whitespaceDescription = "   ";

        // When & Then
        assertThatThrownBy(() -> BoardDescriptionVo.of(whitespaceDescription))
                .isInstanceOf(DomainException.class);
    }
}

