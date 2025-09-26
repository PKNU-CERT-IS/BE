package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminProjectApprovalRequestDto {

    @NotNull
    private Long projectId;
}


