package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import org.certis.studyplatform.shared.type.AttachedType;


@Getter
@Setter
public class ProjectAttachedCreateRequestDto {
    @NotNull(message = "프로젝트 ID는 필수입니다")
    private String name;

    @NotNull(message = "요청자 ID는 필수입니다")
    private AttachedType type;

    private Long size;

    private String attachedUrl;
}
