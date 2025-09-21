package org.certis.studyplatform.project.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.project.domain.repository.ProjectMeetingCommandRepository;
import org.certis.studyplatform.project.domain.repository.ProjectMeetingQueryRepository;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingCreatedVo;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingUpdatedVo;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingVo;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectMeetingEntity;
import org.certis.studyplatform.project.infrastructure.persistence.jpa.ProjectMeetingJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;


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

    @Override
    public ProjectMeetingCreatedVo save(ProjectMeetingVo projectMeetingVo) {
        log.info("Infrastructure: Saving project meeting - title: {}", projectMeetingVo.title());

        // VO를 Entity로 변환
        ProjectMeetingEntity entity = ProjectMeetingEntity.builder()
            .projectId(projectMeetingVo.projectId())
            .memberId(projectMeetingVo.writerId())
            .title(projectMeetingVo.title())
            .content(projectMeetingVo.content())
            .participants(convertParticipantNumberToArray(projectMeetingVo.participantNumber())) // Integer -> String[] 변환
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
                convertParticipantNumberToArray(projectMeetingVo.participantNumber()),
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
     * List<Long> participantIds를 콤마로 구분된 문자열로 변환 (벌크 업데이트용)
     */
    private String convertParticipantIdsToString(java.util.List<Long> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            return "";
        }

        return participantIds.stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
    }


    /**
     * Integer participantNumber를 String[] participants로 변환
     * 임시로 단순 변환 처리 (실제로는 Member ID를 이름으로 변환하는 로직 필요)
     */
    private Long[] convertParticipantNumberToArray(Integer participantNumber) {
        if (participantNumber == null || participantNumber <= 0) {
            return new Long[0];
        }
        
        // participantNumber만큼의 더미 ID 배열 생성 (실제 구현에서는 다른 방식 사용 가능)
        Long[] result = new Long[participantNumber];
        for (int i = 0; i < participantNumber; i++) {
            result[i] = (long) (i + 1); // 더미 ID
        }
        return result;
    }

    /**
     * String[] participants를 List<Long> participantIds로 변환
     * 임시로 단순 변환 처리 (실제로는 participants가 이름 문자열이므로 별도 매핑 필요)
     */
    private java.util.List<Long> convertParticipantsToIds(String[] participants) {
        if (participants == null) {
            return java.util.List.of();
        }
        
        // TODO: 실제로는 participant 이름을 Member ID로 변환하는 로직 필요
        // 임시로 문자열을 Long으로 변환 시도
        return java.util.Arrays.stream(participants)
            .map(s -> {
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException e) {
                    return 0L; // 임시 처리
                }
            })
            .toList();
    }
} 