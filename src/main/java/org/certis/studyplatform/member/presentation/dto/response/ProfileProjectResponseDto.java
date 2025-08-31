package org.certis.studyplatform.member.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.project.domain.ProjectStatus;

import java.time.OffsetDateTime;

/**
 * 프로필용 프로젝트 응답 DTO
 * 내가 관여한 프로젝트 정보
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileProjectResponseDto {

    private Long projectId;
    private String title;
    private String description;
    private ProjectStatus projectStatus;
    private OffsetDateTime projectStartDate;
    private OffsetDateTime projectEndDate;
    private String[] tags;
}
