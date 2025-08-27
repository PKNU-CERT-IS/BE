package org.certis.studyplatform.project.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.domain.repository.ProjectMeetingLinkCommandRepository;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingLinkVo;
import org.certis.studyplatform.project.infrastructure.persistence.jpa.ProjectMeetingLinkJpaRepository;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectMeetingLinkEntity;
import org.certis.studyplatform.project.infrastructure.mapper.ProjectMeetingLinkInfrastructureMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * ProjectMeetingLink Command Repository Implementation
 *
 * ✅ CQRS 엄격 적용: Command는 쓰기 작업만 담당 (Create, Update, Delete)
 * ✅ 모든 조회는 QueryRepository로 위임
 * Infrastructure Layer
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectMeetingLinkCommandRepositoryImpl implements ProjectMeetingLinkCommandRepository {

    private final ProjectMeetingLinkJpaRepository jpaRepository;
    private final ProjectMeetingLinkInfrastructureMapper mapper;

    /**
     * 새로운 프로젝트 회의록 링크 생성
     * ✅ 순수 쓰기 작업: 조회 없이 바로 저장
     */
    @Override
    public void save(ProjectMeetingLinkVo linkVo) {
        log.info("LinkCommand: Creating project meeting link - projectId: {}, URL: {}",
                linkVo.projectId(), linkVo.attachedUrl());

        // VO → Entity 변환
        ProjectMeetingLinkEntity entity = mapper.toEntity(linkVo);

        // JPA Repository를 통한 저장 (순수 쓰기 작업)
        ProjectMeetingLinkEntity savedEntity = jpaRepository.save(entity);

        log.info("LinkCommand: Project meeting link created successfully - ID: {}", savedEntity.getId());
    }

    /**
     * 프로젝트별 모든 링크 소프트 삭제 - 개선된 버전
     */
    @Override
    public void deleteByProjectId(Long projectId) {
        log.info("LinkCommand: Bulk soft deleting project meeting links by projectId - {}", projectId);

        // 개선된 벌크 소프트 삭제 (affectedRows 반환)
        int affectedRows = jpaRepository.bulkSoftDeleteByProjectId(projectId, OffsetDateTime.now());

        log.info("LinkCommand: {} project meeting links bulk deleted successfully - projectId: {}",
                affectedRows, projectId);
    }

    /**
     * 특정 링크 소프트 삭제 - 개선된 버전
     */
    @Override
    public void deleteById(Long linkId) {
        log.info("LinkCommand: Bulk soft deleting project meeting link by ID - {}", linkId);

        // 개선된 벌크 소프트 삭제
        int affectedRows = jpaRepository.bulkSoftDeleteById(linkId, OffsetDateTime.now());

        if (affectedRows == 0) {
            log.warn("LinkCommand: No link found to delete - ID: {}", linkId);
            // 필요에 따라 예외 발생 또는 조용히 처리
        } else {
            log.info("LinkCommand: Project meeting link bulk deleted successfully - ID: {}", linkId);
        }
    }

    /**
     * 회원별 모든 링크 소프트 삭제 - 개선된 버전
     */
    @Override
    public void deleteByMemberId(Long memberId) {
        log.info("LinkCommand: Bulk soft deleting project meeting links by memberId - {}", memberId);

        int affectedRows = jpaRepository.bulkSoftDeleteByMemberId(memberId, OffsetDateTime.now());

        log.info("LinkCommand: {} project meeting links bulk deleted successfully - memberId: {}",
                affectedRows, memberId);
    }
}