package org.certis.studyplatform.project.domain.vo;

import org.certis.studyplatform.project.domain.ProjectParticipantStatus;

import java.time.OffsetDateTime;

/**
 * Project Participant Status Updated VO
 *
 * 프로젝트 참가자 상태 변경 결과 VO
 */
public record ProjectParticipantStatusUpdatedVo(
        Long id,
        Long projectId,
        Long memberId,
        ProjectParticipantStatus previousStatus,
        ProjectParticipantStatus currentStatus,
        OffsetDateTime updatedAt
) {

    /**
     * 벌크 연산용 정적 팩토리 메소드
     * 이전 상태를 알 수 없는 경우 사용 (previousStatus = null)
     *
     * @param participantVo 참가자 정보 VO
     * @return ProjectParticipantStatusUpdatedVo
     */
    public static ProjectParticipantStatusUpdatedVo ofBulkUpdate(ProjectParticipantVo participantVo) {
        return new ProjectParticipantStatusUpdatedVo(
                participantVo.id(),
                participantVo.projectId(),
                participantVo.memberId(),
                null, // 벌크 연산에서는 이전 상태를 알 수 없음
                participantVo.status(),
                OffsetDateTime.now()
        );
    }

    /**
     * 일반적인 상태 변경용 정적 팩토리 메소드
     * 이전 상태와 현재 상태를 모두 아는 경우 사용
     *
     * @param participantVo 참가자 정보 VO
     * @param previousStatus 이전 상태
     * @return ProjectParticipantStatusUpdatedVo
     */
    public static ProjectParticipantStatusUpdatedVo of(ProjectParticipantVo participantVo,
                                                       ProjectParticipantStatus previousStatus) {
        return new ProjectParticipantStatusUpdatedVo(
                participantVo.id(),
                participantVo.projectId(),
                participantVo.memberId(),
                previousStatus,
                participantVo.status(),
                OffsetDateTime.now()
        );
    }

    /**
     * 모든 필드를 직접 지정하는 정적 팩토리 메소드
     *
     * @param id 참가자 ID
     * @param projectId 프로젝트 ID
     * @param memberId 멤버 ID
     * @param previousStatus 이전 상태
     * @param currentStatus 현재 상태
     * @return ProjectParticipantStatusUpdatedVo
     */
    public static ProjectParticipantStatusUpdatedVo of(Long id,
                                                       Long projectId,
                                                       Long memberId,
                                                       ProjectParticipantStatus previousStatus,
                                                       ProjectParticipantStatus currentStatus) {
        return new ProjectParticipantStatusUpdatedVo(
                id,
                projectId,
                memberId,
                previousStatus,
                currentStatus,
                OffsetDateTime.now()
        );
    }

    /**
     * 간단한 상태 변경용 정적 팩토리 메소드
     * ID와 상태 정보만으로 생성 (프로젝트 ID, 멤버 ID는 별도로 조회 필요 시 사용)
     *
     * @param id 참가자 ID
     * @param previousStatus 이전 상태
     * @param currentStatus 현재 상태
     * @return ProjectParticipantStatusUpdatedVo
     */
    public static ProjectParticipantStatusUpdatedVo ofStatusChange(Long id,
                                                                   ProjectParticipantStatus previousStatus,
                                                                   ProjectParticipantStatus currentStatus) {
        return new ProjectParticipantStatusUpdatedVo(
                id,
                null, // 별도로 설정 필요
                null, // 별도로 설정 필요
                previousStatus,
                currentStatus,
                OffsetDateTime.now()
        );
    }
}