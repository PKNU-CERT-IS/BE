package org.certis.studyplatform.project.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.exception.InfrastructureException;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantCommandRepository;
import org.certis.studyplatform.project.domain.vo.*;
import org.certis.studyplatform.project.infrastructure.mapper.ProjectInfrastructureMapper;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectParticipantEntity;
import org.certis.studyplatform.project.infrastructure.persistence.jpa.ProjectParticipantJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Project Participant Command Repository Implementation
 *
 * CQRS Command 측면의 Repository 구현체 (Write 작업)
 * Infrastructure Layer
 * 벌크 연산을 사용하여 성능 최적화
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectParticipantCommandRepositoryImpl implements ProjectParticipantCommandRepository {

    private final ProjectParticipantJpaRepository jpaRepository;
    private final ProjectInfrastructureMapper mapper;

    /**
     * 프로젝트 참가 신청 생성
     */
    @Override
    public ProjectParticipantCreatedVo save(ProjectParticipantVo participantVo) {
        log.debug("Command: Creating participant request - projectId: {}, memberId: {}",
                participantVo.projectId(), participantVo.memberId());

        // VO → Entity 변환
        ProjectParticipantEntity entity = mapper.toEntity(participantVo);

        // JPA Repository를 통한 저장
        ProjectParticipantEntity savedEntity = jpaRepository.save(entity);

        // Entity → CreatedVo 변환
        ProjectParticipantCreatedVo result = mapper.toCreatedVo(savedEntity);

        log.debug("Command: Participant request created successfully - ID: {}", result.id());
        return result;
    }

    /**
     * 프로젝트 참가자 상태 벌크 업데이트 - 조회 없이 바로 업데이트
     */
    @Override
    public ProjectParticipantStatusUpdatedVo updateStatus(ProjectParticipantVo participantVo) {
        log.debug("Command: Bulk updating participant status - ID: {}, status: {}",
                participantVo.id(), participantVo.status());

        // 벌크 업데이트 실행 (조회 없이)
        int affectedRows = jpaRepository.bulkUpdateStatus(
                participantVo.id(),
                participantVo.status(),
                OffsetDateTime.now()
        );

        if (affectedRows == 0) {
            throw new InfrastructureException(ExceptionStatus.PROJECT_INFRASTRUCTURE_NOT_FOUND,
                    "참가자를 찾을 수 없습니다: " + participantVo.id());
        }

        // 결과 VO 생성 (이전 상태는 알 수 없으므로 null 또는 기본값 사용)
        ProjectParticipantStatusUpdatedVo result = ProjectParticipantStatusUpdatedVo.of(
                participantVo.id(),
                participantVo.projectId(),
                participantVo.memberId(),
                null,
                null);

        log.debug("Command: Participant status bulk updated successfully - ID: {}, status: {}",
                participantVo.id(), participantVo.status());
        return result;
    }

    /**
     * 프로젝트 참가 신청 벌크 취소 (소프트 삭제)
     */
    @Override
    public void deleteByProjectIdAndMemberId(Long projectId, Long memberId) {
        log.debug("Command: Bulk soft deleting participant - projectId: {}, memberId: {}", projectId, memberId);

        // 벌크 소프트 삭제 실행 (조회 없이)
        int affectedRows = jpaRepository.bulkSoftDeleteByProjectIdAndMemberId(
                projectId,
                memberId,
                OffsetDateTime.now()
        );

        if (affectedRows == 0) {
            log.warn("Command: No participant found to delete - projectId: {}, memberId: {}",
                    projectId, memberId);
            // 필요에 따라 예외 발생 또는 조용히 처리
            throw new InfrastructureException(ExceptionStatus.PROJECT_INFRASTRUCTURE_NOT_FOUND,
                    "삭제할 참가자를 찾을 수 없습니다");
        }

        log.debug("Command: Participant request bulk cancelled successfully - projectId: {}, memberId: {}",
                projectId, memberId);
    }
}