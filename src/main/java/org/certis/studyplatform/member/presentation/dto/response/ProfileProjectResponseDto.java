package org.certis.studyplatform.member.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private String status; // PLANNING, IN_PROGRESS, COMPLETED, ON_HOLD, CANCELLED
    private String role; // PROJECT_LEADER, TECH_LEADER, MEMBER
    private OffsetDateTime joinedAt;
    private OffsetDateTime projectStartDate;
    private OffsetDateTime projectEndDate;
    private String repositoryUrl;
    private String deployUrl;
    private Integer memberCount;
    private String[] techStack;
}
