package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProjectJoinCancelRequestDto {

    @NotNull(message = "프로젝트 ID는 필수입니다")
    private Long projectId;
}