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
        Long studyId,
        Long memberId,
        String name,
        String attachedUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * 생성용 정적 팩토리 메서드
     */
    public static StudyMeetingLinkVo forCreation(Long studyId, Long memberId, String name, String attachedUrl) {
        return new StudyMeetingLinkVo(
                null, // ID는 생성 시 null
                studyId,
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
        return new StudyMeetingLinkVo(
                id,
                null, // studyId는 변경하지 않음
                null, // memberId는 변경하지 않음
                name,
                attachedUrl,
                null, // createdAt은 변경하지 않음
                null  // updatedAt은 Repository에서 설정
        );
    }

    public static StudyMeetingLinkVo of(Long id, Long studyId, Long memberId, String name, String attachedUrl, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return new StudyMeetingLinkVo(
                id,
                studyId,
                memberId, // memberId는 변경하지 않음
                name,
                attachedUrl,
                null, // createdAt은 변경하지 않음
                null  // updatedAt은 Repository에서 설정
        );

    }
}