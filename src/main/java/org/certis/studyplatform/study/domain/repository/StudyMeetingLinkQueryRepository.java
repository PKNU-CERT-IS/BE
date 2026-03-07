package org.certis.studyplatform.study.domain.repository;

import org.certis.studyplatform.study.domain.vo.StudyMeetingLinkVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface StudyMeetingLinkQueryRepository {

    /**
     * ID로 링크 조회
     */
    Optional<StudyMeetingLinkVo> findById(Long linkId);

    /**
     * 스터디별 링크 목록 조회
     */
    List<StudyMeetingLinkVo> findByStudyId(Long studyId);

    /**
     * 미팅별 링크 목록 조회
     */
    List<StudyMeetingLinkVo> findByMeetingId(Long meetingId);

    /**
     * 여러 회의록의 링크 목록을 일괄 조회
     */
    List<StudyMeetingLinkVo> findByMeetingIds(List<Long> meetingIds);

    /**
     * 스터디별 링크 목록 페이징 조회
     */
    Page<StudyMeetingLinkVo> findByStudyId(Long studyId, Pageable pageable);

    /**
     * 회원별 링크 목록 조회
     */
    List<StudyMeetingLinkVo> findByMemberId(Long memberId);

    /**
     * 링크 존재 여부 확인
     */
    boolean existsById(Long linkId);

    /**
     * 스터디별 링크 존재 여부 확인
     */
    boolean existsByStudyId(Long studyId);

    /**
     * 스터디별 링크 개수 조회
     */
    int countByStudyId(Long studyId);
}
