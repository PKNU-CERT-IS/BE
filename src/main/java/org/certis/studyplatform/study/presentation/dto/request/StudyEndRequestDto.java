package org.certis.studyplatform.study.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.certis.studyplatform.board.presentation.dto.request.AttachmentRequestDto;

/**
 * Study End Request DTO
 *
 * 스터디 종료 요청을 위한 Request DTO
 * Presentation Layer의 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class StudyEndRequestDto {

    @NotNull(message = "스터디 ID는 필수입니다")
    private Long studyId;

    private AttachmentRequestDto attachment;
}
