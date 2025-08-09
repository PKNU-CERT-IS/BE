package org.certis.studyplatform.member.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 프로필용 스터디 응답 DTO
 * 내가 관여한 스터디 정보
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileStudyResponseDto {

    private Long studyId;
    private String title;
    private String description;
    private String status; // RECRUITING, IN_PROGRESS, COMPLETED, CANCELLED
    private String role; // LEADER, MEMBER
    private OffsetDateTime joinedAt;
    private OffsetDateTime studyStartDate;
    private OffsetDateTime studyEndDate;
    private Integer memberCount;
    private Integer maxMembers;
    private String[] tags;
}
