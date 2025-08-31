package org.certis.studyplatform.board.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Attachment VO 테스트")
class AttachmentVoTest {

    @Test
    @DisplayName("모든 유효한 값으로 생성 성공")
    void givenValidValues_whenCreateAttachment_thenSuccess() {
        // Given
        Long id = 1L;
        String name = "파일.pdf";
        String type = "application/pdf";
        String size = "1MB";
        String url = "http://example.com/file.pdf";

        // When
        AttachmentVo result = AttachmentVo.of(id, name, type, size, url);

        // Then
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo(name);
        assertThat(result.type()).isEqualTo(type);
        assertThat(result.size()).isEqualTo(size);
        assertThat(result.attachedUrl()).isEqualTo(url);
    }

    @Test
    @DisplayName("null ID로 생성 허용 (신규 첨부파일)")
    void givenNullId_whenCreateAttachment_thenSuccess() {
        // Given
        Long nullId = null;

        // When
        AttachmentVo result = AttachmentVo.of(nullId, "파일.pdf", "application/pdf", "1MB", "http://example.com/file.pdf");

        // Then
        assertThat(result.id()).isNull();
        assertThat(result.name()).isEqualTo("파일.pdf");
    }

    @Test
    @DisplayName("파일명이 null이면 예외")
    void givenNullName_whenCreateAttachment_thenThrowException() {
        // Given
        String nullName = null;

        // When & Then
        assertThatThrownBy(() -> AttachmentVo.of(1L, nullName, "application/pdf", "1MB", "http://example.com/file.pdf"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("URL이 null이면 예외")
    void givenNullUrl_whenCreateAttachment_thenThrowException() {
        // Given
        String nullUrl = null;

        // When & Then
        assertThatThrownBy(() -> AttachmentVo.of(1L, "파일.pdf", "application/pdf", "1MB", nullUrl))
                .isInstanceOf(Exception.class);
    }
}

