package org.certis.studyplatform.study.application.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;
import org.certis.studyplatform.study.domain.repository.StudyParticipantQueryRepository;
import org.certis.studyplatform.study.domain.vo.StudyParticipantSummaryVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Study Participant Query Service
 *
 * Clean Architecture Application Layer
 * 프로젝트 참가 관련 조회 작업 처리 (CQRS Query Side)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudyParticipantQueryService {

    private final StudyParticipantQueryRepository queryRepository;

    /**
     * 프로젝트 참가자 상세 조회
     */
    public Optional<StudyParticipantVo> getParticipantById(Long participantId) {
        log.info("Query: Getting participant by ID - {}", participantId);
        return queryRepository.findById(participantId);
    }

    /**
     * 프로젝트별 참가자 목록 조회 (상태별 필터링)
     */
    public Page<StudyParticipantSummaryVo> getParticipantsByStudy(Long studyId,
                                                                      StudyParticipantStatus status,
                                                                      Pageable pageable) {
        log.info("Query: Getting participants by study - studyId: {}, status: {}", studyId, status);
        return queryRepository.findByStudyId(studyId, status, pageable);
    }

    /**
     * 프로젝트별 모든 참가자 목록 조회
     */
    public Page<StudyParticipantSummaryVo> getAllParticipantsByStudy(Long studyId, Pageable pageable) {
        log.info("Query: Getting all participants by study - studyId: {}", studyId);
        return queryRepository.findByStudyId(studyId, pageable);
    }

    /**
     * 사용자별 참가 프로젝트 목록 조회
     */
    public Page<StudyParticipantSummaryVo> getParticipantsByMember(Long memberId, Pageable pageable) {
        log.info("Query: Getting participants by member - memberId: {}", memberId);
        return queryRepository.findByMemberId(memberId, pageable);
    }

    /**
     * 프로젝트의 현재 참가자 수 조회
     */
    public long getApprovedParticipantCount(Long studyId) {
        log.info("Query: Getting approved participant count - studyId: {}", studyId);
        return queryRepository.countApprovedParticipantsByStudyId(studyId);
    }

    /**
     * 프로젝트의 대기 중인 참가 신청 수 조회
     */
    public long getPendingParticipantCount(Long studyId) {
        log.info("Query: Getting pending participant count - studyId: {}", studyId);
        return queryRepository.countPendingParticipantsByStudyId(studyId);
    }

    /**
     * Find participant by (studyId, memberId)
     */
    public Optional<StudyParticipantVo> getByStudyIdAndMemberId(Long studyId, Long memberId) {
        log.info("Query: Getting participant by studyId and memberId - studyId: {}, memberId: {}", studyId, memberId);
        return queryRepository.findByStudyIdAndMemberId(studyId, memberId);
    }
}