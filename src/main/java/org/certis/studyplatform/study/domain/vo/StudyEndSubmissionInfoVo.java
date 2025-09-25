package org.certis.studyplatform.study.domain.vo;

import org.certis.studyplatform.shared.domain.ResultSubmitStatus;

import java.time.OffsetDateTime;

public record StudyEndSubmissionInfoVo(
        Long studyId,
        ResultSubmitStatus status,
        OffsetDateTime submittedAt,
        String attachmentUrl,
        // additional context for admin list/detail
        String category,
        String subCategory,
        String title,
        String description,
        Long creatorId,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        Integer currentParticipantNumber,
        Integer maxParticipantNumber
) {}


