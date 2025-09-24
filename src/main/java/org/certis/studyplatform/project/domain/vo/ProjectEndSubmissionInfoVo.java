package org.certis.studyplatform.project.domain.vo;

import org.certis.studyplatform.shared.domain.ResultSubmitStatus;

import java.time.OffsetDateTime;

public record ProjectEndSubmissionInfoVo(
        Long projectId,
        ResultSubmitStatus status,
        OffsetDateTime submittedAt,
        String attachmentUrl
) {}


