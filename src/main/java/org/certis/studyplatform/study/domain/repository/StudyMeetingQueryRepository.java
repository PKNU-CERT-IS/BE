package org.certis.studyplatform.study.domain.repository;

import org.certis.studyplatform.study.domain.vo.StudyMeetingSummaryVo;
import org.certis.studyplatform.study.domain.vo.StudyMeetingVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Study Meeting Query Repository Interface
 *
 * CQRS Query 측면의 Repository (Read 작업)
 * Domain Layer의 인터페이스
 * Infrastructure Layer에서 QueryDSL 또는 JOOQ로 구현
 *
 * ✅ CQRS 패턴 준수:
 * - 모든 조회 관련 메서드 포함
 * - Command 작업 시 필요한 검증용 조회 메서드도 포함
 */
public interface StudyMeetingQueryRepository {

    /**
     * 스터디 회의록 상세 조회
     *
     * @param meetingId 조회할 회의록 ID
     * @return 회의록 상세 정보
     */
    Optional<StudyMeetingVo> findById(Long meetingId);

    /**
     * 스터디별 회의록 목록 조회 (페이징)
     *
     * @param studyId 스터디 ID
     * @param pageable 페이징 정보
     * @return 회의록 목록
     */
    Page<StudyMeetingSummaryVo> findByStudyId(Long studyId, Pageable pageable);

    /**
     * 스터디 회의록 존재 여부 확인
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