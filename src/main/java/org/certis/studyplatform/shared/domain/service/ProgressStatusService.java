package org.certis.studyplatform.shared.domain.service;

import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.certis.studyplatform.study.domain.StudyStatus;
import org.certis.studyplatform.project.domain.ProjectStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Progress Status Service
 * 
 * Study와 Project의 상태 관리를 위한 도메인 서비스
 * 요청된 상태 테이블에 따라 status와 resultSubmitStatus를 계산
 */
@Service
public class ProgressStatusService {

    /**
     * Study의 상태를 계산합니다.
     * 
     * @param currentStatus 현재 상태
     * @param currentResultSubmitStatus 현재 결과 제출 상태
     * @param startedAt 시작 시간
     * @param endedAt 종료 시간
     * @param currentTime 현재 시간
     * @return 새로운 상태와 결과 제출 상태
     */
    public ProgressStatusResult calculateStudyStatus(
            StudyStatus currentStatus,
            ResultSubmitStatus currentResultSubmitStatus,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            OffsetDateTime currentTime) {
        
        // 시간 기반 상태 업데이트
        // APPROVED 상태에서 시작 시간이 지났고 아직 종료 시간이 안 지났으면 INPROGRESS
        if (currentStatus == StudyStatus.APPROVED && 
            currentTime.isAfter(startedAt) && 
            (endedAt == null || currentTime.isBefore(endedAt))) {
            return new ProgressStatusResult(StudyStatus.INPROGRESS, currentResultSubmitStatus);
        }
        
        // INPROGRESS 상태에서 종료 시간이 지났으면 COMPLETED
        if (currentStatus == StudyStatus.INPROGRESS && 
            endedAt != null && 
            currentTime.isAfter(endedAt)) {
            return new ProgressStatusResult(StudyStatus.COMPLETED, ResultSubmitStatus.READY);
        }
        
        return new ProgressStatusResult(currentStatus, currentResultSubmitStatus);
    }

    /**
     * Project의 상태를 계산합니다.
     * 
     * @param currentStatus 현재 상태
     * @param currentResultSubmitStatus 현재 결과 제출 상태
     * @param startedAt 시작 시간
     * @param endedAt 종료 시간
     * @param currentTime 현재 시간
     * @return 새로운 상태와 결과 제출 상태
     */
    public ProgressStatusResult calculateProjectStatus(
            ProjectStatus currentStatus,
            ResultSubmitStatus currentResultSubmitStatus,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            OffsetDateTime currentTime) {
        
        // 시간 기반 상태 업데이트
        // APPROVED 상태에서 시작 시간이 지났고 아직 종료 시간이 안 지났으면 INPROGRESS
        if (currentStatus == ProjectStatus.APPROVED && 
            currentTime.isAfter(startedAt) && 
            (endedAt == null || currentTime.isBefore(endedAt))) {
            return new ProgressStatusResult(ProjectStatus.INPROGRESS, currentResultSubmitStatus);
        }
        
        // INPROGRESS 상태에서 종료 시간이 지났으면 COMPLETED
        if (currentStatus == ProjectStatus.INPROGRESS && 
            endedAt != null && 
            currentTime.isAfter(endedAt)) {
            return new ProgressStatusResult(ProjectStatus.COMPLETED, ResultSubmitStatus.READY);
        }
        
        return new ProgressStatusResult(currentStatus, currentResultSubmitStatus);
    }

    /**
     * 상태 변경 결과를 담는 레코드
     */
    public record ProgressStatusResult(
            Object status,
            ResultSubmitStatus resultSubmitStatus
    ) {}
}
