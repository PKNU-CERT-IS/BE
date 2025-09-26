package org.certis.studyplatform.study.domain.vo;

import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.study.domain.StudyStatus;

import java.time.OffsetDateTime;

public record StudyEndSubmissionInfoVo(
        Long studyId,
        StudyStatus status,
        ResultSubmitStatus resultSubmitStatus,
        OffsetDateTime submittedAt,
        String attachmentUrl,
        // additional context for admin list/detail
        String category,
        String subCategory,
        String title,
        String description,
        Long creatorId,
        String creatorName,
        MemberGrade creatorGrade,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        Integer currentParticipantNumber,
        Integer maxParticipantNumber
) {}


