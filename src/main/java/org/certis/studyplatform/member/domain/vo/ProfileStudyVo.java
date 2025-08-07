package org.certis.studyplatform.member.domain.vo;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 스터디 프로필 Value Object (Record)
 * Domain Layer에서 사용하는 불변 데이터 객체
 */
public record ProfileStudyVo(
        Long studyId,
        String title,
        String description,
        String status,
        String role,
        ZonedDateTime joinedAt,
        ZonedDateTime studyStartDate,
        ZonedDateTime studyEndDate,
        String meetingUrl,
        Integer memberCount,
        List<String> tags,
        String category
) {

    public ProfileStudyVo {
        tags = tags != null ? List.copyOf(tags) : List.of();
    }

    public boolean isInProgress() {
        return "IN_PROGRESS".equals(status);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isStudyLeader() {
        return "STUDY_LEADER".equals(role);
    }

    public boolean hasOnlineMeeting() {
        return meetingUrl != null && !meetingUrl.trim().isEmpty();
    }

    public String[] getTagsArray() {
        return tags.toArray(new String[0]);
    }
}
