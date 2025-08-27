package org.certis.studyplatform.board.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BoardUpdateRequestDto {

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String content;

    @NotBlank(message = "설명은 필수입니다")
    private String description;

    @NotBlank(message = "카테고리는 필수입니다")
    private String category;

    private List<AttachmentRequestDto> attachments; // Optional
}
