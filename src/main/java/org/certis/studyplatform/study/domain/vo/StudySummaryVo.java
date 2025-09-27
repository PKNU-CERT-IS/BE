package org.certis.studyplatform.study.domain.vo;

import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Study Summary Value Object
 *
 * 프로젝트 목록 조회 시 사용되는 요약 정보
 */
public record StudySummaryVo(
    Long id,
    String title,
    String description,
    String category,
    String subcategory,
    OffsetDateTime startDate,
    OffsetDateTime endDate,
    String studyCreatorName,
    MemberGrade studyCreatorGrade,
    String semester,
    String status,
    boolean isParticipantable,
    List<StudyAttachedVo> attachedVo,
    Integer maxParticipants,
    Integer currentParticipants,
    ResultSubmitStatus resultSubmitStatus
) {
    public static StudySummaryVo of(
        Long id,
        String title,
        String description,
        String category,
        String subcategory,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        String studyCreatorName,
        MemberGrade studyCreatorRole,
        String semester,
        String status,
        boolean isParticipantable,
        List<StudyAttachedVo> attachedVo,
        Integer maxParticipants,
        Integer currentParticipants,
        ResultSubmitStatus resultSubmitStatus
    ) {
        return new StudySummaryVo(
            id, title, description, category, subcategory,
            startDate, endDate, studyCreatorName, studyCreatorRole,
            semester, status, isParticipantable, attachedVo, maxParticipants, currentParticipants, resultSubmitStatus
        );
    }
}