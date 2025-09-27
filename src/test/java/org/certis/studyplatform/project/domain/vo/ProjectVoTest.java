package org.certis.studyplatform.project.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.project.domain.ProjectStatus;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ProjectVo 테스트
 */
class ProjectVoTest {

    @Test
    @DisplayName("updateFrom - startedAt이 현재 시각보다 나중인 경우 APPROVED로 초기화")
    void updateFrom_whenStartDateIsAfterCurrentTime_shouldSetStatusToApproved() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime futureStartDate = now.plusDays(7); // 7일 후
        OffsetDateTime futureEndDate = now.plusDays(30); // 30일 후
        
        // READY 상태인 프로젝트 (startDate 변경이 허용되는 상태)
        ProjectVo existingProject = createTestProjectVo(
            "READY", 
            ResultSubmitStatus.READY,
            now.plusDays(1), // 미래 시작일 (READY 상태 유지)
            now.plusDays(10)  // 미래 종료일
        );

        // When
        ProjectVo updatedProject = ProjectVo.updateFrom(
            existingProject,
            "Updated Title",
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            futureStartDate, // 새로운 시작일 (미래)
            futureEndDate,   // 새로운 종료일
            null, // githubUrl은 변경하지 않음
            null, // externalUrl은 변경하지 않음
            null, // demoUrl은 변경하지 않음
            null, // thumbnailUrl은 변경하지 않음
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedProject.status()).isEqualTo(ProjectStatus.APPROVED.name());
        assertThat(updatedProject.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
        assertThat(updatedProject.title()).isEqualTo("Updated Title");
        assertThat(updatedProject.startDate()).isEqualTo(futureStartDate);
        assertThat(updatedProject.endDate()).isEqualTo(futureEndDate);
    }

    @Test
    @DisplayName("updateFrom - startedAt이 현재 시각보다 이전인 경우 기존 로직 적용")
    void updateFrom_whenStartDateIsBeforeCurrentTime_shouldApplyExistingLogic() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime pastStartDate = now.minusDays(1); // 1일 전
        OffsetDateTime futureEndDate = now.plusDays(10); // 10일 후
        
        ProjectVo existingProject = createTestProjectVo(
            "READY", 
            ResultSubmitStatus.READY,
            now.minusDays(2), // 과거 시작일
            now.plusDays(5)   // 미래 종료일
        );

        // When
        ProjectVo updatedProject = ProjectVo.updateFrom(
            existingProject,
            "Updated Title",
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            pastStartDate,    // 새로운 시작일 (과거)
            futureEndDate,   // 새로운 종료일
            null, // githubUrl은 변경하지 않음
            null, // externalUrl은 변경하지 않음
            null, // demoUrl은 변경하지 않음
            null, // thumbnailUrl은 변경하지 않음
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedProject.status()).isEqualTo(ProjectStatus.INPROGRESS.name());
        assertThat(updatedProject.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
        assertThat(updatedProject.title()).isEqualTo("Updated Title");
        assertThat(updatedProject.startDate()).isEqualTo(pastStartDate);
        assertThat(updatedProject.endDate()).isEqualTo(futureEndDate);
    }

    @Test
    @DisplayName("updateFrom - endedAt이 현재 시각보다 이전인 경우 COMPLETED로 설정")
    void updateFrom_whenEndDateIsBeforeCurrentTime_shouldSetStatusToCompleted() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime pastStartDate = now.minusDays(10); // 10일 전
        OffsetDateTime pastEndDate = now.minusDays(1);    // 1일 전
        
        // READY 상태인 프로젝트 (startDate 변경이 허용되는 상태)
        ProjectVo existingProject = createTestProjectVo(
            "READY", 
            ResultSubmitStatus.READY,
            now.plusDays(1), // 미래 시작일 (READY 상태 유지)
            now.plusDays(5)  // 미래 종료일
        );

        // When
        ProjectVo updatedProject = ProjectVo.updateFrom(
            existingProject,
            "Updated Title",
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            pastStartDate,    // 새로운 시작일 (과거)
            pastEndDate,     // 새로운 종료일 (과거)
            null, // githubUrl은 변경하지 않음
            null, // externalUrl은 변경하지 않음
            null, // demoUrl은 변경하지 않음
            null, // thumbnailUrl은 변경하지 않음
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedProject.status()).isEqualTo(ProjectStatus.COMPLETED.name());
        assertThat(updatedProject.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
        assertThat(updatedProject.title()).isEqualTo("Updated Title");
        assertThat(updatedProject.startDate()).isEqualTo(pastStartDate);
        assertThat(updatedProject.endDate()).isEqualTo(pastEndDate);
    }

    @Test
    @DisplayName("updateFrom - null 값들은 기존 값 유지")
    void updateFrom_whenNullValues_shouldKeepExistingValues() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime pastStartDate = now.minusDays(1);
        OffsetDateTime futureEndDate = now.plusDays(10);
        
        ProjectVo existingProject = createTestProjectVo(
            "INPROGRESS", 
            ResultSubmitStatus.INPROGRESS,
            pastStartDate,
            futureEndDate
        );

        // When
        ProjectVo updatedProject = ProjectVo.updateFrom(
            existingProject,
            null, // title은 변경하지 않음
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            null, // startDate는 변경하지 않음
            null, // endDate는 변경하지 않음
            null, // githubUrl은 변경하지 않음
            null, // externalUrl은 변경하지 않음
            null, // demoUrl은 변경하지 않음
            null, // thumbnailUrl은 변경하지 않음
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedProject.title()).isEqualTo(existingProject.title());
        assertThat(updatedProject.description()).isEqualTo(existingProject.description());
        assertThat(updatedProject.content()).isEqualTo(existingProject.content());
        assertThat(updatedProject.category()).isEqualTo(existingProject.category());
        assertThat(updatedProject.subCategory()).isEqualTo(existingProject.subCategory());
        assertThat(updatedProject.startDate()).isEqualTo(existingProject.startDate());
        assertThat(updatedProject.endDate()).isEqualTo(existingProject.endDate());
        assertThat(updatedProject.maxParticipants()).isEqualTo(existingProject.maxParticipants());
        
        // Status는 기존 로직에 따라 재계산됨
        assertThat(updatedProject.status()).isEqualTo(ProjectStatus.INPROGRESS.name());
        assertThat(updatedProject.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
    }

    @Test
    @DisplayName("updateFrom - READY 상태가 아닐 때 startDate 변경 시 예외 발생")
    void updateFrom_whenStatusIsNotReadyAndStartDateChanged_shouldThrowException() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime newStartDate = now.plusDays(7);
        
        // INPROGRESS 상태인 프로젝트
        ProjectVo existingProject = createTestProjectVo(
            "INPROGRESS", 
            ResultSubmitStatus.INPROGRESS,
            now.minusDays(1), // 과거 시작일
            now.plusDays(10)  // 미래 종료일
        );

        // When & Then
        assertThatThrownBy(() -> ProjectVo.updateFrom(
            existingProject,
            null, // title은 변경하지 않음
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            newStartDate, // 새로운 시작일 (변경 시도)
            null, // endDate는 변경하지 않음
            null, // githubUrl은 변경하지 않음
            null, // externalUrl은 변경하지 않음
            null, // demoUrl은 변경하지 않음
            null, // thumbnailUrl은 변경하지 않음
            null  // maxParticipants는 변경하지 않음
        ))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("status", ExceptionStatus.PROJECT_DOMAIN_INVALID_STATUS)
        .hasMessageContaining("프로젝트가 READY 상태가 아닐 때는 시작일을 변경할 수 없습니다");
    }

    @Test
    @DisplayName("updateFrom - READY 상태일 때 startDate 변경은 허용")
    void updateFrom_whenStatusIsReadyAndStartDateChanged_shouldAllowChange() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime newStartDate = now.plusDays(7);
        OffsetDateTime newEndDate = now.plusDays(30);
        
        // READY 상태인 프로젝트
        ProjectVo existingProject = createTestProjectVo(
            "READY", 
            ResultSubmitStatus.READY,
            now.plusDays(1), // 미래 시작일
            now.plusDays(10)  // 미래 종료일
        );

        // When
        ProjectVo updatedProject = ProjectVo.updateFrom(
            existingProject,
            null, // title은 변경하지 않음
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            newStartDate, // 새로운 시작일 (변경)
            newEndDate,   // 새로운 종료일 (변경)
            null, // githubUrl은 변경하지 않음
            null, // externalUrl은 변경하지 않음
            null, // demoUrl은 변경하지 않음
            null, // thumbnailUrl은 변경하지 않음
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedProject.startDate()).isEqualTo(newStartDate);
        assertThat(updatedProject.endDate()).isEqualTo(newEndDate);
        assertThat(updatedProject.status()).isEqualTo(ProjectStatus.APPROVED.name());
        assertThat(updatedProject.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
    }

    private ProjectVo createTestProjectVo(String status, ResultSubmitStatus resultSubmitStatus, 
                                        OffsetDateTime startDate, OffsetDateTime endDate) {
        return new ProjectVo(
            1L,
            "Test Project",
            "Test Description",
            "Test Content",
            "SECURITY",
            "WEB",
            startDate,
            endDate,
            1L,
            "Test Creator",
            "FRESHMAN",
            "2024-1",
            status,
            resultSubmitStatus,
            "https://github.com/test",
            null, // externalUrl
            "https://demo.test.com",
            "https://thumbnail.test.com",
            10,
            5,
            true,
            Collections.emptyList(),
            Collections.emptyList()
        );
    }
}
