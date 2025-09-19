package org.certis.studyplatform.project.domain.repository;

import org.certis.studyplatform.project.domain.vo.*;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Project Participant Query Repository Interface
 *
 * CQRS Query 측면의 Repository (Read 작업)
 * Domain Layer의 인터페이스
 * Infrastructure Layer에서 jOOQ로 구현
 */
public interface ProjectParticipantQueryRepository {

    /**
     * 프로젝트 참가자 상세 조회
     */
    Optional<ProjectParticipantVo> findById(Long participantId);

    /**
     * 프로젝트별 참가자 목록 조회 (상태별 필터링 가능)
     */
    Page<ProjectParticipantSummaryVo> findByProjectId(Long projectId, ProjectParticipantStatus status, Pageable pageable);

    /**
     * 프로젝트별 모든 참가자 목록 조회
     */
    Page<ProjectParticipantSummaryVo> findByProjectId(Long projectId, Pageable pageable);

    /**
     * 사용자별 참가 프로젝트 목록 조회
     */
    Page<ProjectParticipantSummaryVo> findByMemberId(Long memberId, Pageable pageable);

    /**
     * 특정 프로젝트에 특정 사용자가 이미 참가 신청했는지 확인
     */
    boolean existsByProjectIdAndMemberId(Long projectId, Long memberId);

    /**
     * 특정 프로젝트에 특정 사용자의 참가 신청 조회 (상태 무관)
     */
    Optional<ProjectParticipantVo> findByProjectIdAndMemberId(Long projectId, Long memberId);

    /**
     * 특정 프로젝트에 특정 사용자의 PENDING 상태 참가 신청 조회
     */
    Optional<ProjectParticipantVo> findPendingByProjectIdAndMemberId(Long projectId, Long memberId);

    /**
     * 프로젝트의 현재 참가자 수 조회 (APPROVED 상태만)
     */
    long countApprovedParticipantsByProjectId(Long projectId);

    /**
     * 프로젝트의 대기 중인 참가 신청 수 조회
     */
    long countPendingParticipantsByProjectId(Long projectId);

    /**
     * 프로젝트의 모든 승인된 참가자 목록 조회 (페이징 없음)
     */
    List<ProjectParticipantSummaryVo> findAllApprovedByProjectId(Long projectId);

    /**
     * 현재 진행 중(기간 내)이며 APPROVED인 프로젝트 수 (회원 기준)
     */
    long countActiveProjectsByMemberId(Long memberId);
}
