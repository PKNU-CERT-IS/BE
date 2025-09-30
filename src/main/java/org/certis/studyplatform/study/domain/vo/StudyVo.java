package org.certis.studyplatform.study.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.study.domain.StudyStatus;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.certis.studyplatform.shared.util.DateTimeUtils;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Study Value Object
 *
 * Clean Architecture Domain Layer
 * 불변 데이터 전송 객체
 * StudyDetailResponseDto와 매칭되는 완전한 프로젝트 정보
 */
public record StudyVo(
        Long id,
        String title,
        String description,
        String content,
        String category,
        String subCategory,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long creatorId,
        String creatorName,
        MemberGrade creatorGrade,
        String creatorProfileImageUrl,
        String semester,
        String status,
        ResultSubmitStatus resultSubmitStatus,
        Integer maxParticipants,
        Integer currentParticipants,
        boolean isParticipantable,
        List<StudyAttachedVo> attached,
        List<StudyMeetingSummaryVo> summaryVoList,
        List<StudyParticipantVo> participantVoList
) {

    /**
     * Compact constructor with validation
     */
    public StudyVo {
        // 제목/설명/내용/카테고리 길이 검증
        if (title == null || title.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_INVALID_TITLE, "스터디 제목은 필수입니다");
        }
        if (description != null && description.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION, "스터디 설명은 비어있을 수 없습니다");
        }
        if (content != null && content.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION, "스터디 내용은 비어있을 수 없습니다");
        }
        if (category != null && category.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION, "스터디 카테고리는 비어있을 수 없습니다");
        }
        if (subCategory != null && subCategory.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION, "스터디 하위 카테고리는 비어있을 수 없습니다");
        }

        // 기간 검증
        if (startDate == null || endDate == null) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_DATE_INVALID, "스터디 시작일과 종료일은 필수입니다");
        }

        // 시작일은 월요일이어야 함
        java.time.DayOfWeek studyStartDayOfWeek = startDate.getDayOfWeek();
        if (studyStartDayOfWeek != java.time.DayOfWeek.MONDAY) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_INVALID_START_DAY, "스터디 시작일은 월요일이어야 합니다");
        }

        // 스터디 종료 시에는 시작일과 종료일 비교를 건너뛰기
        // (ended_at을 현재 시간으로 설정할 때 startDate가 현재 시간보다 늦을 수 있음)
        if (id != null && endDate != null && endDate.isAfter(OffsetDateTime.now().minusMinutes(1))) {
            // 스터디 종료 중인 경우 validation 건너뛰기
        } else if (startDate.isAfter(endDate)) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_DATE_INVALID, "시작일은 종료일보다 빨라야 합니다");
        }

        // 과거 날짜 검증 (id가 null인 경우만 - 새로 생성하는 경우)
        if (id == null && startDate.isBefore(OffsetDateTime.now())) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_DATE_INVALID, "스터디 시작일은 현재보다 미래여야 합니다");
        }

        // 최대 참가자 수 검증
        if (maxParticipants == null || maxParticipants < 1) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_MAX_PARTICIPANTS_INVALID, "최대 참가자 수는 1명 이상이어야 합니다");
        }

        // 현재 참가자 수 검증
        if (currentParticipants != null && currentParticipants < 0) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_MAX_PARTICIPANTS_INVALID, "현재 참가자 수는 0명 이상이어야 합니다");
        }

        // 현재 참가자 수가 최대 참가자 수를 초과하는지 검증
        if (currentParticipants != null && currentParticipants > maxParticipants) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_MAX_PARTICIPANTS_TOO_SMALL, "현재 참가자 수는 최대 참가자 수를 초과할 수 없습니다");
        }

        // Creator ID 검증
        if (creatorId == null) {
            throw new DomainException(ExceptionStatus.STUDY_DOMAIN_INVALID_CREATOR, "스터디 생성자 ID는 필수입니다");
        }
    }

    /**
     * 기본 팩토리 메서드 (스터디 핵심 정보로 VO 생성)
     * 목록 정보는 빈 리스트로 초기화됩니다.
     */
    public static StudyVo of(
            Long id,
            String title,
            String description,
            String content,
            String category,
            String subCategory,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            Long creatorId,
            String creatorName,
            MemberGrade creatorGrade,
            String creatorProfileImageUrl,
            String semester,
            String status,
            ResultSubmitStatus resultSubmitStatus,
            Integer maxParticipants,
            Integer currentParticipants,
            boolean isParticipantable,
            List<StudyAttachedVo> attached
    ) {
        return new StudyVo(
                id, title, description, content, category, subCategory,
                startDate, endDate, createdAt, updatedAt,
                creatorId, creatorName, creatorGrade, creatorProfileImageUrl,
                semester, status, resultSubmitStatus,
                maxParticipants, currentParticipants,
                isParticipantable,
                attached,
                Collections.emptyList(), Collections.emptyList()
        );
    }

    // Backward-compatible factory without resultSubmitStatus used in legacy tests
    public static StudyVo of(
            Long id,
            String title,
            String description,
            String content,
            String category,
            String subCategory,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            Long creatorId,
            String creatorName,
            MemberGrade creatorGrade,
            String creatorProfileImageUrl,
            String semester,
            String status,
            Integer maxParticipants,
            Integer currentParticipants,
            boolean isParticipantable,
            List<StudyAttachedVo> attached
    ) {
        return of(id, title, description, content, category, subCategory, startDate, endDate,
                createdAt, updatedAt, creatorId, creatorName, creatorGrade, creatorProfileImageUrl, semester, status, null,
                maxParticipants, currentParticipants, isParticipantable, attached);
    }

    /**
     * 새 스터디 생성용 팩토리 메서드
     */
    public static StudyVo createNew(
            String title,
            String description,
            String content,
            String category,
            String subCategory,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Long creatorId,
            String creatorName,
            MemberGrade creatorGrade,
            String creatorProfileImageUrl,
            Integer maxParticipants
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        return new StudyVo(
                null, // id는 null (새 생성)
                title, description, content, category, subCategory,
                startDate, endDate, now, now, // createdAt, updatedAt
                creatorId, creatorName, creatorGrade, creatorProfileImageUrl,
                calculateSemester(endDate), // semester 계산
                calculateStatusWithStartDate(startDate, endDate), // status 계산 (startDate 고려)
                ResultSubmitStatus.READY, // 새로 생성된 스터디는 READY
                maxParticipants, 0, // 초기 참가자는 0명
                true, // 새로 생성된 스터디는 참여 가능
                Collections.emptyList(),
                Collections.emptyList(), // 초기 회의록 목록은 비어 있음
                Collections.emptyList()  // 초기 참가자 목록은 비어 있음
        );
    }

    /**
     * 업데이트용 팩토리 메서드
     */
    public static StudyVo updateFrom(StudyVo existing,
                                     String title,
                                     String description,
                                     String content,
                                     String category,
                                     String subCategory,
                                     OffsetDateTime startDate,
                                     OffsetDateTime endDate,
                                     Integer maxParticipants) {
        // startDate 변경 시 상태 검증
        if (startDate != null && !startDate.equals(existing.startDate())) {
            StudyStatus currentStatus = StudyStatus.fromStatusString(existing.status());
            if (!currentStatus.isReady()) {
                throw new DomainException(ExceptionStatus.STUDY_DOMAIN_INVALID_STATUS,
                        "스터디가 READY 상태가 아닐 때는 시작일을 변경할 수 없습니다. 현재 상태: " + currentStatus.getDescription());
            }
        }
        
        OffsetDateTime newStartDate = startDate != null ? startDate : existing.startDate();
        OffsetDateTime newEndDate = endDate != null ? endDate : existing.endDate();

        // 변경된 기간을 기준으로 상태/제출상태 계산 (기존 상태를 고려)
        StatusAndResultSubmitStatus statusAndSubmit = calculateStatusAndResultSubmitStatus(
                newStartDate, newEndDate, existing.status(), existing.resultSubmitStatus());

        return new StudyVo(
                existing.id(),
                title != null ? title : existing.title(),
                description != null ? description : existing.description(),
                content != null ? content : existing.content(),
                category != null ? category : existing.category(),
                subCategory != null ? subCategory : existing.subCategory(),
                newStartDate,
                newEndDate,
                existing.createdAt(), // 기존 createdAt 유지
                OffsetDateTime.now(), // updatedAt은 현재 시간으로 갱신
                existing.creatorId(),
                existing.creatorName(),
                existing.creatorGrade(), // 기존 creatorGrade 유지
                existing.creatorProfileImageUrl(), // 기존 creatorProfileImageUrl 유지
                calculateSemester(newEndDate), // semester 재계산
                statusAndSubmit.status(),
                statusAndSubmit.resultSubmitStatus(),
                maxParticipants != null ? maxParticipants : existing.maxParticipants(),
                existing.currentParticipants(),
                existing.isParticipantable(), // 기존 참여 가능 여부 유지
                existing.attached(),
                existing.summaryVoList(),    // 기존 summaryVoList 유지
                existing.participantVoList() // 기존 participantVoList 유지

        );
    }

    // Backward-compatible auxiliary constructor for tests using new StudyVo(...) without resultSubmitStatus
    public StudyVo(
            Long id,
            String title,
            String description,
            String content,
            String category,
            String subCategory,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            Long creatorId,
            String creatorName,
            MemberGrade creatorGrade,
            String creatorProfileImageUrl,
            String semester,
            String status,
            Integer maxParticipants,
            Integer currentParticipants,
            boolean isParticipantable,
            List<StudyAttachedVo> attached,
            List<StudyMeetingSummaryVo> summaryVoList,
            List<StudyParticipantVo> participantVoList
    ) {
        this(id, title, description, content, category, subCategory, startDate, endDate,
                createdAt, updatedAt, creatorId, creatorName, creatorGrade, creatorProfileImageUrl, semester, status, null,
                maxParticipants, currentParticipants, isParticipantable, attached, summaryVoList, participantVoList);
    }

    // ================================================================
    // 비즈니스 로직 계산 메서드들
    // ================================================================

    /**
     * ended_at을 기준으로 semester 계산
     * 3월~8월: 해당 년도 1학기
     * 9월~다음해 2월: 해당 년도 2학기
     */
    private static String calculateSemester(OffsetDateTime endedAt) {
        if (endedAt == null) {
            return null;
        }
        
        java.time.LocalDate endDate = endedAt.toLocalDate();
        int year = endDate.getYear();
        int month = endDate.getMonthValue();
        
        if (month >= 3 && month <= 8) {
            return year + "-1"; // 1학기
        } else {
            return year + "-2"; // 2학기
        }
    }

    /**
     * ended_at을 기준으로 status 계산
     * ended_at이 현재 시간보다 지났으면 COMPLETED, 아니면 INPROGRESS
     */
    private static String calculateStatus(OffsetDateTime endedAt) {
        if (endedAt == null) {
            return StudyStatus.INPROGRESS.name(); // 종료일이 없으면 진행 중 상태
        }
        
        OffsetDateTime now = OffsetDateTime.now();
        return endedAt.isBefore(now) ? StudyStatus.COMPLETED.name() : StudyStatus.INPROGRESS.name();
    }

    /**
     * startedAt과 endedAt을 고려하여 상태 계산 (새로 생성할 때 사용)
     */
    private static String calculateStatusWithStartDate(OffsetDateTime startDate, OffsetDateTime endDate) {
        if (startDate == null || endDate == null) {
            return StudyStatus.READY.name();
        }
        
        OffsetDateTime now = OffsetDateTime.now();
        
        // 아직 시작하지 않았으면 READY
        if (now.isBefore(startDate)) {
            return StudyStatus.READY.name();
        }
        
        // 종료되었으면 COMPLETED
        if (now.isAfter(endDate) || now.isEqual(endDate)) {
            return StudyStatus.COMPLETED.name();
        }
        
        // 시작했지만 아직 종료되지 않았으면 INPROGRESS
        return StudyStatus.INPROGRESS.name();
    }

    /**
     * startedAt과 endedAt을 기준으로 status와 resultSubmitStatus 계산
     * 기존 상태를 고려하여 적절한 상태를 유지
     */
    private static StatusAndResultSubmitStatus calculateStatusAndResultSubmitStatus(
            OffsetDateTime startedAt, OffsetDateTime endedAt, String currentStatus, ResultSubmitStatus currentResultSubmitStatus) {
        OffsetDateTime now = OffsetDateTime.now();
        StudyStatus existingStatus = StudyStatus.fromStatusString(currentStatus);
        
        // 기존 상태가 REJECTED이면 유지
        if (existingStatus.isRejected()) {
            return new StatusAndResultSubmitStatus(currentStatus, currentResultSubmitStatus);
        }
        
        // 기존 상태가 COMPLETED이면 유지
        if (existingStatus.isCompleted()) {
            return new StatusAndResultSubmitStatus(currentStatus, currentResultSubmitStatus);
        }
        
        // startedAt이 현재 시각보다 나중인 경우
        if (startedAt != null && startedAt.isAfter(now)) {
            // 기존 상태가 READY가 아닌 경우 기존 상태 유지
            if (!existingStatus.isReady()) {
                return new StatusAndResultSubmitStatus(currentStatus, currentResultSubmitStatus);
            }
            // READY 상태인 경우에만 APPROVED로 변경
            return new StatusAndResultSubmitStatus(StudyStatus.APPROVED.name(), ResultSubmitStatus.READY);
        }
        
        // endedAt이 현재 시간보다 지났으면 COMPLETED
        if (endedAt != null && endedAt.isBefore(now)) {
            return new StatusAndResultSubmitStatus(StudyStatus.COMPLETED.name(), ResultSubmitStatus.READY);
        }
        
        // 기존 상태가 APPROVED나 INPROGRESS인 경우 유지
        if (existingStatus.isApproved() || existingStatus.isInProgress()) {
            return new StatusAndResultSubmitStatus(currentStatus, currentResultSubmitStatus);
        }
        
        // 기존 상태가 READY인 경우에만 INPROGRESS로 변경
        if (existingStatus.isReady()) {
            return new StatusAndResultSubmitStatus(StudyStatus.INPROGRESS.name(), currentResultSubmitStatus);
        }
        
        // 그 외의 경우 기존 상태 유지
        return new StatusAndResultSubmitStatus(currentStatus, currentResultSubmitStatus);
    }

    /**
     * Status와 ResultSubmitStatus를 함께 반환하는 레코드
     */
    private record StatusAndResultSubmitStatus(String status, ResultSubmitStatus resultSubmitStatus) {}

}
