package org.certis.studyplatform.shared.domain.service;

import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.certis.studyplatform.study.domain.StudyStatus;
import org.certis.studyplatform.project.domain.ProjectStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProgressStatusService 테스트
 */
class ProgressStatusServiceTest {

    private ProgressStatusService progressStatusService;

    @BeforeEach
    void setUp() {
        progressStatusService = new ProgressStatusService();
    }

    @Test
    @DisplayName("Study: APPROVED 상태에서 시작 시간 이후로 INPROGRESS로 변경")
    void study_approvedToInProgress_whenStartedAtPassed() {
        // Given
        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now().plusDays(1);
        OffsetDateTime currentTime = OffsetDateTime.now();

        // When
        var result = progressStatusService.calculateStudyStatus(
            StudyStatus.APPROVED,
            ResultSubmitStatus.READY,
            startedAt,
            endedAt,
            currentTime
        );

        // Then
        assertThat(result.status()).isEqualTo(StudyStatus.INPROGRESS);
        assertThat(result.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
    }

    @Test
    @DisplayName("Study: INPROGRESS 상태에서 종료 시간 이후로 COMPLETED로 변경")
    void study_inProgressToCompleted_whenEndedAtPassed() {
        // Given
        OffsetDateTime startedAt = OffsetDateTime.now().minusDays(1);
        OffsetDateTime endedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime currentTime = OffsetDateTime.now();

        // When
        var result = progressStatusService.calculateStudyStatus(
            StudyStatus.INPROGRESS,
            ResultSubmitStatus.READY,
            startedAt,
            endedAt,
            currentTime
        );

        // Then
        assertThat(result.status()).isEqualTo(StudyStatus.COMPLETED);
        assertThat(result.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
    }

    @Test
    @DisplayName("Study: 시작 시간 이전이면 상태 변경 없음")
    void study_noChange_whenStartedAtNotPassed() {
        // Given
        OffsetDateTime startedAt = OffsetDateTime.now().plusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now().plusDays(1);
        OffsetDateTime currentTime = OffsetDateTime.now();

        // When
        var result = progressStatusService.calculateStudyStatus(
            StudyStatus.APPROVED,
            ResultSubmitStatus.READY,
            startedAt,
            endedAt,
            currentTime
        );

        // Then
        assertThat(result.status()).isEqualTo(StudyStatus.APPROVED);
        assertThat(result.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
    }

    @Test
    @DisplayName("Project: APPROVED 상태에서 시작 시간 이후로 INPROGRESS로 변경")
    void project_approvedToInProgress_whenStartedAtPassed() {
        // Given
        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now().plusDays(1);
        OffsetDateTime currentTime = OffsetDateTime.now();

        // When
        var result = progressStatusService.calculateProjectStatus(
            ProjectStatus.APPROVED,
            ResultSubmitStatus.READY,
            startedAt,
            endedAt,
            currentTime
        );

        // Then
        assertThat(result.status()).isEqualTo(ProjectStatus.INPROGRESS);
        assertThat(result.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
    }

    @Test
    @DisplayName("Project: INPROGRESS 상태에서 종료 시간 이후로 COMPLETED로 변경")
    void project_inProgressToCompleted_whenEndedAtPassed() {
        // Given
        OffsetDateTime startedAt = OffsetDateTime.now().minusDays(1);
        OffsetDateTime endedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime currentTime = OffsetDateTime.now();

        // When
        var result = progressStatusService.calculateProjectStatus(
            ProjectStatus.INPROGRESS,
            ResultSubmitStatus.READY,
            startedAt,
            endedAt,
            currentTime
        );

        // Then
        assertThat(result.status()).isEqualTo(ProjectStatus.COMPLETED);
        assertThat(result.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
    }

    @Test
    @DisplayName("Project: 시작 시간 이전이면 상태 변경 없음")
    void project_noChange_whenStartedAtNotPassed() {
        // Given
        OffsetDateTime startedAt = OffsetDateTime.now().plusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now().plusDays(1);
        OffsetDateTime currentTime = OffsetDateTime.now();

        // When
        var result = progressStatusService.calculateProjectStatus(
            ProjectStatus.APPROVED,
            ResultSubmitStatus.READY,
            startedAt,
            endedAt,
            currentTime
        );

        // Then
        assertThat(result.status()).isEqualTo(ProjectStatus.APPROVED);
        assertThat(result.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
    }
}
