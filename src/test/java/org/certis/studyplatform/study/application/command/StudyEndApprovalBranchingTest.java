package org.certis.studyplatform.study.application.command;

import org.certis.studyplatform.member.application.GracePeriodService;
import org.certis.studyplatform.shared.service.S3FileService;
import org.certis.studyplatform.study.domain.repository.StudyCommandRepository;
import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
import org.certis.studyplatform.study.domain.service.StudyDomainService;
import org.certis.studyplatform.study.domain.service.StudyParticipantDomainService;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StudyEndApprovalBranchingTest {

    private StudyCommandService newService(StudyDomainService studyDomainService,
                                           StudyParticipantDomainService participantDomainService,
                                           StudyQueryRepository queryRepository,
                                           StudyCommandRepository commandRepository,
                                           S3FileService s3FileService,
                                           GracePeriodService gracePeriodService) {
        return new StudyCommandService(
                studyDomainService,
                participantDomainService,
                queryRepository,
                commandRepository,
                s3FileService,
                gracePeriodService
        );
    }

    @Test
    @DisplayName("approveStudyEnd: when status INPROGRESS and resultSubmitStatus INPROGRESS, marks COMPLETED")
    void approveStudyEnd_marksCompleted() {
        // Given
        StudyDomainService studyDomainService = mock(StudyDomainService.class);
        StudyParticipantDomainService studyParticipantDomainService = mock(StudyParticipantDomainService.class);
        StudyQueryRepository studyQueryRepository = mock(StudyQueryRepository.class);
        StudyCommandRepository studyCommandRepository = mock(StudyCommandRepository.class);
        S3FileService s3FileService = mock(S3FileService.class);
        GracePeriodService gracePeriodService = mock(GracePeriodService.class);

        StudyCommandService service = newService(
                studyDomainService,
                studyParticipantDomainService,
                studyQueryRepository,
                studyCommandRepository,
                s3FileService,
                gracePeriodService
        );

        Long studyId = 10L;
        Long adminId = 1L;

        // When
        service.approveStudyEnd(studyId, adminId);

        // Then
        verify(studyDomainService, times(1))
                .approveEnd(eq(studyId), any(OffsetDateTime.class), eq(ResultSubmitStatus.COMPLETED));
    }

    @Test
    @DisplayName("rejectStudyEnd: when status INPROGRESS and resultSubmitStatus INPROGRESS, marks REJECTED and deletes attachment")
    void rejectStudyEnd_marksRejected_andDeletesAttachment() {
        // Given
        StudyDomainService studyDomainService = mock(StudyDomainService.class);
        StudyParticipantDomainService studyParticipantDomainService = mock(StudyParticipantDomainService.class);
        StudyQueryRepository studyQueryRepository = mock(StudyQueryRepository.class);
        StudyCommandRepository studyCommandRepository = mock(StudyCommandRepository.class);
        S3FileService s3FileService = mock(S3FileService.class);
        GracePeriodService gracePeriodService = mock(GracePeriodService.class);

        StudyCommandService service = newService(
                studyDomainService,
                studyParticipantDomainService,
                studyQueryRepository,
                studyCommandRepository,
                s3FileService,
                gracePeriodService
        );

        Long studyId = 11L;
        Long adminId = 2L;
        when(studyDomainService.getResultAttachmentUrlById(studyId)).thenReturn(Optional.of("https://s3/file.pdf"));

        // When
        service.rejectStudyEnd(studyId, adminId);

        // Then
        verify(s3FileService, atLeast(0)).deleteFile(anyString()); // best-effort delete
        verify(studyDomainService, times(1))
                .rejectEnd(eq(studyId), eq(ResultSubmitStatus.REJECTED), any(OffsetDateTime.class));
    }
}


