package org.certis.studyplatform.study.domain.service;

import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.certis.studyplatform.shared.domain.event.ProgressStatusUpdateEvent;
import org.certis.studyplatform.shared.domain.service.ProgressStatusService;
import org.certis.studyplatform.study.domain.StudyStatus;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyEntity;
import org.certis.studyplatform.study.domain.repository.StudyCommandRepository;
import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
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
 * StudyProgressStatusEventListener 테스트
 */
@ExtendWith(MockitoExtension.class)
class StudyProgressStatusEventListenerTest {

    @Mock
    private StudyQueryRepository studyQueryRepository;

    @Mock
    private StudyCommandRepository studyCommandRepository;

    @Mock
    private ProgressStatusService progressStatusService;

    private StudyProgressStatusEventListener eventListener;

    @BeforeEach
    void setUp() {
        eventListener = new StudyProgressStatusEventListener(
            studyQueryRepository,
            studyCommandRepository,
            progressStatusService
        );
    }

    @Test
    @DisplayName("Study 상태 업데이트 이벤트 처리 - 상태 변경됨")
    void handleProgressStatusUpdate_statusChanged() {
        // Given
        Long studyId = 1L;
        OffsetDateTime currentTime = OffsetDateTime.now();
        ProgressStatusUpdateEvent event = ProgressStatusUpdateEvent.forStudy(studyId, currentTime);

        StudyEntity studyEntity = StudyEntity.builder()
            .id(studyId)
            .status(StudyStatus.APPROVED)
            .resultSubmitStatus(ResultSubmitStatus.READY)
            .startedAt(currentTime.minusHours(1))
            .endedAt(currentTime.plusDays(1))
            .build();

        when(studyQueryRepository.findEntityById(studyId)).thenReturn(Optional.of(studyEntity));
        when(progressStatusService.calculateStudyStatus(
            any(), any(), any(), any(), any()
        )).thenReturn(new ProgressStatusService.ProgressStatusResult(
            StudyStatus.INPROGRESS, ResultSubmitStatus.READY
        ));

        // When
        eventListener.handleProgressStatusUpdate(event);

        // Then
        ArgumentCaptor<StudyEntity> captor = ArgumentCaptor.forClass(StudyEntity.class);
        verify(studyCommandRepository).save(captor.capture());
        
        StudyEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getStatus()).isEqualTo(StudyStatus.INPROGRESS);
        assertThat(savedEntity.getResultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
    }

    @Test
    @DisplayName("Study 상태 업데이트 이벤트 처리 - 상태 변경 없음")
    void handleProgressStatusUpdate_noStatusChange() {
        // Given
        Long studyId = 1L;
        OffsetDateTime currentTime = OffsetDateTime.now();
        ProgressStatusUpdateEvent event = ProgressStatusUpdateEvent.forStudy(studyId, currentTime);

        StudyEntity studyEntity = StudyEntity.builder()
            .id(studyId)
            .status(StudyStatus.APPROVED)
            .resultSubmitStatus(ResultSubmitStatus.READY)
            .startedAt(currentTime.minusHours(1))
            .endedAt(currentTime.plusDays(1))
            .build();

        when(studyQueryRepository.findEntityById(studyId)).thenReturn(Optional.of(studyEntity));
        when(progressStatusService.calculateStudyStatus(
            any(), any(), any(), any(), any()
        )).thenReturn(new ProgressStatusService.ProgressStatusResult(
            StudyStatus.APPROVED, ResultSubmitStatus.READY
        ));

        // When
        eventListener.handleProgressStatusUpdate(event);

        // Then
        verify(studyCommandRepository, never()).save(any(org.certis.studyplatform.study.infrastructure.persistence.entity.StudyEntity.class));
    }

    @Test
    @DisplayName("Study 상태 업데이트 이벤트 처리 - Study 없음")
    void handleProgressStatusUpdate_studyNotFound() {
        // Given
        Long studyId = 1L;
        OffsetDateTime currentTime = OffsetDateTime.now();
        ProgressStatusUpdateEvent event = ProgressStatusUpdateEvent.forStudy(studyId, currentTime);

        when(studyQueryRepository.findEntityById(studyId)).thenReturn(Optional.empty());

        // When
        eventListener.handleProgressStatusUpdate(event);

        // Then
        verify(studyCommandRepository, never()).save(any(org.certis.studyplatform.study.infrastructure.persistence.entity.StudyEntity.class));
        verify(progressStatusService, never()).calculateStudyStatus(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Study 상태 업데이트 이벤트 처리 - 시작 시간 이전")
    void handleProgressStatusUpdate_beforeStartTime() {
        // Given
        Long studyId = 1L;
        OffsetDateTime currentTime = OffsetDateTime.now();
        ProgressStatusUpdateEvent event = ProgressStatusUpdateEvent.forStudy(studyId, currentTime);

        StudyEntity studyEntity = StudyEntity.builder()
            .id(studyId)
            .status(StudyStatus.APPROVED)
            .resultSubmitStatus(ResultSubmitStatus.READY)
            .startedAt(currentTime.plusHours(1)) // 미래 시간
            .endedAt(currentTime.plusDays(1))
            .build();

        when(studyQueryRepository.findEntityById(studyId)).thenReturn(Optional.of(studyEntity));

        // When
        eventListener.handleProgressStatusUpdate(event);

        // Then
        verify(studyCommandRepository, never()).save(any(org.certis.studyplatform.study.infrastructure.persistence.entity.StudyEntity.class));
        verify(progressStatusService, never()).calculateStudyStatus(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Project 이벤트는 무시됨")
    void handleProgressStatusUpdate_ignoresProjectEvent() {
        // Given
        Long projectId = 1L;
        OffsetDateTime currentTime = OffsetDateTime.now();
        ProgressStatusUpdateEvent event = ProgressStatusUpdateEvent.forProject(projectId, currentTime);

        // When
        eventListener.handleProgressStatusUpdate(event);

        // Then
        verify(studyQueryRepository, never()).findById(any());
        verify(studyCommandRepository, never()).save(any(org.certis.studyplatform.study.infrastructure.persistence.entity.StudyEntity.class));
        verify(progressStatusService, never()).calculateStudyStatus(any(), any(), any(), any(), any());
    }
}
