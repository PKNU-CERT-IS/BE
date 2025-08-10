package org.certis.studyplatform.member.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.domain.MemberRole;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 내 프로필 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileInfoResponseDto {
    private Long memberId;
    private String name;
    private String description;
    private String profileImage;
    private List<OffsetDateTime> todaySchedules;
    private Integer penaltyCount;
    private OffsetDateTime gracePeriod;
    private MemberRole memberRole;
    private MemberGrade memberGrade;
    private List<String> skills;
    private OffsetDateTime createdAt;
}
