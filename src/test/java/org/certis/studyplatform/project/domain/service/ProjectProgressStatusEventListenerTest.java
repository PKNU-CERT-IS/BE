package org.certis.studyplatform.project.domain.service;

import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.certis.studyplatform.shared.domain.event.ProgressStatusUpdateEvent;
import org.certis.studyplatform.shared.domain.service.ProgressStatusService;
import org.certis.studyplatform.project.domain.ProjectStatus;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectEntity;
import org.certis.studyplatform.project.domain.repository.ProjectCommandRepository;
import org.certis.studyplatform.project.domain.repository.ProjectQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProjectProgressStatusEventListener 테스트
 */
@ExtendWith(MockitoExtension.class)
class ProjectProgressStatusEventListenerTest {

    @Mock
    private ProjectQueryRepository projectQueryRepository;

    @Mock
    private ProjectCommandRepository projectCommandRepository;

    @Mock
    private ProgressStatusService progressStatusService;

    private ProjectProgressStatusEventListener eventListener;

    @BeforeEach
    void setUp() {
        eventListener = new ProjectProgressStatusEventListener(
            projectQueryRepository,
            projectCommandRepository,
            progressStatusService
        );
    }

    @Test
    @DisplayName("Project 상태 업데이트 이벤트 처리 - 상태 변경됨")
    void handleProgressStatusUpdate_statusChanged() {
        // Given
        Long projectId = 1L;
        OffsetDateTime currentTime = OffsetDateTime.now();
        ProgressStatusUpdateEvent event = ProgressStatusUpdateEvent.forProject(projectId, currentTime);

        ProjectEntity projectEntity = ProjectEntity.builder()
            .id(projectId)
            .status(ProjectStatus.APPROVED)
            .resultSubmitStatus(ResultSubmitStatus.READY)
            .startedAt(currentTime.minusHours(1))
            .endedAt(currentTime.plusDays(1))
            .build();

        when(projectQueryRepository.findEntityById(projectId)).thenReturn(Optional.of(projectEntity));
        when(progressStatusService.calculateProjectStatus(
            any(), any(), any(), any(), any()
        )).thenReturn(new ProgressStatusService.ProgressStatusResult(
            ProjectStatus.INPROGRESS, ResultSubmitStatus.READY
        ));

        // When
        eventListener.handleProgressStatusUpdate(event);

        // Then
        ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
        verify(projectCommandRepository).save(captor.capture());
        
        ProjectEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getStatus()).isEqualTo(ProjectStatus.INPROGRESS);
        assertThat(savedEntity.getResultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
    }

    @Test
    @DisplayName("Project 상태 업데이트 이벤트 처리 - 상태 변경 없음")
    void handleProgressStatusUpdate_noStatusChange() {
        // Given
        Long projectId = 1L;
        OffsetDateTime currentTime = OffsetDateTime.now();
        ProgressStatusUpdateEvent event = ProgressStatusUpdateEvent.forProject(projectId, currentTime);

        ProjectEntity projectEntity = ProjectEntity.builder()
            .id(projectId)
            .status(ProjectStatus.APPROVED)
            .resultSubmitStatus(ResultSubmitStatus.READY)
            .startedAt(currentTime.minusHours(1))
            .endedAt(currentTime.plusDays(1))
            .build();

        when(projectQueryRepository.findEntityById(projectId)).thenReturn(Optional.of(projectEntity));
        when(progressStatusService.calculateProjectStatus(
            any(), any(), any(), any(), any()
        )).thenReturn(new ProgressStatusService.ProgressStatusResult(
            ProjectStatus.APPROVED, ResultSubmitStatus.READY
        ));

        // When
        eventListener.handleProgressStatusUpdate(event);

        // Then
        verify(projectCommandRepository, never()).save(any(org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectEntity.class));
    }

    @Test
    @DisplayName("Project 상태 업데이트 이벤트 처리 - Project 없음")
    void handleProgressStatusUpdate_projectNotFound() {
        // Given
        Long projectId = 1L;
        OffsetDateTime currentTime = OffsetDateTime.now();
        ProgressStatusUpdateEvent event = ProgressStatusUpdateEvent.forProject(projectId, currentTime);

        when(projectQueryRepository.findEntityById(projectId)).thenReturn(Optional.empty());

        // When
        eventListener.handleProgressStatusUpdate(event);

        // Then
        verify(projectCommandRepository, never()).save(any(org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectEntity.class));
        verify(progressStatusService, never()).calculateProjectStatus(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Project 상태 업데이트 이벤트 처리 - 시작 시간 이전")
    void handleProgressStatusUpdate_beforeStartTime() {
        // Given
        Long projectId = 1L;
        OffsetDateTime currentTime = OffsetDateTime.now();
        ProgressStatusUpdateEvent event = ProgressStatusUpdateEvent.forProject(projectId, currentTime);

        ProjectEntity projectEntity = ProjectEntity.builder()
            .id(projectId)
            .status(ProjectStatus.APPROVED)
            .resultSubmitStatus(ResultSubmitStatus.READY)
            .startedAt(currentTime.plusHours(1)) // 미래 시간
            .endedAt(currentTime.plusDays(1))
            .build();

        when(projectQueryRepository.findEntityById(projectId)).thenReturn(Optional.of(projectEntity));

        // When
        eventListener.handleProgressStatusUpdate(event);

        // Then
        verify(projectCommandRepository, never()).save(any(org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectEntity.class));
        verify(progressStatusService, never()).calculateProjectStatus(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Study 이벤트는 무시됨")
    void handleProgressStatusUpdate_ignoresStudyEvent() {
        // Given
        Long studyId = 1L;
        OffsetDateTime currentTime = OffsetDateTime.now();
        ProgressStatusUpdateEvent event = ProgressStatusUpdateEvent.forStudy(studyId, currentTime);

        // When
        eventListener.handleProgressStatusUpdate(event);

        // Then
        verify(projectQueryRepository, never()).findById(any());
        verify(projectCommandRepository, never()).save(any(org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectEntity.class));
        verify(progressStatusService, never()).calculateProjectStatus(any(), any(), any(), any(), any());
    }
}
