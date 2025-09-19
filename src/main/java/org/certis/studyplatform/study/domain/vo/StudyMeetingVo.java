package org.certis.studyplatform.study.domain.vo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Study Meeting Value Object
 *
 * 스터디 회의록 상세 정보를 나타내는 도메인 VO
 * Domain Layer의 불변 객체
 */
public record StudyMeetingVo(
    Long id,
    Long studyId,
    String title,
    String content,
    List<Long> participantIds,
    Long writerId,
    boolean isEditable,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    private static final int MAX_TITLE_LENGTH = 30;
    /**
     * 기본 팩토리 메서드
     */
    public static StudyMeetingVo of(
            Long id,
            Long studyId,
            String title,
            String content,
            List<Long> participantIds,
            Long writerId,
            boolean isEditable,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        if (title == null || title.trim().isEmpty() || title.length() > MAX_TITLE_LENGTH) {
            throw new org.certis.studyplatform.exception.DomainException(org.certis.studyplatform.exception.ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION, "invalid meeting title");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new org.certis.studyplatform.exception.DomainException(org.certis.studyplatform.exception.ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION, "invalid meeting content");
        }
        if (participantIds == null) {
            throw new org.certis.studyplatform.exception.DomainException(org.certis.studyplatform.exception.ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION, "participants must not be null");
        }
        return new StudyMeetingVo(
            id, studyId, title, content,
            participantIds, writerId, isEditable,
            createdAt, updatedAt
        );
    }
} 