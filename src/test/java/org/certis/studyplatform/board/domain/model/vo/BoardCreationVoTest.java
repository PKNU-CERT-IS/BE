package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Board Creation VO 테스트")
class BoardCreationVoTest {

    @Test
    @DisplayName("유효한 데이터로 생성 성공")
    void givenValidData_whenCreateBoardCreationVo_thenSuccess() {
        // Given
        String title = "제목";
        String content = "내용";
        String description = "설명";
        String category = "TECH";
        Long authorId = 1L;

        // When
        BoardCreationVo result = BoardCreationVo.of(title, content, description, category, authorId, List.of());

        // Then
        assertThat(result.title()).isEqualTo(title);
        assertThat(result.content()).isEqualTo(content);
        assertThat(result.description()).isEqualTo(description);
        assertThat(result.category()).isEqualTo(category);
        assertThat(result.authorId()).isEqualTo(authorId);
        assertThat(result.attachments()).isEmpty();
    }

    @Test
    @DisplayName("null authorId로 생성 시 예외")
    void givenNullAuthorId_whenCreateBoardCreationVo_thenThrowException() {
        // Given
        Long nullAuthorId = null;

        // When & Then
        assertThatThrownBy(() -> BoardCreationVo.of("제목", "내용", "설명", "TECH", nullAuthorId, List.of()))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_INVALID_ID);
    }

    @Test
    @DisplayName("음수 authorId로 생성 시 예외")
    void givenNegativeAuthorId_whenCreateBoardCreationVo_thenThrowException() {
        // Given
        Long negativeAuthorId = -1L;

        // When & Then
        assertThatThrownBy(() -> BoardCreationVo.of("제목", "내용", "설명", "TECH", negativeAuthorId, List.of()))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_INVALID_ID);
    }

    @Test
    @DisplayName("0 authorId로 생성 시 예외")
    void givenZeroAuthorId_whenCreateBoardCreationVo_thenThrowException() {
        // Given
        Long zeroAuthorId = 0L;

        // When & Then
        assertThatThrownBy(() -> BoardCreationVo.of("제목", "내용", "설명", "TECH", zeroAuthorId, List.of()))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_INVALID_ID);
    }

    @Test
    @DisplayName("null attachments는 빈 리스트로 변환")
    void givenNullAttachments_whenCreateBoardCreationVo_thenEmptyList() {
        // Given
        List<AttachmentVo> nullAttachments = null;

        // When
        BoardCreationVo result = BoardCreationVo.of("제목", "내용", "설명", "TECH", 1L, nullAttachments);

        // Then
        assertThat(result.attachments()).isNotNull();
        assertThat(result.attachments()).isEmpty();
    }

    @Test
    @DisplayName("첨부파일이 있는 게시글 생성")
    void givenAttachments_whenCreateBoardCreationVo_thenSuccess() {
        // Given
        List<AttachmentVo> attachments = List.of(
                AttachmentVo.of(null, "파일1.pdf", "application/pdf", "1MB", "http://example.com/file1.pdf"),
                AttachmentVo.of(null, "파일2.jpg", "image/jpeg", "500KB", "http://example.com/file2.jpg")
        );

        // When
        BoardCreationVo result = BoardCreationVo.of("제목", "내용", "설명", "TECH", 1L, attachments);

        // Then
        assertThat(result.attachments()).hasSize(2);
        assertThat(result.attachments().get(0).name()).isEqualTo("파일1.pdf");
        assertThat(result.attachments().get(1).name()).isEqualTo("파일2.jpg");
    }
}

