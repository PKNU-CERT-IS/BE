package org.certis.studyplatform.study.domain.repository;

import org.certis.studyplatform.study.domain.vo.StudyMeetingLinkVo;

public interface StudyMeetingLinkCommandRepository {

    /**
     * 스터디 회의록 링크 저장
     */
    void save(StudyMeetingLinkVo linkVo);

    /**
     * 스터디별 모든 링크 삭제
     */
    void deleteByStudyId(Long studyId);

    /**
     * 회의록별 모든 링크 삭제
     */
    void deleteByMeetingId(Long meetingId);

    /**
     * 특정 링크 삭제
     */
    void deleteById(Long linkId);

    /**
     * 회원별 모든 링크 삭제
     */
    void deleteByMemberId(Long memberId);
}
