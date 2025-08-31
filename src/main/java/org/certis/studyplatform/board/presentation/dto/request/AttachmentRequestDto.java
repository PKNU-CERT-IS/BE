package org.certis.studyplatform.board.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttachmentRequestDto {

    private Long id;  // 기존 파일 ID (수정 시에만 존재, 새 파일은 null)

    @NotBlank(message = "파일명은 필수입니다")
    private String name;

    @NotBlank(message = "파일 타입은 필수입니다")
    private String type;

    @NotBlank(message = "파일 크기는 필수입니다")
    private String size;

    @NotBlank(message = "파일 URL은 필수입니다")
    private String attachedUrl;
}