package org.certis.studyplatform.study.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.study.domain.StudyStatus;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * StudyVo 테스트
 */
class StudyVoTest {

    @Test
    @DisplayName("updateFrom - startedAt이 현재 시각보다 나중인 경우 APPROVED로 초기화")
    void updateFrom_whenStartDateIsAfterCurrentTime_shouldSetStatusToApproved() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime futureStartDate = alignToNextMonday(now.plusDays(7));
        OffsetDateTime futureEndDate = futureStartDate.plusDays(30);
        
        // READY 상태인 스터디 (startDate 변경이 허용되는 상태)
        StudyVo existingStudy = createTestStudyVo(
            "READY", 
            ResultSubmitStatus.READY,
            alignToNextMonday(now.plusDays(1)), // 미래 시작일 (READY 상태 유지)
            alignToNextMonday(now.plusDays(1)).plusDays(10)  // 미래 종료일
        );

        // When
        StudyVo updatedStudy = StudyVo.updateFrom(
            existingStudy,
            "Updated Title",
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            futureStartDate, // 새로운 시작일 (미래)
            futureEndDate,   // 새로운 종료일
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedStudy.status()).isEqualTo(StudyStatus.APPROVED.name());
        assertThat(updatedStudy.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
        assertThat(updatedStudy.title()).isEqualTo("Updated Title");
        assertThat(updatedStudy.startDate()).isEqualTo(futureStartDate);
        assertThat(updatedStudy.endDate()).isEqualTo(futureEndDate);
    }

    @Test
    @DisplayName("updateFrom - startedAt이 현재 시각보다 이전인 경우 기존 로직 적용")
    void updateFrom_whenStartDateIsBeforeCurrentTime_shouldApplyExistingLogic() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime pastStartDate = alignToNextMonday(now.minusDays(8)); // 이전 주 월요일
        OffsetDateTime futureEndDate = pastStartDate.plusDays(10);
        
        StudyVo existingStudy = createTestStudyVo(
            "READY", 
            ResultSubmitStatus.READY,
            alignToNextMonday(now.minusDays(9)), // 더 과거 시작일
            alignToNextMonday(now.minusDays(9)).plusDays(12)   // 미래 종료일
        );

        // When
        StudyVo updatedStudy = StudyVo.updateFrom(
            existingStudy,
            "Updated Title",
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            pastStartDate,    // 새로운 시작일 (과거)
            futureEndDate,   // 새로운 종료일
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedStudy.status()).isEqualTo(StudyStatus.INPROGRESS.name());
        assertThat(updatedStudy.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
        assertThat(updatedStudy.title()).isEqualTo("Updated Title");
        assertThat(updatedStudy.startDate()).isEqualTo(pastStartDate);
        assertThat(updatedStudy.endDate()).isEqualTo(futureEndDate);
    }

    @Test
    @DisplayName("updateFrom - endedAt이 현재 시각보다 이전인 경우 COMPLETED로 설정")
    void updateFrom_whenEndDateIsBeforeCurrentTime_shouldSetStatusToCompleted() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime pastStartDate = alignToNextMonday(now.minusDays(14)); // 2주 전 월요일
        OffsetDateTime pastEndDate = pastStartDate.plusDays(1);
        
        // READY 상태인 스터디 (startDate 변경이 허용되는 상태)
        StudyVo existingStudy = createTestStudyVo(
            "READY", 
            ResultSubmitStatus.READY,
            alignToNextMonday(now.plusDays(1)), // 미래 시작일 (READY 상태 유지)
            alignToNextMonday(now.plusDays(1)).plusDays(5)  // 미래 종료일
        );

        // When
        StudyVo updatedStudy = StudyVo.updateFrom(
            existingStudy,
            "Updated Title",
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            pastStartDate,    // 새로운 시작일 (과거)
            pastEndDate,     // 새로운 종료일 (과거)
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedStudy.status()).isEqualTo(StudyStatus.COMPLETED.name());
        assertThat(updatedStudy.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
        assertThat(updatedStudy.title()).isEqualTo("Updated Title");
        assertThat(updatedStudy.startDate()).isEqualTo(pastStartDate);
        assertThat(updatedStudy.endDate()).isEqualTo(pastEndDate);
    }

    @Test
    @DisplayName("updateFrom - null 값들은 기존 값 유지")
    void updateFrom_whenNullValues_shouldKeepExistingValues() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime pastStartDate = now.minusDays(1);
        OffsetDateTime futureEndDate = now.plusDays(10);
        
        StudyVo existingStudy = createTestStudyVo(
            "INPROGRESS", 
            ResultSubmitStatus.INPROGRESS,
            pastStartDate,
            futureEndDate
        );

        // When
        StudyVo updatedStudy = StudyVo.updateFrom(
            existingStudy,
            null, // title은 변경하지 않음
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            null, // startDate는 변경하지 않음
            null, // endDate는 변경하지 않음
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedStudy.title()).isEqualTo(existingStudy.title());
        assertThat(updatedStudy.description()).isEqualTo(existingStudy.description());
        assertThat(updatedStudy.content()).isEqualTo(existingStudy.content());
        assertThat(updatedStudy.category()).isEqualTo(existingStudy.category());
        assertThat(updatedStudy.subCategory()).isEqualTo(existingStudy.subCategory());
        assertThat(updatedStudy.startDate()).isEqualTo(existingStudy.startDate());
        assertThat(updatedStudy.endDate()).isEqualTo(existingStudy.endDate());
        assertThat(updatedStudy.maxParticipants()).isEqualTo(existingStudy.maxParticipants());
        
        // Status는 기존 로직에 따라 재계산됨
        assertThat(updatedStudy.status()).isEqualTo(StudyStatus.INPROGRESS.name());
        assertThat(updatedStudy.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.INPROGRESS);
    }

    @Test
    @DisplayName("updateFrom - READY 상태가 아닐 때 startDate 변경 시 예외 발생")
    void updateFrom_whenStatusIsNotReadyAndStartDateChanged_shouldThrowException() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime newStartDate = alignToNextMonday(now.plusDays(7));
        
        // INPROGRESS 상태인 스터디
        StudyVo existingStudy = createTestStudyVo(
            "INPROGRESS", 
            ResultSubmitStatus.INPROGRESS,
            now.minusDays(1), // 과거 시작일
            now.plusDays(10)  // 미래 종료일
        );

        // When & Then
        assertThatThrownBy(() -> StudyVo.updateFrom(
            existingStudy,
            null, // title은 변경하지 않음
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            newStartDate, // 새로운 시작일 (변경 시도)
            null, // endDate는 변경하지 않음
            null  // maxParticipants는 변경하지 않음
        ))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("status", ExceptionStatus.STUDY_DOMAIN_INVALID_STATUS)
        .hasMessageContaining("스터디가 READY 상태가 아닐 때는 시작일을 변경할 수 없습니다");
    }

    @Test
    @DisplayName("updateFrom - READY 상태일 때 startDate 변경은 허용")
    void updateFrom_whenStatusIsReadyAndStartDateChanged_shouldAllowChange() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime newStartDate = alignToNextMonday(now.plusDays(7));
        OffsetDateTime newEndDate = newStartDate.plusDays(30);
        
        // READY 상태인 스터디
        StudyVo existingStudy = createTestStudyVo(
            "READY", 
            ResultSubmitStatus.READY,
            alignToNextMonday(now.plusDays(1)), // 미래 시작일
            alignToNextMonday(now.plusDays(1)).plusDays(10)  // 미래 종료일
        );

        // When
        StudyVo updatedStudy = StudyVo.updateFrom(
            existingStudy,
            null, // title은 변경하지 않음
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            newStartDate, // 새로운 시작일 (변경)
            newEndDate,   // 새로운 종료일 (변경)
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedStudy.startDate()).isEqualTo(newStartDate);
        assertThat(updatedStudy.endDate()).isEqualTo(newEndDate);
        assertThat(updatedStudy.status()).isEqualTo(StudyStatus.APPROVED.name());
        assertThat(updatedStudy.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY);
    }

    @Test
    @DisplayName("updateFrom - APPROVED 상태에서 update 시 상태 유지")
    void updateFrom_whenStatusIsApproved_shouldMaintainStatus() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime futureEndDate = now.plusDays(30);
        
        // APPROVED 상태인 스터디
        StudyVo existingStudy = createTestStudyVo(
            "APPROVED", 
            ResultSubmitStatus.READY,
            now.plusDays(1), // 미래 시작일
            now.plusDays(10)  // 미래 종료일
        );

        // When
        StudyVo updatedStudy = StudyVo.updateFrom(
            existingStudy,
            "Updated Title",
            "Updated Description",
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            null, // startDate는 변경하지 않음 (APPROVED 상태에서는 시작일 변경 불가)
            futureEndDate,   // 새로운 종료일
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedStudy.status()).isEqualTo("APPROVED"); // 기존 상태 유지
        assertThat(updatedStudy.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.READY); // 기존 상태 유지
        assertThat(updatedStudy.title()).isEqualTo("Updated Title");
        assertThat(updatedStudy.description()).isEqualTo("Updated Description");
        assertThat(updatedStudy.startDate()).isEqualTo(existingStudy.startDate()); // 기존 시작일 유지
        assertThat(updatedStudy.endDate()).isEqualTo(futureEndDate);
    }

    @Test
    @DisplayName("updateFrom - INPROGRESS 상태에서 update 시 상태 유지")
    void updateFrom_whenStatusIsInProgress_shouldMaintainStatus() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime pastStartDate = now.minusDays(1);
        OffsetDateTime futureEndDate = now.plusDays(10);
        
        // INPROGRESS 상태인 스터디
        StudyVo existingStudy = createTestStudyVo(
            "INPROGRESS", 
            ResultSubmitStatus.INPROGRESS,
            pastStartDate,
            futureEndDate
        );

        // When
        StudyVo updatedStudy = StudyVo.updateFrom(
            existingStudy,
            "Updated Title",
            null, // description은 변경하지 않음
            "Updated Content",
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            pastStartDate, // 시작일은 변경하지 않음
            futureEndDate, // 종료일은 변경하지 않음
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedStudy.status()).isEqualTo("INPROGRESS"); // 기존 상태 유지
        assertThat(updatedStudy.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.INPROGRESS); // 기존 상태 유지
        assertThat(updatedStudy.title()).isEqualTo("Updated Title");
        assertThat(updatedStudy.content()).isEqualTo("Updated Content");
    }

    @Test
    @DisplayName("updateFrom - COMPLETED 상태에서 update 시 상태 유지")
    void updateFrom_whenStatusIsCompleted_shouldMaintainStatus() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime pastStartDate = alignToNextMonday(now.minusDays(14));
        OffsetDateTime pastEndDate = pastStartDate.plusDays(1);
        
        // COMPLETED 상태인 스터디
        StudyVo existingStudy = createTestStudyVo(
            "COMPLETED", 
            ResultSubmitStatus.COMPLETED,
            pastStartDate,
            pastEndDate
        );

        // When
        StudyVo updatedStudy = StudyVo.updateFrom(
            existingStudy,
            "Updated Title",
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            pastStartDate, // 시작일은 변경하지 않음
            pastEndDate, // 종료일은 변경하지 않음
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedStudy.status()).isEqualTo("COMPLETED"); // 기존 상태 유지
        assertThat(updatedStudy.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.COMPLETED); // 기존 상태 유지
        assertThat(updatedStudy.title()).isEqualTo("Updated Title");
    }

    @Test
    @DisplayName("updateFrom - REJECTED 상태에서 update 시 상태 유지")
    void updateFrom_whenStatusIsRejected_shouldMaintainStatus() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime futureEndDate = now.plusDays(30);
        
        // REJECTED 상태인 스터디
        StudyVo existingStudy = createTestStudyVo(
            "REJECTED", 
            ResultSubmitStatus.REJECTED,
            now.plusDays(1),
            now.plusDays(10)
        );

        // When
        StudyVo updatedStudy = StudyVo.updateFrom(
            existingStudy,
            "Updated Title",
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            null, // startDate는 변경하지 않음 (REJECTED 상태에서는 시작일 변경 불가)
            futureEndDate,   // 새로운 종료일
            null  // maxParticipants는 변경하지 않음
        );

        // Then
        assertThat(updatedStudy.status()).isEqualTo("REJECTED"); // 기존 상태 유지
        assertThat(updatedStudy.resultSubmitStatus()).isEqualTo(ResultSubmitStatus.REJECTED); // 기존 상태 유지
        assertThat(updatedStudy.title()).isEqualTo("Updated Title");
        assertThat(updatedStudy.startDate()).isEqualTo(existingStudy.startDate()); // 기존 시작일 유지
        assertThat(updatedStudy.endDate()).isEqualTo(futureEndDate);
    }

    private StudyVo createTestStudyVo(String status, ResultSubmitStatus resultSubmitStatus, 
                                    OffsetDateTime startDate, OffsetDateTime endDate) {
        // Align start to Monday to satisfy domain rule
        OffsetDateTime mondayStart = alignToNextMonday(startDate);
        OffsetDateTime safeEnd = endDate.isAfter(mondayStart) ? endDate : mondayStart.plusDays(7);
        return new StudyVo(
            1L,
            "Test Study",
            "Test Description",
            "Test Content",
            "SECURITY",
            "WEB",
            mondayStart,
            safeEnd,
            OffsetDateTime.now().minusDays(30),
            OffsetDateTime.now().minusDays(1),
            1L,
            "Test Creator",
            MemberGrade.FRESHMAN,
            null, // creatorProfileImageUrl
            "2024-1",
            status,
            resultSubmitStatus,
            10,
            5,
            true,
            Collections.emptyList(),
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

    @Test
    @DisplayName("create - 시작일이 월요일이 아니면 예외")
    void create_whenStartDateIsNotMonday_shouldThrow() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime nonMondayStart = now.plusDays(1);
        OffsetDateTime end = nonMondayStart.plusDays(7);

        assertThatThrownBy(() -> new StudyVo(
            null,
            "title",
            "desc",
            "content",
            "SECURITY",
            "WEB",
            nonMondayStart,
            end,
            now.minusDays(1),
            now,
            1L,
            "creator",
            MemberGrade.FRESHMAN,
            null, // creatorProfileImageUrl
            "2024-1",
            StudyStatus.READY.name(),
            ResultSubmitStatus.READY,
            5,
            0,
            true,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        ))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("status", ExceptionStatus.STUDY_DOMAIN_INVALID_START_DAY);
    }

    @Test
    @DisplayName("create - 시작일이 종료일보다 늦으면 예외")
    void create_whenStartAfterEnd_shouldThrow() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(8);
        start = start.plusDays(java.time.DayOfWeek.MONDAY.getValue() - start.getDayOfWeek().getValue());
        OffsetDateTime end = start.minusDays(1);

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime finalStart = start;
        assertThatThrownBy(() -> new StudyVo(
            null,
            "title",
            "desc",
            "content",
            "SECURITY",
            "WEB",
            finalStart,
            end,
            now.minusDays(1),
            now,
            1L,
            "creator",
            MemberGrade.FRESHMAN,
            null, // creatorProfileImageUrl
            "2024-1",
            StudyStatus.READY.name(),
            ResultSubmitStatus.READY,
            5,
            0,
            true,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        ))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("status", ExceptionStatus.STUDY_DOMAIN_DATE_INVALID);
    }

    @Test
    @DisplayName("updateFrom - 기존 상태가 APPROVED인 경우 상태 유지")
    void updateFrom_whenExistingStatusIsApproved_shouldMaintainStatus() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime pastStartDate = now.minusDays(1); // 과거 시작일
        OffsetDateTime futureEndDate = now.plusDays(10); // 미래 종료일
        
        // APPROVED 상태인 스터디
        StudyVo existingStudy = createTestStudyVo(
            "APPROVED", 
            ResultSubmitStatus.READY,
            pastStartDate,
            futureEndDate
        );

        // When - 제목만 변경
        StudyVo updatedStudy = StudyVo.updateFrom(
            existingStudy,
            "Updated Title",
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            null, // startDate는 변경하지 않음
            null, // endDate는 변경하지 않음
            null  // maxParticipants는 변경하지 않음
        );

        // Then - 상태가 유지되어야 함
        assertThat(updatedStudy.status()).isEqualTo("APPROVED");
        assertThat(updatedStudy.title()).isEqualTo("Updated Title");
    }

    @Test
    @DisplayName("updateFrom - 기존 상태가 INPROGRESS인 경우 상태 유지")
    void updateFrom_whenExistingStatusIsInProgress_shouldMaintainStatus() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime pastStartDate = now.minusDays(1); // 과거 시작일
        OffsetDateTime futureEndDate = now.plusDays(10); // 미래 종료일
        
        // INPROGRESS 상태인 스터디
        StudyVo existingStudy = createTestStudyVo(
            "INPROGRESS", 
            ResultSubmitStatus.READY,
            pastStartDate,
            futureEndDate
        );

        // When - 제목만 변경
        StudyVo updatedStudy = StudyVo.updateFrom(
            existingStudy,
            "Updated Title",
            null, // description은 변경하지 않음
            null, // content는 변경하지 않음
            null, // category는 변경하지 않음
            null, // subCategory는 변경하지 않음
            null, // startDate는 변경하지 않음
            null, // endDate는 변경하지 않음
            null  // maxParticipants는 변경하지 않음
        );

        // Then - 상태가 유지되어야 함
        assertThat(updatedStudy.status()).isEqualTo("INPROGRESS");
        assertThat(updatedStudy.title()).isEqualTo("Updated Title");
    }
}
