package org.certis.studyplatform.shared.domain.service;

import org.certis.studyplatform.shared.domain.event.ProgressStatusUpdateEvent;
import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
import org.certis.studyplatform.project.domain.repository.ProjectQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * ProgressStatusScheduler 테스트
 */
@ExtendWith(MockitoExtension.class)
class ProgressStatusSchedulerTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private StudyQueryRepository studyQueryRepository;

    @Mock
    private ProjectQueryRepository projectQueryRepository;

    private ProgressStatusScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ProgressStatusScheduler(
            eventPublisher,
            studyQueryRepository,
            projectQueryRepository
        );
    }

    @Test
    @DisplayName("스케줄러 실행 - Study와 Project 이벤트 발행")
    void updateProgressStatus_publishesEvents() {
        // Given
        List<Long> studyIds = List.of(1L, 2L);
        List<Long> projectIds = List.of(3L, 4L);

        when(studyQueryRepository.findApprovedStudiesStartedBefore(any()))
            .thenReturn(studyIds);
        when(projectQueryRepository.findApprovedProjectsStartedBefore(any()))
            .thenReturn(projectIds);

        // When
        scheduler.updateProgressStatus();

        // Then
        ArgumentCaptor<ProgressStatusUpdateEvent> eventCaptor = 
            ArgumentCaptor.forClass(ProgressStatusUpdateEvent.class);
        
        verify(eventPublisher, times(4)).publishEvent(eventCaptor.capture());
        
        List<ProgressStatusUpdateEvent> events = eventCaptor.getAllValues();
        
        // Study 이벤트 확인
        assertThat(events).filteredOn(event -> "STUDY".equals(event.getEntityType()))
            .hasSize(2)
            .extracting(ProgressStatusUpdateEvent::getEntityId)
            .containsExactlyInAnyOrder(1L, 2L);
        
        // Project 이벤트 확인
        assertThat(events).filteredOn(event -> "PROJECT".equals(event.getEntityType()))
            .hasSize(2)
            .extracting(ProgressStatusUpdateEvent::getEntityId)
            .containsExactlyInAnyOrder(3L, 4L);
    }

    @Test
    @DisplayName("스케줄러 실행 - 업데이트할 항목 없음")
    void updateProgressStatus_noUpdatesNeeded() {
        // Given
        when(studyQueryRepository.findApprovedStudiesStartedBefore(any()))
            .thenReturn(List.of());
        when(projectQueryRepository.findApprovedProjectsStartedBefore(any()))
            .thenReturn(List.of());

        // When
        scheduler.updateProgressStatus();

        // Then
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("스케줄러 실행 - Study만 업데이트")
    void updateProgressStatus_onlyStudies() {
        // Given
        List<Long> studyIds = List.of(1L, 2L);

        when(studyQueryRepository.findApprovedStudiesStartedBefore(any()))
            .thenReturn(studyIds);
        when(projectQueryRepository.findApprovedProjectsStartedBefore(any()))
            .thenReturn(List.of());

        // When
        scheduler.updateProgressStatus();

        // Then
        ArgumentCaptor<ProgressStatusUpdateEvent> eventCaptor = 
            ArgumentCaptor.forClass(ProgressStatusUpdateEvent.class);
        
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        
        List<ProgressStatusUpdateEvent> events = eventCaptor.getAllValues();
        assertThat(events).allMatch(event -> "STUDY".equals(event.getEntityType()));
        assertThat(events).extracting(ProgressStatusUpdateEvent::getEntityId)
            .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("스케줄러 실행 - Project만 업데이트")
    void updateProgressStatus_onlyProjects() {
        // Given
        List<Long> projectIds = List.of(3L, 4L);

        when(studyQueryRepository.findApprovedStudiesStartedBefore(any()))
            .thenReturn(List.of());
        when(projectQueryRepository.findApprovedProjectsStartedBefore(any()))
            .thenReturn(projectIds);

        // When
        scheduler.updateProgressStatus();

        // Then
        ArgumentCaptor<ProgressStatusUpdateEvent> eventCaptor = 
            ArgumentCaptor.forClass(ProgressStatusUpdateEvent.class);
        
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        
        List<ProgressStatusUpdateEvent> events = eventCaptor.getAllValues();
        assertThat(events).allMatch(event -> "PROJECT".equals(event.getEntityType()));
        assertThat(events).extracting(ProgressStatusUpdateEvent::getEntityId)
            .containsExactlyInAnyOrder(3L, 4L);
    }
}
