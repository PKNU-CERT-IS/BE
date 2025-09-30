package org.certis.studyplatform.study.domain.repository;

import org.certis.studyplatform.study.domain.vo.StudyParticipantCreatedVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantStatusUpdatedVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantVo;

/**
 * Study Participant Command Repository Interface
 *
 * CQRS Command 측면의 Repository (Write 작업)
 * Domain Layer의 인터페이스
 * Infrastructure Layer에서 JPA로 구현
 */
public interface StudyParticipantCommandRepository {

    /**
     * 스터디 참가 신청 생성
     */
    StudyParticipantCreatedVo save(StudyParticipantVo participantVo);

    /**
     * 스터디 참가자 상태 업데이트
     */
    StudyParticipantStatusUpdatedVo updateStatus(StudyParticipantVo participantVo, Long requesterId);

    /**
     * 스터디 참가 신청 취소 (소프트 삭제)
     */
    void deleteByStudyIdAndMemberId(Long studyId, Long memberId);

    /**
     * 참가 신청 단건 하드 삭제 (승인 취소 등)
     */
    void deleteByIdHard(Long participantId);

    /**
     * 참가 신청 단건 소프트 삭제 (거절 등)
     */
    void softDeleteById(Long participantId);

    /**
     * 소프트 삭제된 참가 신청 복원 (deletedAt → NULL, updatedAt 갱신)
     * @return 복원된 행 수 (0이면 복원 대상 없음)
     */
    int restoreByStudyIdAndMemberId(Long studyId, Long memberId);
}