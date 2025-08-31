package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProjectJoinApproveRequestDto {

    @NotNull(message = "참가자 ID는 필수입니다")
    private Long participantId;
}