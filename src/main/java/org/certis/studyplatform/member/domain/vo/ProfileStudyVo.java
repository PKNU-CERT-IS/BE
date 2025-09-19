package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.study.domain.StudyStatus;

import java.time.OffsetDateTime;
import java.util.Arrays;

/**
 * 스터디 프로필 Value Object (Record)
 * Domain Layer에서 사용하는 불변 데이터 객체
 */
public record ProfileStudyVo(
        Long studyId,
        String title,
        String description,
        StudyStatus studyStatus,
        OffsetDateTime studyStartDate,
        OffsetDateTime studyEndDate,
        String[] tags,
        
        // Category information
        String category,
        String subcategory
) {

    /**
     * 방어적 복사를 위한 정규화 생성자
     */
    public ProfileStudyVo {
        tags = tags != null ? tags.clone() : new String[0];
    }

    public boolean isInProgress() {
        return StudyStatus.INPROGRESS.equals(studyStatus);
    }

    public boolean isCompleted() {
        return StudyStatus.COMPLETED.equals(studyStatus);
    }

    public boolean isReady() {
        return StudyStatus.READY.equals(studyStatus);
    }

    public boolean isRejected() {
        return StudyStatus.REJECTED.equals(studyStatus);
    }

    public boolean isActive() {
        return studyStatus != null && studyStatus.isActive();
    }

    public String[] getTagsArray() {
        return tags.clone();
    }

    /**
     * 스터디 기간 내에 있는지 확인
     */
    public boolean isWithinStudyPeriod() {
        if (studyStartDate == null || studyEndDate == null) {
            return false;
        }
        OffsetDateTime now = OffsetDateTime.now();
        return !now.isBefore(studyStartDate) && !now.isAfter(studyEndDate);
    }

    /**
     * 특정 기술을 사용하는지 확인
     */
    public boolean usesTechnology(String technology) {
        if (technology == null || tags == null) {
            return false;
        }
        return Arrays.stream(tags)
                .anyMatch(tag -> tag.equalsIgnoreCase(technology));
    }
}
