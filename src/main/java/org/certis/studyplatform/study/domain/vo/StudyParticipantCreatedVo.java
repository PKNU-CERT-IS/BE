package org.certis.studyplatform.study.domain.vo;

import org.certis.studyplatform.study.domain.StudyParticipantStatus;

import java.time.OffsetDateTime;

public record StudyParticipantCreatedVo(
        Long id,
        Long studyId,
        Long memberId,
        StudyParticipantStatus status,
        OffsetDateTime createdAt
) {

    public static StudyParticipantCreatedVo of(Long id,
                                               Long studyId,
                                               Long memberId,
                                               StudyParticipantStatus status,
                                                 OffsetDateTime createdAt) {
        return new StudyParticipantCreatedVo(
                id,
                studyId,
                memberId,
                status,
                createdAt
        );
    }
}
