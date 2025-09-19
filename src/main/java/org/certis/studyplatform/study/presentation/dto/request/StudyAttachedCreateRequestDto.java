package org.certis.studyplatform.study.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import org.certis.studyplatform.shared.type.AttachedType;


@Getter
@Setter
public class StudyAttachedCreateRequestDto {
    @NotNull(message = "스터디 ID는 필수입니다")
    private String name;

    @NotNull(message = "요청자 ID는 필수입니다")
    private AttachedType type;

    private Long size;

    private String attachedUrl;
}
