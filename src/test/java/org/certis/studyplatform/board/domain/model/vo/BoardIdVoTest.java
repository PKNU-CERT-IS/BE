package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Board ID VO 테스트")
class BoardIdVoTest {

    @Test
    @DisplayName("유효한 ID로 생성 성공")
    void createBoardIdVo_WithValidId_Success() {
        // Given
        Long validId = 1L;

        // When
        BoardIdVo result = BoardIdVo.of(validId);

        // Then
        assertThat(result.value()).isEqualTo(1L);
    }

    @Test
    @DisplayName("null ID로 생성 시 예외")
    void createBoardIdVo_WithNullId_ThrowsException() {
        // Given
        Long nullId = null;

        // When & Then
        assertThatThrownBy(() -> BoardIdVo.of(nullId))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_INVALID_ID);
    }

    @Test
    @DisplayName("음수 ID로 생성 시 예외")
    void createBoardIdVo_WithNegativeId_ThrowsException() {
        // Given
        Long negativeId = -1L;

        // When & Then
        assertThatThrownBy(() -> BoardIdVo.of(negativeId))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_INVALID_ID);
    }
}
