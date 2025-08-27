package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Project Meeting All Request DTO
 *
 * 프로젝트 회의록 전체 목록 조회 요청 데이터
 */
@Getter
@Setter
public class ProjectMeetingAllRequestDto {

    @NotNull(message = "프로젝트 ID는 필수입니다")
    @Positive(message = "프로젝트 ID는 양수여야 합니다")
    private Long projectId;

    // toString for logging
    @Override
    public String toString() {
        return "ProjectMeetingAllRequestDto{" +
                "projectId=" + projectId +
                '}';
    }
} 