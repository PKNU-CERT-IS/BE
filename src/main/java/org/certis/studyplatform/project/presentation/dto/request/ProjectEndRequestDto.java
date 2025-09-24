package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.certis.studyplatform.board.presentation.dto.request.AttachmentRequestDto;

/**
 * Project End Request DTO
 *
 * 프로젝트 종료 요청을 위한 Request DTO
 * Presentation Layer의 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class ProjectEndRequestDto {

    @NotNull(message = "프로젝트 ID는 필수입니다")
    private Long projectId;

    private AttachmentRequestDto attachment;
}
