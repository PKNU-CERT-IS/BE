package org.certis.studyplatform.board.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Board VO 테스트")
class BoardVoTest {

    @Nested
    @DisplayName("생성 관련 테스트")
    class CreationTests {

        @Test
        @DisplayName("유효한 데이터로 생성 성공")
        void givenValidData_whenCreateBoardVo_thenSuccess() {
            // When
            BoardVo result = newBoard(100L);

            // Then
            assertAll(
                    () -> assertThat(result.id()).isEqualTo(1L),
                    () -> assertThat(result.title()).isEqualTo("제목"),
                    () -> assertThat(result.authorId()).isEqualTo(100L)
            );
        }

        @Test
        @DisplayName("null 첨부파일 리스트 처리")
        void givenNullAttachments_whenCreateBoardVo_thenEmptyListHandledGracefully() {
            // When
            BoardVo board = BoardVo.of(1L, "제목", "내용", "설명", "TECH",
                    100L, OffsetDateTime.now(), OffsetDateTime.now(), null);

            // Then
            assertAll(
                    () -> assertThat(board.hasAttachments()).isFalse(),
                    () -> assertThat(board.getAttachmentCount()).isEqualTo(0),
                    () -> assertThat(board.attachments()).isEmpty()
            );
        }
    }

    @Nested
    @DisplayName("비즈니스 메서드 테스트")
    class BusinessMethodTests {

        @Test
        @DisplayName("작성자 확인 - 본인일 때 true")
        void givenSameAuthorId_whenIsAuthor_thenTrue() {
            // Given
            BoardVo board = newBoard(100L);

            // When & Then
            assertThat(board.isAuthor(100L)).isTrue();
        }

        @Test
        @DisplayName("작성자 확인 - 다른 사람일 때 false")
        void givenDifferentAuthorId_whenIsAuthor_thenFalse() {
            // Given
            BoardVo board = newBoard(100L);

            // When & Then
            assertThat(board.isAuthor(200L)).isFalse();
        }

        @Test
        @DisplayName("첨부파일 없을 때 hasAttachments는 false")
        void givenEmptyAttachments_whenHasAttachments_thenFalse() {
            // Given
            BoardVo board = newBoardWithAttachments(List.of());

            // When & Then
            assertAll(
                    () -> assertThat(board.hasAttachments()).isFalse(),
                    () -> assertThat(board.getAttachmentCount()).isEqualTo(0)
            );
        }

        @Test
        @DisplayName("첨부파일이 있을 때 hasAttachments는 true")
        void givenAttachments_whenHasAttachments_thenTrue() {
            // Given
            List<AttachmentVo> attachments = List.of(
                    AttachmentVo.of(1L, "파일.pdf", "application/pdf", "1MB", "http://example.com/file.pdf")
            );
            BoardVo board = newBoardWithAttachments(attachments);

            // When & Then
            assertAll(
                    () -> assertThat(board.hasAttachments()).isTrue(),
                    () -> assertThat(board.getAttachmentCount()).isEqualTo(1)
            );
        }
    }

    // ================================================================
    // 헬퍼 메서드
    // ================================================================
    private BoardVo newBoard(Long authorId) {
        OffsetDateTime now = OffsetDateTime.now();
        return BoardVo.of(1L, "제목", "내용", "설명", "TECH", authorId, now, now, List.of());
    }

    private BoardVo newBoardWithAttachments(List<AttachmentVo> attachments) {
        OffsetDateTime now = OffsetDateTime.now();
        return BoardVo.of(1L, "제목", "내용", "설명", "TECH", 100L, now, now, attachments);
    }
}
