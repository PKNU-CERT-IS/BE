package org.certis.studyplatform.study.application.command;

import org.certis.studyplatform.member.application.GracePeriodService;
import org.certis.studyplatform.shared.service.S3FileService;
import org.certis.studyplatform.study.domain.repository.StudyCommandRepository;
import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
import org.certis.studyplatform.study.domain.service.StudyDomainService;
import org.certis.studyplatform.study.domain.service.StudyParticipantDomainService;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StudyCommandServiceApprovalTest {

    @Test
    @DisplayName("approveStudyCreation: status set to APPROVED and startedAt preserved for grace period")
    void approveStudyCreation_setsApproved_preservesStartedAt() {
        // Given
        StudyDomainService studyDomainService = mock(StudyDomainService.class);
        StudyParticipantDomainService studyParticipantDomainService = mock(StudyParticipantDomainService.class);
        StudyQueryRepository studyQueryRepository = mock(StudyQueryRepository.class);
        StudyCommandRepository studyCommandRepository = mock(StudyCommandRepository.class);
        S3FileService s3FileService = mock(S3FileService.class);
        GracePeriodService gracePeriodService = mock(GracePeriodService.class);

        StudyCommandService service = new StudyCommandService(
                studyDomainService,
                studyParticipantDomainService,
                studyQueryRepository,
                studyCommandRepository,
                s3FileService,
                gracePeriodService
        );

        Long studyId = 100L;
        Long adminId = 7L;
        OffsetDateTime startedAt = OffsetDateTime.now().plusDays(2);
        OffsetDateTime endedAt = startedAt.plusWeeks(4);

        doNothing().when(studyCommandRepository).approveCreation(eq(studyId));
        when(studyDomainService.getStudyById(any())).thenReturn(
                StudyVo.of(
                        studyId,
                        "title",
                        "desc",
                        "content",
                        "cat",
                        "sub",
                        startedAt,
                        endedAt,
                        null,
                        null,
                        1L,
                        "creator",
                        null,
                        null,
                        "APPROVED",
                        ResultSubmitStatus.READY.toString(),
                        10,
                        0,
                        false,
                        Collections.emptyList()
                )
        );

        // When
        service.approveStudyCreation(studyId, adminId);

        // Then
        verify(studyCommandRepository, times(1)).approveCreation(eq(studyId));
        verify(gracePeriodService, times(1)).extendGracePeriodForApprovedStudy(eq(studyId), eq(startedAt), eq(endedAt));
    }
}


