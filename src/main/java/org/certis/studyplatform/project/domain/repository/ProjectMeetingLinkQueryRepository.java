package org.certis.studyplatform.project.domain.repository;

import org.certis.studyplatform.project.domain.vo.ProjectMeetingLinkVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProjectMeetingLinkQueryRepository {

    /**
     * ID로 링크 조회
     */
    Optional<ProjectMeetingLinkVo> findById(Long linkId);

    /**
     * 회의록별 링크 목록 조회
     */
    List<ProjectMeetingLinkVo> findByMeetingId(Long meetingId);

    /**
     * 회의록별 링크 목록 페이징 조회
     */
    Page<ProjectMeetingLinkVo> findByMeetingId(Long meetingId, Pageable pageable);

    /**
     * 회원별 링크 목록 조회
     */
    List<ProjectMeetingLinkVo> findByMemberId(Long memberId);

    /**
     * 링크 존재 여부 확인
     */
    boolean existsById(Long linkId);

    /**
     * 회의록별 링크 존재 여부 확인
     */
    boolean existsByMeetingId(Long meetingId);

    /**
     * 회의록별 링크 개수 조회
     */
    int countByMeetingId(Long meetingId);
}
