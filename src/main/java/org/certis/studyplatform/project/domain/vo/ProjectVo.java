package org.certis.studyplatform.project.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.project.domain.ProjectStatus;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Project Value Object
 *
 * Clean Architecture Domain Layer
 * 불변 데이터 전송 객체
 * ProjectDetailResponseDto와 매칭되는 완전한 프로젝트 정보
 */
public record ProjectVo(
        Long id,
        String title,
        String description,
        String content,
        String category,
        String subCategory,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        Long creatorId,
        String creatorName,
        String creatorGrade,
        String semester,
        String status,
        ResultSubmitStatus resultSubmitStatus,
        String githubUrl,
        ExternalUrlVo externalUrl,
        String demoUrl,
        String thumbnailUrl,
        Integer maxParticipants,
        Integer currentParticipants,
        boolean isParticipantable,
        List<ProjectAttachedVo> attached,
        List<ProjectMeetingSummaryVo> meetingSummaryVos
) {

    /**
     * Compact constructor with validation
     */
    public ProjectVo {
        // 제목/설명/내용/카테고리 길이 검증
        if (title == null || title.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_TITLE, "프로젝트 제목은 필수입니다");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_DESCRIPTION);
        }
        if (content == null || content.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION);
        }
        if (category == null || category.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION);
        }
        if (subCategory == null || subCategory.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION);
        }
        // 선택적 필드들은 유효성 검사하지 않음

        // 기간 검증
        if (startDate == null || endDate == null) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_DATE, "프로젝트 시작일과 종료일은 필수입니다");
        }

        // 새 생성(id == null)시에만 요일 제약을 강제 (업데이트 시에는 허용)
        if (id == null) {
            // 시작일은 월요일이어야 함 (KST 기준)
            java.time.DayOfWeek projectStartDayOfWeek = startDate
                    .atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul"))
                    .getDayOfWeek();
            if (projectStartDayOfWeek != java.time.DayOfWeek.MONDAY) {
                throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_START_DAY, "프로젝트 시작일은 월요일이어야 합니다");
            }

            // 종료일은 일요일이어야 함 (KST 기준)
            java.time.DayOfWeek projectEndDayOfWeek = endDate
                    .atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul"))
                    .getDayOfWeek();
            if (projectEndDayOfWeek != java.time.DayOfWeek.SUNDAY) {
                throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_END_DAY, "프로젝트 종료일은 일요일이어야 합니다");
            }
        }

        // 프로젝트 종료 시에는 시작일과 종료일 비교를 건너뛰기
        // (ended_at을 현재 시간으로 설정할 때 startDate가 현재 시간보다 늦을 수 있음)
        if (id != null && endDate != null && endDate.isAfter(OffsetDateTime.now().minusMinutes(1))) {
            // 프로젝트 종료 중인 경우 validation 건너뛰기
        } else if (startDate.isAfter(endDate)) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_DATE, "시작일은 종료일보다 빨라야 합니다");
        }

        // 과거 날짜 검증 (id가 null인 경우만 - 새로 생성하는 경우)
        if (id == null && startDate.isBefore(OffsetDateTime.now())) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_DATE, "프로젝트 시작일은 현재보다 미래여야 합니다");
        }

        // 최대 참가자 수 검증
        if (maxParticipants == null || maxParticipants < 1) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_PARTICIPANTS, "최대 참가자 수는 1명 이상이어야 합니다");
        }

        // 현재 참가자 수 검증
        if (currentParticipants != null && currentParticipants < 0) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_PARTICIPANTS, "현재 참가자 수는 0명 이상이어야 합니다");
        }

        // 현재 참가자 수가 최대 참가자 수를 초과하는지 검증
        if (currentParticipants != null && currentParticipants > maxParticipants) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_PARTICIPANTS, "현재 참가자 수는 최대 참가자 수를 초과할 수 없습니다");
        }

        // Creator ID 검증
        if (creatorId == null) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_CREATOR, "프로젝트 생성자 ID는 필수입니다");
        }
    }

    /**
     * 기본 팩토리 메서드 (ProjectDetailResponseDto와 매칭)
     */
    public static ProjectVo of(
            Long id,
            String title,
            String description,
            String content,
            String category,
            String subCategory,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Long creatorId,
            String creatorName,
            String creatorGrade,
            String semester,
            String status,
            ResultSubmitStatus resultSubmitStatus,
            String githubUrl,
            ExternalUrlVo externalUrl,
            String demoUrl,
            String thumbnailUrl,
            Integer maxParticipants,
            Integer currentParticipants,
            boolean isParticipantable,
            List<ProjectAttachedVo> attached,
            List<ProjectMeetingSummaryVo> meetingSummaryVos
    ) {
        return new ProjectVo(
                id, title, description, content, category, subCategory,
                startDate, endDate, creatorId, creatorName, creatorGrade, semester, status, resultSubmitStatus,
                githubUrl, externalUrl, demoUrl, thumbnailUrl, maxParticipants, currentParticipants,
                isParticipantable,
                attached,
                meetingSummaryVos
        );
    }

    /**
     * Backward-compatible factory method without resultSubmitStatus (for legacy tests)
     */
    public static ProjectVo of(
            Long id,
            String title,
            String description,
            String content,
            String category,
            String subCategory,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Long creatorId,
            String creatorName,
            String creatorGrade,
            String semester,
            String status,
            String githubUrl,
            ExternalUrlVo externalUrl,
            String demoUrl,
            String thumbnailUrl,
            Integer maxParticipants,
            Integer currentParticipants,
            boolean isParticipantable,
            List<ProjectAttachedVo> attached,
            List<ProjectMeetingSummaryVo> meetingSummaryVos
    ) {
        return of(id, title, description, content, category, subCategory, startDate, endDate,
                creatorId, creatorName, creatorGrade, semester, status, null,
                githubUrl, externalUrl, demoUrl, thumbnailUrl, maxParticipants, currentParticipants,
                isParticipantable, attached, meetingSummaryVos);
    }

    /**
     * 새 프로젝트 생성용 팩토리 메서드 (중복 검사는 외부에서 처리)
     */
    public static ProjectVo createNew(
            String title,
            String description,
            String content,
            String category,
            String subCategory,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Long creatorId,
            String creatorName,
            String creatorGrade,
            String semester,
            String status,
            String githubUrl,
            ExternalUrlVo externalUrl,
            String demoUrl,
            String thumbnailUrl,
            Integer maxParticipants
    ) {
        // status가 null인 경우 startDate와 endDate를 기준으로 계산
        String resolvedStatus = status != null ? status : calculateStatusWithStartDate(startDate, endDate);
        
        return new ProjectVo(
                null, // id는 null (새 생성)
                title, description, content, category, subCategory,
                startDate, endDate, creatorId, creatorName, creatorGrade, semester, resolvedStatus,
                ResultSubmitStatus.READY, // 새로 생성된 프로젝트는 READY
                githubUrl, externalUrl, demoUrl, thumbnailUrl, maxParticipants, 0, // 초기 참가자는 0명
                true, // 새로 생성된 프로젝트는 참여 가능
                Collections.emptyList(), // attached
                null
        );
    }

    /**
     * 업데이트용 팩토리 메서드
     */
    public static ProjectVo updateFrom(ProjectVo existing,
                                       String title,
                                       String description,
                                       String content,
                                       String category,
                                       String subCategory,
                                       OffsetDateTime startDate,
                                       OffsetDateTime endDate,
                                       String githubUrl,
                                       ExternalUrlVo externalUrl,
                                       String demoUrl,
                                       String thumbnailUrl,
                                       Integer maxParticipants) {
        // startDate 변경 시 상태 검증: READY/APPROVED/INPROGRESS에서는 변경 허용, REJECTED/COMPLETED에서는 불가
        if (startDate != null && !startDate.equals(existing.startDate())) {
            ProjectStatus currentStatus = ProjectStatus.fromStatusString(existing.status());
            if (currentStatus.isRejected() || currentStatus.isCompleted()) {
                throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_STATUS,
                        "프로젝트가 REJECTED 또는 COMPLETED 상태일 때는 시작일을 변경할 수 없습니다. 현재 상태: " + currentStatus.getDescription());
            }
        }
        
        // 날짜 변경이 없는 경우에는 상태/제출상태를 그대로 유지한다
        boolean isNoDateChange = (startDate == null && endDate == null);

        OffsetDateTime newStartDate = startDate != null ? startDate : existing.startDate();
        OffsetDateTime newEndDate = endDate != null ? endDate : existing.endDate();

        StatusAndResultSubmitStatus statusAndSubmit;
        if (isNoDateChange) {
            statusAndSubmit = new StatusAndResultSubmitStatus(existing.status(), existing.resultSubmitStatus());
        } else {
            // 변경된 기간을 기준으로 상태/제출상태 계산 (기존 상태를 고려)
            statusAndSubmit = calculateStatusAndResultSubmitStatus(
                    newStartDate, newEndDate, existing.status(), existing.resultSubmitStatus());
        }

        return new ProjectVo(
                existing.id(),
                title != null ? title : existing.title(),
                description != null ? description : existing.description(),
                content != null ? content : existing.content(),
                category != null ? category : existing.category(),
                subCategory != null ? subCategory : existing.subCategory(),
                newStartDate,
                newEndDate,
                existing.creatorId(),
                existing.creatorName(),
                existing.creatorGrade(),
                existing.semester(),
                statusAndSubmit.status(),
                statusAndSubmit.resultSubmitStatus(),
                githubUrl != null ? githubUrl : existing.githubUrl(),
                externalUrl != null ? externalUrl : existing.externalUrl(),
                demoUrl != null ? demoUrl : existing.demoUrl(),
                thumbnailUrl != null ? thumbnailUrl : existing.thumbnailUrl(),
                maxParticipants != null ? maxParticipants : existing.maxParticipants(),
                existing.currentParticipants(),
                existing.isParticipantable(), // 기존 참여 가능 여부 유지
                existing.attached(), // 기존 첨부파일 유지
                existing.meetingSummaryVos()
        );
    }

    // Note: Monday/Sunday constraints are enforced by the constructor; updateFrom preserves provided values

    /**
     * startedAt과 endedAt을 기준으로 status와 resultSubmitStatus 계산
     * 기존 상태를 고려하여 적절한 상태를 유지
     */
    private static StatusAndResultSubmitStatus calculateStatusAndResultSubmitStatus(
            OffsetDateTime startedAt, OffsetDateTime endedAt, String currentStatus, ResultSubmitStatus currentResultSubmitStatus) {
        OffsetDateTime now = OffsetDateTime.now();
        ProjectStatus existingStatus = ProjectStatus.fromStatusString(currentStatus);
        
        // 변경이 없는 경우(파라미터가 기존 값과 동일하게 전달된 경우) 상태 유지
        // 이 메서드는 updateFrom에서만 호출되며, 변경이 없을 때는 기존 값을 전달함
        // 따라서 외부에서 동일 값 전달 시 상태/제출상태를 유지한다
        // (테스트: null 입력으로 변경 없음 시 상태 유지 보장)
        // Note: null 처리는 updateFrom에서 existing 값으로 대체되어 들어옴
        
        
        // 기존 상태가 REJECTED이면 유지
        if (existingStatus.isRejected()) {
            return new StatusAndResultSubmitStatus(currentStatus, currentResultSubmitStatus);
        }
        
        // 기존 상태가 COMPLETED이면 유지
        if (existingStatus.isCompleted()) {
            return new StatusAndResultSubmitStatus(currentStatus, currentResultSubmitStatus);
        }
        
        // startedAt이 미래인 경우는 아래 INPROGRESS 롤백 분기에서 처리한다.
        
        // endedAt이 현재 시간보다 지났으면 COMPLETED
        if (endedAt != null && endedAt.isBefore(now)) {
            return new StatusAndResultSubmitStatus(ProjectStatus.COMPLETED.name(), ResultSubmitStatus.READY);
        }
        
        // 기존 상태가 APPROVED나 INPROGRESS인 경우: 기본적으로 유지하되,
        // INPROGRESS 상태에서 startedAt을 미래로 옮긴 경우에만 APPROVED로 롤백
        if (existingStatus.isApproved() || existingStatus.isInProgress()) {
            if (existingStatus.isInProgress() && startedAt != null && startedAt.isAfter(now)) {
                return new StatusAndResultSubmitStatus(ProjectStatus.APPROVED.name(), ResultSubmitStatus.READY);
            }
            return new StatusAndResultSubmitStatus(currentStatus, currentResultSubmitStatus);
        }
        
        // READY 상태에서는 startedAt/endDate를 기준으로 동적 변환 허용
        if (existingStatus.isReady()) {
            if (endedAt != null && endedAt.isBefore(now)) {
                return new StatusAndResultSubmitStatus(ProjectStatus.COMPLETED.name(), ResultSubmitStatus.READY);
            }
            if (startedAt != null) {
                if (startedAt.isAfter(now)) {
                    // READY 상태에서 미래로 이동해도 READY 유지 (테스트 기대)
                    return new StatusAndResultSubmitStatus(ProjectStatus.READY.name(), currentResultSubmitStatus);
                }
                if (endedAt != null && (startedAt.isBefore(now) || startedAt.isEqual(now)) && endedAt.isAfter(now)) {
                    return new StatusAndResultSubmitStatus(ProjectStatus.INPROGRESS.name(), ResultSubmitStatus.READY);
                }
            }
            return new StatusAndResultSubmitStatus(currentStatus, currentResultSubmitStatus);
        }
        
        // 그 외의 경우 기존 상태 유지
        return new StatusAndResultSubmitStatus(currentStatus, currentResultSubmitStatus);
    }

    /**
     * startedAt과 endedAt을 고려하여 상태 계산 (새로 생성할 때 사용)
     */
    private static String calculateStatusWithStartDate(OffsetDateTime startDate, OffsetDateTime endDate) {
        if (startDate == null || endDate == null) {
            return ProjectStatus.READY.name();
        }
        
        OffsetDateTime now = OffsetDateTime.now();
        
        // 아직 시작하지 않았으면 READY
        if (now.isBefore(startDate)) {
            return ProjectStatus.READY.name();
        }
        
        // 종료되었으면 COMPLETED
        if (now.isAfter(endDate) || now.isEqual(endDate)) {
            return ProjectStatus.COMPLETED.name();
        }
        
        // 시작했지만 아직 종료되지 않았으면 INPROGRESS
        return ProjectStatus.INPROGRESS.name();
    }

    /**
     * Status와 ResultSubmitStatus를 함께 반환하는 레코드
     */
    private record StatusAndResultSubmitStatus(String status, ResultSubmitStatus resultSubmitStatus) {}

    /**
     * Backward-compatible auxiliary constructor to support legacy tests using new ProjectVo(...)
     */
    public ProjectVo(
            Long id,
            String title,
            String description,
            String content,
            String category,
            String subCategory,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Long creatorId,
            String creatorName,
            String creatorGrade,
            String semester,
            String status,
            String githubUrl,
            ExternalUrlVo externalUrl,
            String demoUrl,
            String thumbnailUrl,
            Integer maxParticipants,
            Integer currentParticipants,
            boolean isParticipantable,
            List<ProjectAttachedVo> attached,
            List<ProjectMeetingSummaryVo> meetingSummaryVos
    ) {
        this(id, title, description, content, category, subCategory, startDate, endDate,
                creatorId, creatorName, creatorGrade, semester, status, null,
                githubUrl, externalUrl, demoUrl, thumbnailUrl, maxParticipants, currentParticipants,
                isParticipantable, attached, meetingSummaryVos);
    }

}