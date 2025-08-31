package org.certis.studyplatform.project.domain.repository;

import org.certis.studyplatform.project.domain.vo.ProjectMeetingSummaryVo;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Project Meeting Query Repository Interface
 *
 * CQRS Query 측면의 Repository (Read 작업)
 * Domain Layer의 인터페이스
 * Infrastructure Layer에서 QueryDSL 또는 JOOQ로 구현
 *
 * ✅ CQRS 패턴 준수:
 * - 모든 조회 관련 메서드 포함
 * - Command 작업 시 필요한 검증용 조회 메서드도 포함
 */
public interface ProjectMeetingQueryRepository {

    /**
     * 프로젝트 회의록 상세 조회
     *
     * @param meetingId 조회할 회의록 ID
     * @return 회의록 상세 정보
     */
    Optional<ProjectMeetingVo> findById(Long meetingId);

    /**
     * 프로젝트별 회의록 목록 조회 (페이징)
     *
     * @param projectId 프로젝트 ID
     * @param pageable 페이징 정보
     * @return 회의록 목록
     */
    Page<ProjectMeetingSummaryVo> findByProjectId(Long projectId, Pageable pageable);

    /**
     * 프로젝트 회의록 존재 여부 확인
     *
     * @param meetingId 회의록 ID
     * @return 존재 여부
     */
    boolean existsById(Long meetingId);

    /**
     * 작성자가 해당 회의록의 수정 권한이 있는지 확인
     *
     * @param meetingId 회의록 ID
     * @param requesterId 요청자 ID
     * @return 수정 권한 여부
     */
    boolean hasEditPermission(Long meetingId, Long requesterId);
}