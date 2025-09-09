package org.certis.studyplatform.study.domain.repository;

import org.certis.studyplatform.study.domain.StudyParticipantStatus;
import org.certis.studyplatform.study.domain.vo.StudyParticipantSummaryVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Study Participant Query Repository Interface
 *
 * CQRS Query 측면의 Repository (Read 작업)
 * Domain Layer의 인터페이스
 * Infrastructure Layer에서 jOOQ로 구현
 */
public interface StudyParticipantQueryRepository {

    /**
     * 스터디 참가자 상세 조회
     */
    Optional<StudyParticipantVo> findById(Long participantId);

    /**
     * 스터디별 참가자 목록 조회 (상태별 필터링 가능)
     */
    Page<StudyParticipantSummaryVo> findByStudyId(Long studyId, StudyParticipantStatus status, Pageable pageable);

    /**
     * 스터디별 모든 참가자 목록 조회
     */
    Page<StudyParticipantSummaryVo> findByStudyId(Long studyId, Pageable pageable);

    /**
     * 사용자별 참가 스터디 목록 조회
     */
    Page<StudyParticipantSummaryVo> findByMemberId(Long memberId, Pageable pageable);

    /**
     * 특정 스터디에 특정 사용자가 이미 참가 신청했는지 확인
     */
    boolean existsByStudyIdAndMemberId(Long studyId, Long memberId);

    /**
     * 특정 스터디에 특정 사용자의 참가 신청 조회 (상태 무관)
     */
    Optional<StudyParticipantVo> findByStudyIdAndMemberId(Long studyId, Long memberId);

    /**
     * 특정 스터디에 특정 사용자의 PENDING 상태 참가 신청 조회
     */
    Optional<StudyParticipantVo> findPendingByStudyIdAndMemberId(Long studyId, Long memberId);

    /**
     * 스터디의 현재 참가자 수 조회 (APPROVED 상태만)
     */
    long countApprovedParticipantsByStudyId(Long studyId);

    /**
     * 스터디의 대기 중인 참가 신청 수 조회
     */
    long countPendingParticipantsByStudyId(Long studyId);
}
