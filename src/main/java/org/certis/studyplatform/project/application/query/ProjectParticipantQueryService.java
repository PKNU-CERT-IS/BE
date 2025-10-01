package org.certis.studyplatform.project.application.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository;
import org.certis.studyplatform.project.domain.vo.ProjectParticipantSummaryVo;
import org.certis.studyplatform.project.domain.vo.ProjectParticipantVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Project Participant Query Service
 *
 * Clean Architecture Application Layer
 * 프로젝트 참가 관련 조회 작업 처리 (CQRS Query Side)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProjectParticipantQueryService {

    private final ProjectParticipantQueryRepository queryRepository;

    /**
     * 프로젝트 참가자 상세 조회
     */
    public Optional<ProjectParticipantVo> getParticipantById(Long participantId) {
        log.info("Query: Getting participant by ID - {}", participantId);
        return queryRepository.findById(participantId);
    }

    /**
     * 프로젝트별 참가자 목록 조회 (상태별 필터링)
     */
    public Page<ProjectParticipantSummaryVo> getParticipantsByProject(Long projectId,
                                                                      ProjectParticipantStatus status,
                                                                      Pageable pageable) {
        log.info("Query: Getting participants by project - projectId: {}, status: {}", projectId, status);
        return queryRepository.findByProjectId(projectId, status, pageable);
    }

    /**
     * 프로젝트별 모든 참가자 목록 조회
     */
    public Page<ProjectParticipantSummaryVo> getAllParticipantsByProject(Long projectId, Pageable pageable) {
        log.info("Query: Getting all participants by project - projectId: {}", projectId);
        return queryRepository.findByProjectId(projectId, pageable);
    }

    /**
     * 사용자별 참가 프로젝트 목록 조회
     */
    public Page<ProjectParticipantSummaryVo> getParticipantsByMember(Long memberId, Pageable pageable) {
        log.info("Query: Getting participants by member - memberId: {}", memberId);
        return queryRepository.findByMemberId(memberId, pageable);
    }

    /**
     * 프로젝트의 현재 참가자 수 조회
     */
    public long getApprovedParticipantCount(Long projectId) {
        log.info("Query: Getting approved participant count - projectId: {}", projectId);
        return queryRepository.countApprovedParticipantsByProjectId(projectId);
    }

    /**
     * 프로젝트의 대기 중인 참가 신청 수 조회
     */
    public long getPendingParticipantCount(Long projectId) {
        log.info("Query: Getting pending participant count - projectId: {}", projectId);
        return queryRepository.countPendingParticipantsByProjectId(projectId);
    }

    /**
     * Find participant by (projectId, memberId)
     */
    public Optional<ProjectParticipantVo> getByProjectIdAndMemberId(Long projectId, Long memberId) {
        log.info("Query: Getting participant by projectId and memberId - projectId: {}, memberId: {}", projectId, memberId);
        return queryRepository.findByProjectIdAndMemberId(projectId, memberId);
    }
}