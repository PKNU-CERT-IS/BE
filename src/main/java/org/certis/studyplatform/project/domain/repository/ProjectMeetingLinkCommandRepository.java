package org.certis.studyplatform.project.domain.repository;

import org.certis.studyplatform.project.domain.vo.ProjectMeetingCreatedVo;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingLinkVo;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingVo;

public interface ProjectMeetingLinkCommandRepository {

    /**
     * 프로젝트 회의록 링크 저장
     */
    void save(ProjectMeetingLinkVo linkVo);

    /**
     * 프로젝트별 모든 링크 삭제
     */
    void deleteByProjectId(Long projectId);

    /**
     * 특정 링크 삭제
     */
    void deleteById(Long linkId);

    /**
     * 회원별 모든 링크 삭제
     */
    void deleteByMemberId(Long memberId);
}
