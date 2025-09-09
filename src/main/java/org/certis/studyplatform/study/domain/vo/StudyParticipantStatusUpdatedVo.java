package org.certis.studyplatform.study.domain.vo;

import org.certis.studyplatform.study.domain.StudyParticipantStatus;

import java.time.OffsetDateTime;

/**
 * Study Participant Status Updated VO
 *
 * 프로젝트 참가자 상태 변경 결과 VO
 */
public record StudyParticipantStatusUpdatedVo(
        Long id,
        Long studyId,
        Long memberId,
        StudyParticipantStatus previousStatus,
        StudyParticipantStatus currentStatus,
        OffsetDateTime updatedAt
) {

    /**
     * 벌크 연산용 정적 팩토리 메소드
     * 이전 상태를 알 수 없는 경우 사용 (previousStatus = null)
     *
     * @param participantVo 참가자 정보 VO
     * @return StudyParticipantStatusUpdatedVo
     */
    public static StudyParticipantStatusUpdatedVo ofBulkUpdate(StudyParticipantVo participantVo) {
        return new StudyParticipantStatusUpdatedVo(
                participantVo.id(),
                participantVo.studyId(),
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
     * @return StudyParticipantStatusUpdatedVo
     */
    public static StudyParticipantStatusUpdatedVo of(StudyParticipantVo participantVo,
                                                       StudyParticipantStatus previousStatus) {
        return new StudyParticipantStatusUpdatedVo(
                participantVo.id(),
                participantVo.studyId(),
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
     * @param studyId 프로젝트 ID
     * @param memberId 멤버 ID
     * @param previousStatus 이전 상태
     * @param currentStatus 현재 상태
     * @return StudyParticipantStatusUpdatedVo
     */
    public static StudyParticipantStatusUpdatedVo of(Long id,
                                                       Long studyId,
                                                       Long memberId,
                                                       StudyParticipantStatus previousStatus,
                                                       StudyParticipantStatus currentStatus) {
        return new StudyParticipantStatusUpdatedVo(
                id,
                studyId,
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
     * @return StudyParticipantStatusUpdatedVo
     */
    public static StudyParticipantStatusUpdatedVo ofStatusChange(Long id,
                                                                   StudyParticipantStatus previousStatus,
                                                                   StudyParticipantStatus currentStatus) {
        return new StudyParticipantStatusUpdatedVo(
                id,
                null, // 별도로 설정 필요
                null, // 별도로 설정 필요
                previousStatus,
                currentStatus,
                OffsetDateTime.now()
        );
    }
}