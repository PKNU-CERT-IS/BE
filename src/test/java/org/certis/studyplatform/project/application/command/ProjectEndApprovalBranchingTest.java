package org.certis.studyplatform.project.application.command;

import org.certis.studyplatform.member.application.GracePeriodService;
import org.certis.studyplatform.project.domain.repository.ProjectCommandRepository;
import org.certis.studyplatform.project.domain.service.ProjectDomainService;
import org.certis.studyplatform.project.domain.service.ProjectParticipantDomainService;
import org.certis.studyplatform.project.infrastructure.persistence.jpa.ProjectJpaRepository;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.certis.studyplatform.shared.service.S3FileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProjectEndApprovalBranchingTest {

    private ProjectCommandService newService(ProjectDomainService domainService,
                                             ProjectParticipantDomainService participantDomainService,
                                             ProjectCommandRepository commandRepository,
                                             S3FileService s3FileService,
                                             GracePeriodService gracePeriodService,
                                             ProjectJpaRepository jpaRepository) {
        return new ProjectCommandService(
                domainService,
                participantDomainService,
                commandRepository,
                s3FileService,
                gracePeriodService,
                null,
                jpaRepository
        );
    }

    @Test
    @DisplayName("approveProjectEnd: when status INPROGRESS and resultSubmitStatus INPROGRESS, marks COMPLETED")
    void approveProjectEnd_marksCompleted() {
        // Given
        ProjectDomainService projectDomainService = mock(ProjectDomainService.class);
        ProjectParticipantDomainService projectParticipantDomainService = mock(ProjectParticipantDomainService.class);
        ProjectCommandRepository projectCommandRepository = mock(ProjectCommandRepository.class);
        S3FileService s3FileService = mock(S3FileService.class);
        GracePeriodService gracePeriodService = mock(GracePeriodService.class);
        ProjectJpaRepository projectJpaRepository = mock(ProjectJpaRepository.class);

        ProjectCommandService service = newService(
                projectDomainService,
                projectParticipantDomainService,
                projectCommandRepository,
                s3FileService,
                gracePeriodService,
                projectJpaRepository
        );

        Long projectId = 20L;
        Long adminId = 1L;

        // When
        service.approveProjectEnd(projectId, adminId);

        // Then
        verify(projectJpaRepository, times(1))
                .approveEnd(eq(projectId), any(OffsetDateTime.class), eq(ResultSubmitStatus.COMPLETED));
    }

    @Test
    @DisplayName("rejectProjectEnd: when status INPROGRESS and resultSubmitStatus INPROGRESS, marks REJECTED and deletes attachment")
    void rejectProjectEnd_marksRejected_andDeletesAttachment() {
        // Given
        ProjectDomainService projectDomainService = mock(ProjectDomainService.class);
        ProjectParticipantDomainService projectParticipantDomainService = mock(ProjectParticipantDomainService.class);
        ProjectCommandRepository projectCommandRepository = mock(ProjectCommandRepository.class);
        S3FileService s3FileService = mock(S3FileService.class);
        GracePeriodService gracePeriodService = mock(GracePeriodService.class);
        ProjectJpaRepository projectJpaRepository = mock(ProjectJpaRepository.class);

        ProjectCommandService service = newService(
                projectDomainService,
                projectParticipantDomainService,
                projectCommandRepository,
                s3FileService,
                gracePeriodService,
                projectJpaRepository
        );

        Long projectId = 21L;
        Long adminId = 2L;
        when(projectJpaRepository.findById(projectId)).thenReturn(Optional.empty());

        // When
        service.rejectProjectEnd(projectId, adminId);

        // Then
        verify(projectJpaRepository, times(1))
                .rejectEnd(eq(projectId), eq(ResultSubmitStatus.REJECTED), any(OffsetDateTime.class));
        verify(projectJpaRepository, times(1))
                .rejectCompletely(eq(projectId), any(OffsetDateTime.class));
    }
}


