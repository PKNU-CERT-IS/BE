package org.certis.studyplatform.study.domain.vo;

import java.time.OffsetDateTime;

/**
 * Study Meeting Link Value Object
 *
 * Clean Architecture Domain Layer
 * 스터디 회의록 링크 도메인 객체
 */
public record StudyMeetingLinkVo(
        Long id,
        Long meetingId,
        Long memberId,
        String name,
        String attachedUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * 생성용 정적 팩토리 메서드
     */
    public static StudyMeetingLinkVo forCreation(Long meetingId, Long memberId, String name, String attachedUrl) {
        validate(name, attachedUrl);
        return new StudyMeetingLinkVo(
                null, // ID는 생성 시 null
                meetingId,
                memberId,
                name,
                attachedUrl,
                null, // createdAt은 Repository에서 설정
                null  // updatedAt은 Repository에서 설정
        );
    }

    /**
     * 수정용 정적 팩토리 메서드
     */
    public static StudyMeetingLinkVo forUpdate(Long id, String name, String attachedUrl) {
        validate(name, attachedUrl);
        return new StudyMeetingLinkVo(
                id,
                null, // meetingId는 변경하지 않음
                null, // memberId는 변경하지 않음
                name,
                attachedUrl,
                null, // createdAt은 변경하지 않음
                null  // updatedAt은 Repository에서 설정
        );
    }

    public static StudyMeetingLinkVo of(Long id, Long meetingId, Long memberId, String name, String attachedUrl, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        validate(name, attachedUrl);
        return new StudyMeetingLinkVo(
                id,
                meetingId,
                memberId, // memberId는 변경하지 않음
                name,
                attachedUrl,
                createdAt,
                updatedAt
        );

    }

    private static void validate(String name, String attachedUrl) {
        if (name == null || name.trim().isEmpty() || name.length() > 30) {
            throw new org.certis.studyplatform.exception.DomainException(org.certis.studyplatform.exception.ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION, "invalid link name");
        }
        if (attachedUrl == null || attachedUrl.trim().isEmpty()) {
            throw new org.certis.studyplatform.exception.DomainException(org.certis.studyplatform.exception.ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION, "invalid link url");
        }
    }
}