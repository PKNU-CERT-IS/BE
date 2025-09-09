package org.certis.studyplatform.study.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Study Join Request DTO
 *
 * 스터디 참가 신청 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class StudyJoinRequestDto {

    @NotNull(message = "스터디 ID는 필수입니다")
    private Long studyId;
}