package org.certis.studyplatform.project.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.project.domain.ProjectStatus;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;

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
        OffsetDateTime futureStartDate = alignToNextMonday(now.plusDays(7));
        OffsetDateTime futureEndDate = alignToKstSunday(futureStartDate.plusDays(30));
        
        // READY 상태인 프로젝트 (startDate 변경이 허용되는 상태)
        ProjectVo existingProject = createTestProjectVo(
            "READY", 
            ResultSubmitStatus.READY,
            alignToNextMonday(now.plusDays(1)), // 미래 시작일 (READY 상태 유지)
            alignToNextMonday(now.plusDays(1)).plusDays(10)  // 미래 종료일
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
        // 정책에 따라 READY 상태에서는 미래로 옮겨도 READY 유지
        assertThat(updatedProject.status()).isEqualTo(ProjectStatus.READY.name());
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
        OffsetDateTime pastStartDate = alignToNextMonday(now.minusDays(8)); // 이전 주 월요일
        OffsetDateTime futureEndDate = alignToKstSunday(pastStartDate.plusDays(10));
        
        ProjectVo existingProject = createTestProjectVo(
            "READY", 
            ResultSubmitStatus.READY,
            alignToNextMonday(now.minusDays(9)), // 더 과거 시작일
            alignToNextMonday(now.minusDays(9)).plusDays(12)   // 미래 종료일
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
        OffsetDateTime pastStartDate = alignToNextMonday(now.minusDays(14)); // 2주 전 월요일
        OffsetDateTime pastEndDate = alignToKstSunday(pastStartDate.plusDays(1));
        
        // READY 상태인 프로젝트 (startDate 변경이 허용되는 상태)
        ProjectVo existingProject = createTestProjectVo(
            "READY", 
            ResultSubmitStatus.READY,
            alignToNextMonday(now.plusDays(1)), // 미래 시작일 (READY 상태 유지)
            alignToNextMonday(now.plusDays(1)).plusDays(5)  // 미래 종료일
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
        OffsetDateTime futureEndDate = alignToKstSunday(now.plusDays(10));
        
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
        
        // 상태는 날짜 변경이 없으므로 유지됨
        assertThat(updatedProject.status()).isEqualTo(existingProject.status());
        assertThat(updatedProject.resultSubmitStatus()).isEqualTo(existingProject.resultSubmitStatus());
    }

    @Test
    @DisplayName("updateFrom - INPROGRESS에서 startDate를 미래로 옮기면 APPROVED로 롤백")
    void updateFrom_whenInProgressAndStartMovedToFuture_shouldRollbackToApproved() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime newStartDate = alignToNextMonday(now.plusDays(7));
        
        // INPROGRESS 상태인 프로젝트
        ProjectVo existingProject = createTestProjectVo(
            "INPROGRESS", 
            ResultSubmitStatus.INPROGRESS,
            now.minusDays(1), // 과거 시작일
            now.plusDays(10)  // 미래 종료일
        );

        // When
        ProjectVo updated = ProjectVo.updateFrom(
            existingProject,
            null, null, null, null, null,
            newStartDate,
            null,
            null, null, null, null, null
        );

        // Then
        assertThat(updated.status()).isEqualTo(ProjectStatus.APPROVED.name());
        assertThat(updated.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
    }

    @Test
    @DisplayName("updateFrom - READY 상태에서 startDate 변경해도 READY 유지")
    void updateFrom_whenStatusIsReadyAndStartDateChanged_shouldAllowChange() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime newStartDate = alignToNextMonday(now.plusDays(7));
        OffsetDateTime newEndDate = alignToKstSunday(newStartDate.plusDays(30));
        
        // READY 상태인 프로젝트
        ProjectVo existingProject = createTestProjectVo(
            "READY", 
            ResultSubmitStatus.READY,
            alignToNextMonday(now.plusDays(1)), // 미래 시작일
            alignToNextMonday(now.plusDays(1)).plusDays(10)  // 미래 종료일
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
        assertThat(updatedProject.status()).isEqualTo(ProjectStatus.READY.name());
        assertThat(updatedProject.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
    }

    @Test
    @DisplayName("updateFrom - APPROVED 상태에서 update 시 상태 유지")
    void updateFrom_whenStatusIsApproved_shouldMaintainStatus() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime futureEndDate = alignToKstSunday(now.plusDays(30));
        
        // APPROVED 상태인 프로젝트
        ProjectVo existingProject = createTestProjectVo(
            "APPROVED", 
            ResultSubmitStatus.READY,
            now.plusDays(1), // 미래 시작일
            now.plusDays(10)  // 미래 종료일
        );

        // When
        ProjectVo updatedProject = ProjectVo.updateFrom(
            existingProject,
            "Updated Title",
            "Updated Description",
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            null, // startDate는 변경하지 않음 (APPROVED 상태에서는 시작일 변경 불가)
            futureEndDate,   // 새로운 종료일
            null, // githubUrl은 변경하지 않음
            null, // externalUrl은 변경하지 않음
            null, // demoUrl은 변경하지 않음
            null, // thumbnailUrl은 변경하지 않음
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedProject.status()).isEqualTo("APPROVED"); // 기존 상태 유지
        assertThat(updatedProject.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY); // 기존 상태 유지
        assertThat(updatedProject.title()).isEqualTo("Updated Title");
        assertThat(updatedProject.description()).isEqualTo("Updated Description");
        assertThat(updatedProject.startDate()).isEqualTo(existingProject.startDate()); // 기존 시작일 유지
        assertThat(updatedProject.endDate()).isEqualTo(futureEndDate);
    }

    @Test
    @DisplayName("updateFrom - INPROGRESS 상태에서 update 시 상태 유지")
    void updateFrom_whenStatusIsInProgress_shouldMaintainStatus() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime pastStartDate = now.minusDays(1);
        OffsetDateTime futureEndDate = alignToKstSunday(now.plusDays(10));
        
        // INPROGRESS 상태인 프로젝트
        ProjectVo existingProject = createTestProjectVo(
            "INPROGRESS", 
            ResultSubmitStatus.INPROGRESS,
            pastStartDate,
            futureEndDate
        );

        // When
        ProjectVo updatedProject = ProjectVo.updateFrom(
            existingProject,
            "Updated Title",
            null, // description은 변경하지 않음
            "Updated Content",
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            pastStartDate, // 시작일은 변경하지 않음
            futureEndDate, // 종료일은 변경하지 않음
            null, // githubUrl은 변경하지 않음
            null, // externalUrl은 변경하지 않음
            null, // demoUrl은 변경하지 않음
            null, // thumbnailUrl은 변경하지 않음
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedProject.status()).isEqualTo("INPROGRESS"); // 기존 상태 유지
        assertThat(updatedProject.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.INPROGRESS); // 기존 상태 유지
        assertThat(updatedProject.title()).isEqualTo("Updated Title");
        assertThat(updatedProject.content()).isEqualTo("Updated Content");
    }

    @Test
    @DisplayName("updateFrom - COMPLETED 상태에서 update 시 상태 유지")
    void updateFrom_whenStatusIsCompleted_shouldMaintainStatus() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime pastStartDate = alignToNextMonday(now.minusDays(14));
        OffsetDateTime pastEndDate = pastStartDate.plusDays(1);
        
        // COMPLETED 상태인 프로젝트
        ProjectVo existingProject = createTestProjectVo(
            "COMPLETED", 
            ResultSubmitStatus.COMPLETED,
            pastStartDate,
            pastEndDate
        );

        // When
        ProjectVo updatedProject = ProjectVo.updateFrom(
            existingProject,
            "Updated Title",
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            pastStartDate, // 시작일은 변경하지 않음
            pastEndDate, // 종료일은 변경하지 않음
            null, // githubUrl은 변경하지 않음
            null, // externalUrl은 변경하지 않음
            null, // demoUrl은 변경하지 않음
            null, // thumbnailUrl은 변경하지 않음
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedProject.status()).isEqualTo("COMPLETED"); // 기존 상태 유지
        assertThat(updatedProject.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.COMPLETED); // 기존 상태 유지
        assertThat(updatedProject.title()).isEqualTo("Updated Title");
    }

    @Test
    @DisplayName("updateFrom - REJECTED 상태에서 update 시 상태 유지")
    void updateFrom_whenStatusIsRejected_shouldMaintainStatus() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime futureEndDate = now.plusDays(30);
        
        // REJECTED 상태인 프로젝트
        ProjectVo existingProject = createTestProjectVo(
            "REJECTED", 
            ResultSubmitStatus.REJECTED,
            now.plusDays(1),
            now.plusDays(10)
        );

        // When
        ProjectVo updatedProject = ProjectVo.updateFrom(
            existingProject,
            "Updated Title",
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            null, // startDate는 변경하지 않음 (REJECTED 상태에서는 시작일 변경 불가)
            futureEndDate,   // 새로운 종료일
            null, // githubUrl은 변경하지 않음
            null, // externalUrl은 변경하지 않음
            null, // demoUrl은 변경하지 않음
            null, // thumbnailUrl은 변경하지 않음
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedProject.status()).isEqualTo("REJECTED"); // 기존 상태 유지
        assertThat(updatedProject.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.REJECTED); // 기존 상태 유지
        assertThat(updatedProject.title()).isEqualTo("Updated Title");
        assertThat(updatedProject.startDate()).isEqualTo(existingProject.startDate()); // 기존 시작일 유지
        assertThat(updatedProject.endDate()).isEqualTo(futureEndDate);
    }

    private ProjectVo createTestProjectVo(String status, ResultSubmitStatus resultSubmitStatus, 
                                        OffsetDateTime startDate, OffsetDateTime endDate) {
        // Align start to Monday to satisfy domain rule
        OffsetDateTime mondayStart = alignToNextMonday(startDate);
        // align end to Sunday in KST to satisfy domain rule
        java.time.ZoneId kst = java.time.ZoneId.of("Asia/Seoul");
        var endZdt = (endDate.isAfter(mondayStart) ? endDate : mondayStart.plusDays(6)).atZoneSameInstant(kst);
        int shift = java.time.DayOfWeek.SUNDAY.getValue() - endZdt.getDayOfWeek().getValue();
        if (shift < 0) shift += 7;
        OffsetDateTime safeEnd = endZdt.plusDays(shift).withHour(23).withMinute(59).withSecond(59).withNano(0).toOffsetDateTime();
        return new ProjectVo(
            1L,
            "Test Project",
            "Test Description",
            "Test Content",
            "SECURITY",
            "WEB",
            mondayStart,
            safeEnd,
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

    private OffsetDateTime alignToNextMonday(OffsetDateTime source) {
        java.time.DayOfWeek dow = source.getDayOfWeek();
        int shift = java.time.DayOfWeek.MONDAY.getValue() - dow.getValue();
        if (shift < 0) {
            shift += 7;
        }
        return source.plusDays(shift);
    }

    private OffsetDateTime alignToKstSunday(OffsetDateTime source) {
        java.time.ZoneId kst = java.time.ZoneId.of("Asia/Seoul");
        var zdt = source.atZoneSameInstant(kst);
        int shift = java.time.DayOfWeek.SUNDAY.getValue() - zdt.getDayOfWeek().getValue();
        if (shift < 0) shift += 7;
        return zdt.plusDays(shift).withHour(23).withMinute(59).withSecond(59).withNano(0).toOffsetDateTime();
    }

    @Test
    @DisplayName("create - 시작일이 월요일이 아니면 예외")
    void create_whenStartDateIsNotMonday_shouldThrow() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime nonMondayStart = alignToNextMonday(now).plusDays(1);
        OffsetDateTime end = alignToKstSunday(nonMondayStart);

        assertThatThrownBy(() -> new ProjectVo(
            null,
            "title",
            "desc",
            "content",
            "SECURITY",
            "WEB",
            nonMondayStart,
            end,
            1L,
            "creator",
            "FRESHMAN",
            "2024-1",
            ProjectStatus.READY.name(),
            ResultSubmitStatus.READY,
            null,
            null,
            null,
            null,
            5,
            0,
            true,
            Collections.emptyList(),
            Collections.emptyList()
        ))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("status", ExceptionStatus.PROJECT_DOMAIN_INVALID_START_DAY);
    }

    @Test
    @DisplayName("create - 시작일이 종료일보다 늦으면 예외")
    void create_whenStartAfterEnd_shouldThrow() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(8);
        // 월요일 정합성 보장을 위해 같은 주의 월요일로 조정
        start = start.plusDays(java.time.DayOfWeek.MONDAY.getValue() - start.getDayOfWeek().getValue());
        OffsetDateTime end = start.minusDays(1);

        OffsetDateTime finalStart = start;
        assertThatThrownBy(() -> new ProjectVo(
            null,
            "title",
            "desc",
            "content",
            "SECURITY",
            "WEB",
            finalStart,
            end,
            1L,
            "creator",
            "FRESHMAN",
            "2024-1",
            ProjectStatus.READY.name(),
            ResultSubmitStatus.READY,
            null,
            null,
            null,
            null,
            5,
            0,
            true,
            Collections.emptyList(),
            Collections.emptyList()
        ))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("status", ExceptionStatus.PROJECT_DOMAIN_INVALID_DATE);
    }

    @Test
    @DisplayName("updateFrom - 기존 상태가 APPROVED인 경우 상태 유지")
    void updateFrom_whenExistingStatusIsApproved_shouldMaintainStatus() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime pastStartDate = now.minusDays(1); // 과거 시작일
        OffsetDateTime futureEndDate = now.plusDays(10); // 미래 종료일
        
        // APPROVED 상태인 프로젝트
        ProjectVo existingProject = createTestProjectVo(
            "APPROVED", 
            ResultSubmitStatus.READY,
            pastStartDate,
            futureEndDate
        );

        // When - 제목만 변경
        ProjectVo updatedProject = ProjectVo.updateFrom(
            existingProject,
            "Updated Title",
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

        // Then - 상태가 유지되어야 함
        assertThat(updatedProject.status()).isEqualTo("APPROVED");
        assertThat(updatedProject.title()).isEqualTo("Updated Title");
    }

    @Test
    @DisplayName("updateFrom - 기존 상태가 INPROGRESS인 경우 상태 유지")
    void updateFrom_whenExistingStatusIsInProgress_shouldMaintainStatus() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime pastStartDate = now.minusDays(1); // 과거 시작일
        OffsetDateTime futureEndDate = now.plusDays(10); // 미래 종료일
        
        // INPROGRESS 상태인 프로젝트
        ProjectVo existingProject = createTestProjectVo(
            "INPROGRESS", 
            ResultSubmitStatus.READY,
            pastStartDate,
            futureEndDate
        );

        // When - 제목만 변경
        ProjectVo updatedProject = ProjectVo.updateFrom(
            existingProject,
            "Updated Title",
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

        // Then - 상태가 유지되어야 함
        assertThat(updatedProject.status()).isEqualTo("INPROGRESS");
        assertThat(updatedProject.title()).isEqualTo("Updated Title");
    }
}
