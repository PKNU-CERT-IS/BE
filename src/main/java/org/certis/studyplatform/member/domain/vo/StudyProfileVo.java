
package org.certis.studyplatform.member.domain.vo;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 스터디 프로필 Value Object (Record)
 * Domain Layer에서 사용하는 불변 데이터 객체
 *
 * Record를 사용하여 불변성, equals, hashCode, toString 자동 제공
 */
public record StudyProfileVo(
        Long studyId,
        String title,
        String description,
        String status, // RECRUITING, IN_PROGRESS, COMPLETED, CANCELLED
        String role, // LEADER, MEMBER
        ZonedDateTime joinedAt,
        ZonedDateTime studyStartDate,
        ZonedDateTime studyEndDate,
        Integer memberCount,
        Integer maxMembers,
        List<String> tags
) {

    /**
     * 방어적 복사를 위한 정규화 생성자
     */
    public StudyProfileVo {
        // List의 불변성 보장
        tags = tags != null ? List.copyOf(tags) : List.of();
    }

    /**
     * 편의 생성자 - tags 없이
     */
    public StudyProfileVo(Long studyId, String title, String description, String status,
                          String role, ZonedDateTime joinedAt, ZonedDateTime studyStartDate,
                          ZonedDateTime studyEndDate, Integer memberCount, Integer maxMembers) {
        this(studyId, title, description, status, role, joinedAt, studyStartDate,
                studyEndDate, memberCount, maxMembers, List.of());
    }

    /**
     * 스터디가 진행 중인지 확인
     */
    public boolean isInProgress() {
        return "IN_PROGRESS".equals(status);
    }

    /**
     * 스터디가 완료되었는지 확인
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    /**
     * 모집 중인지 확인
     */
    public boolean isRecruiting() {
        return "RECRUITING".equals(status);
    }

    /**
     * 리더인지 확인
     */
    public boolean isLeader() {
        return "LEADER".equals(role);
    }

    /**
     * 멤버인지 확인
     */
    public boolean isMember() {
        return "MEMBER".equals(role);
    }

    /**
     * 태그 배열 반환 (Presentation Layer 호환)
     */
    public String[] getTagsArray() {
        return tags.toArray(new String[0]);
    }

    /**
     * 정원이 찼는지 확인
     */
    public boolean isFull() {
        return memberCount != null && maxMembers != null &&
                memberCount.equals(maxMembers);
    }

    /**
     * 스터디 기간 내에 있는지 확인
     */
    public boolean isWithinStudyPeriod() {
        if (studyStartDate == null || studyEndDate == null) {
            return false;
        }
        ZonedDateTime now = ZonedDateTime.now();
        return !now.isBefore(studyStartDate) && !now.isAfter(studyEndDate);
    }
}

