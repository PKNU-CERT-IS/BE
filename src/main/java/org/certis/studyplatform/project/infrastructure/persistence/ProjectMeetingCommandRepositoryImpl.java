package org.certis.studyplatform.project.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.project.domain.repository.ProjectMeetingCommandRepository;
import org.certis.studyplatform.project.domain.repository.ProjectMeetingQueryRepository;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingCreatedVo;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingUpdatedVo;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingVo;
import org.certis.studyplatform.project.domain.vo.ProjectParticipantSummaryVo;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectMeetingEntity;
import org.certis.studyplatform.project.infrastructure.persistence.jpa.ProjectMeetingJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * Project Meeting Command Repository Implementation
 *
 * Infrastructure Layer의 실제 데이터베이스 Repository 구현체
 * JPA Entity와 매핑하여 실제 데이터베이스 처리
 * 
 * ✅ CQRS 패턴 준수:
 * - 쓰기 작업(Create, Update, Delete)만 담당
 * - 검증 작업은 QueryRepository를 통해 처리
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ProjectMeetingCommandRepositoryImpl implements ProjectMeetingCommandRepository {

    private final ProjectMeetingJpaRepository projectMeetingJpaRepository;
    // ✅ CQRS: 검증을 위한 QueryRepository 의존성 추가
    private final ProjectMeetingQueryRepository projectMeetingQueryRepository;
    private final ProjectParticipantQueryRepository projectParticipantQueryRepository;

    @Override
    public ProjectMeetingCreatedVo save(ProjectMeetingVo projectMeetingVo) {
        log.info("Infrastructure: Saving project meeting - title: {}", projectMeetingVo.title());

        // VO를 Entity로 변환
        ProjectMeetingEntity entity = ProjectMeetingEntity.builder()
            .projectId(projectMeetingVo.projectId())
            .memberId(projectMeetingVo.writerId())
            .title(projectMeetingVo.title())
            .content(projectMeetingVo.content())
            .participants(toParticipantsArray(
                    projectMeetingVo.projectId(),
                    projectMeetingVo.writerId(),
                    projectMeetingVo.participantNumber()
            ))
            .build();

        // 데이터베이스에 저장
        ProjectMeetingEntity savedEntity = projectMeetingJpaRepository.save(entity);

        ProjectMeetingCreatedVo result = ProjectMeetingCreatedVo.of(
            savedEntity.getId(),
            savedEntity.getProjectId(),
            savedEntity.getTitle(),
            savedEntity.getContent(),
            projectMeetingVo.participantNumber(),
            savedEntity.getMemberId(),
            savedEntity.getCreatedAt()
        );

        log.info("Infrastructure: Project meeting saved successfully - ID: {}", savedEntity.getId());
        return result;
    }

    @Override
    public ProjectMeetingUpdatedVo update(ProjectMeetingVo projectMeetingVo) {
        log.info("Infrastructure: Bulk updating project meeting - ID: {}", projectMeetingVo.id());

        // ✅ CQRS: QueryRepository를 통한 검증
        if (!projectMeetingQueryRepository.existsById(projectMeetingVo.id())) {
            throw new DomainException(ExceptionStatus.PROJECT_INFRASTRUCTURE_NOT_FOUND, "회의록을 찾을 수 없습니다");
        }

        if (!projectMeetingQueryRepository.hasEditPermission(projectMeetingVo.id(), projectMeetingVo.writerId())) {
            throw new DomainException(ExceptionStatus.PRESENTATION_AUTH_ACCESS_DENIED, "회의록 수정 권한이 없습니다");
        }

        // 벌크 업데이트 실행 (조회 없이)
        int affectedRows = projectMeetingJpaRepository.bulkUpdateMeeting(
                projectMeetingVo.id(),
                projectMeetingVo.title(),
                projectMeetingVo.content(),
                toParticipantsArray(
                        projectMeetingVo.projectId(),
                        projectMeetingVo.writerId(),
                        projectMeetingVo.participantNumber()
                ),
                OffsetDateTime.now()
        );

        if (affectedRows == 0) {
            throw new DomainException(ExceptionStatus.PROJECT_INFRASTRUCTURE_NOT_FOUND,
                    "회의록 업데이트 실패 - 동시성 문제 또는 존재하지 않음");
        }

        // 업데이트된 결과는 QueryRepository로 조회해서 반환
        // 또는 VO 정보를 기반으로 결과 생성
        ProjectMeetingUpdatedVo result = ProjectMeetingUpdatedVo.of(
                projectMeetingVo.id(),
                projectMeetingVo.title(),
                projectMeetingVo.content(),
                projectMeetingVo.participantNumber(),
                OffsetDateTime.now()
        );

        log.info("Infrastructure: Project meeting bulk updated successfully - ID: {}", projectMeetingVo.id());
        return result;
    }

    /**
     * 벌크 삭제로 변경 - 조회 없이 바로 소프트 삭제
     */
    @Override
    public void deleteByIdWithPermission(Long meetingId, Long requesterId) {
        log.info("Infrastructure: Bulk deleting project meeting with permission check - meetingId: {}, requesterId: {}", meetingId, requesterId);

        // ✅ CQRS: QueryRepository를 통한 검증
        if (!projectMeetingQueryRepository.existsById(meetingId)) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_NOT_FOUND, "회의록을 찾을 수 없습니다");
        }

        if (!projectMeetingQueryRepository.hasEditPermission(meetingId, requesterId)) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_PERMISSION, "회의록 삭제 권한이 없습니다");
        }

        // 벌크 소프트 삭제 실행 (조회 없이)
        int affectedRows = projectMeetingJpaRepository.bulkSoftDeleteById(meetingId, OffsetDateTime.now());

        if (affectedRows == 0) {
            throw new DomainException(ExceptionStatus.PROJECT_INFRASTRUCTURE_NOT_FOUND,
                    "회의록 삭제 실패 - 동시성 문제 또는 이미 삭제됨");
        }

        log.info("Infrastructure: Project meeting bulk deleted successfully - ID: {}", meetingId);
    }

    /**
     * 회의 참가자 배열 생성
     * - 프로젝트의 APPROVED 참가자 목록을 조회하여 memberId 배열 구성
     * - 작성자(writerId)가 목록에 없으면 포함
     * - participantNumber가 지정되면 해당 수로 상한
     */
    private Long[] toParticipantsArray(Long projectId, Long writerId, Integer participantNumber) {
        try {
            List<ProjectParticipantSummaryVo> approved = projectParticipantQueryRepository.findAllApprovedByProjectId(projectId);

            List<Long> participantIds = new ArrayList<>();
            for (ProjectParticipantSummaryVo vo : approved) {
                Long memberId = vo.memberId();
                if (memberId != null && !participantIds.contains(memberId)) {
                    participantIds.add(memberId);
                }
            }

            if (writerId != null && !participantIds.contains(writerId)) {
                participantIds.add(0, writerId);
            }

            if (participantNumber != null && participantNumber > 0 && participantIds.size() > participantNumber) {
                participantIds = participantIds.subList(0, participantNumber);
            }

            return participantIds.toArray(new Long[0]);
        } catch (Exception e) {
            log.warn("Infrastructure: Failed to build participants array for projectId={}, falling back to writer only. error={}",
                    projectId, e.getMessage(), e);
            return writerId != null ? new Long[]{writerId} : new Long[0];
        }
    }
} 