package org.certis.studyplatform.board.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttachmentResponseDto {

    private Long id;
    private String name;
    private String type;
    private String size;
    private String attachedUrl;
}