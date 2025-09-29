package org.certis.studyplatform.study.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.certis.studyplatform.member.domain.MemberGrade;
import java.time.OffsetDateTime;
import org.certis.studyplatform.study.domain.StudyStatus;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStudyEndSubmissionResponseDto {

    private Long studyId;

    private StudyStatus status;

    private ResultSubmitStatus resultSubmitStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime submittedAt;

    private StudyAttachedResponseDto attachment;

    // additional fields for admin view
    private String category;
    private String subCategory;
    private String title;
    private String description;
    private Long creatorId;

    private String studyCreatorName;
    private MemberGrade studyCreatorGrade;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime endedAt;

    private Integer currentParticipantNumber;
    private Integer maxParticipantNumber;
}


