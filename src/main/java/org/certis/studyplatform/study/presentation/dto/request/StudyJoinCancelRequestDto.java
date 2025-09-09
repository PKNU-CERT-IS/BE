package org.certis.studyplatform.study.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudyJoinCancelRequestDto {

    @NotNull(message = "스터디 ID는 필수입니다")
    private Long studyId;
}