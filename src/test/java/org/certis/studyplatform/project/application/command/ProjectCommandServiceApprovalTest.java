package org.certis.studyplatform.project.application.command;

import org.certis.studyplatform.member.application.GracePeriodService;
import org.certis.studyplatform.project.domain.service.ProjectDomainService;
import org.certis.studyplatform.project.domain.service.ProjectParticipantDomainService;
import org.certis.studyplatform.project.domain.repository.ProjectCommandRepository;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectEntity;
import org.certis.studyplatform.project.infrastructure.persistence.jpa.ProjectAttachedJpaRepository;
import org.certis.studyplatform.project.infrastructure.persistence.jpa.ProjectJpaRepository;
import org.certis.studyplatform.shared.service.S3FileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProjectCommandServiceApprovalTest {

    @Test
    @DisplayName("approveProjectCreation: status set to APPROVED and startedAt preserved for grace period")
    void approveProjectCreation_setsApproved_preservesStartedAt() {
        // Given
        ProjectDomainService projectDomainService = mock(ProjectDomainService.class);
        ProjectParticipantDomainService projectParticipantDomainService = mock(ProjectParticipantDomainService.class);
        ProjectCommandRepository projectCommandRepository = mock(ProjectCommandRepository.class);
        S3FileService s3FileService = mock(S3FileService.class);
        GracePeriodService gracePeriodService = mock(GracePeriodService.class);
        ProjectAttachedJpaRepository projectAttachedJpaRepository = mock(ProjectAttachedJpaRepository.class);
        ProjectJpaRepository projectJpaRepository = mock(ProjectJpaRepository.class);

        ProjectCommandService service = new ProjectCommandService(
                projectDomainService,
                projectParticipantDomainService,
                projectCommandRepository,
                s3FileService,
                gracePeriodService,
                projectAttachedJpaRepository,
                projectJpaRepository
        );

        Long projectId = 10L;
        Long adminId = 5L;
        OffsetDateTime originalStartedAt = OffsetDateTime.now().plusDays(3); // future
        OffsetDateTime originalEndedAt = originalStartedAt.plusWeeks(4);

        ProjectEntity entity = ProjectEntity.builder()
                .id(projectId)
                .startedAt(originalStartedAt)
                .endedAt(originalEndedAt)
                .build();

        doNothing().when(projectCommandRepository).approveCreation(eq(projectId));
        when(projectJpaRepository.findById(projectId)).thenReturn(Optional.of(entity));

        // When
        service.approveProjectCreation(projectId, adminId);

        // Then
        verify(projectCommandRepository, times(1)).approveCreation(eq(projectId));

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<OffsetDateTime> startCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> endCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(gracePeriodService, times(1))
                .extendGracePeriodForApprovedProject(idCaptor.capture(), startCaptor.capture(), endCaptor.capture());

        assertThat(idCaptor.getValue()).isEqualTo(projectId);
        assertThat(startCaptor.getValue()).isEqualTo(originalStartedAt);
        assertThat(endCaptor.getValue()).isEqualTo(originalEndedAt);
    }
}


