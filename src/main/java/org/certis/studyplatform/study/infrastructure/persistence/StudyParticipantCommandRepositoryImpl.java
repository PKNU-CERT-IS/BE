package org.certis.studyplatform.study.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.exception.InfrastructureException;
import org.certis.studyplatform.study.domain.repository.StudyParticipantCommandRepository;
import org.certis.studyplatform.study.domain.vo.StudyParticipantCreatedVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantStatusUpdatedVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantVo;
import org.certis.studyplatform.study.infrastructure.mapper.StudyInfrastructureMapper;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyParticipantEntity;
import org.certis.studyplatform.study.infrastructure.persistence.jpa.StudyParticipantJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Study Participant Command Repository Implementation
 *
 * CQRS Command 측면의 Repository 구현체 (Write 작업)
 * Infrastructure Layer
 * 벌크 연산을 사용하여 성능 최적화
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StudyParticipantCommandRepositoryImpl implements StudyParticipantCommandRepository {

    private final StudyParticipantJpaRepository jpaRepository;
    private final StudyInfrastructureMapper mapper;

    /**
     * 프로젝트 참가 신청 생성
     */
    @Override
    public StudyParticipantCreatedVo save(StudyParticipantVo participantVo) {
        log.debug("Command: Creating participant request - studyId: {}, memberId: {}",
                participantVo.studyId(), participantVo.memberId());

        // VO → Entity 변환
        StudyParticipantEntity entity = mapper.toEntity(participantVo);

        // JPA Repository를 통한 저장
        StudyParticipantEntity savedEntity = jpaRepository.save(entity);

        // Entity → CreatedVo 변환
        StudyParticipantCreatedVo result = mapper.toCreatedVo(savedEntity);

        log.debug("Command: Participant request created successfully - ID: {}", result.id());
        return result;
    }

    /**
     * 프로젝트 참가자 상태 벌크 업데이트 - 조회 없이 바로 업데이트
     */
    @Override
    public StudyParticipantStatusUpdatedVo updateStatus(StudyParticipantVo participantVo) {
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
        StudyParticipantStatusUpdatedVo result = StudyParticipantStatusUpdatedVo.of(
                participantVo.id(),
                participantVo.studyId(),
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
    public void deleteByStudyIdAndMemberId(Long studyId, Long memberId) {
        log.debug("Command: Bulk soft deleting participant - studyId: {}, memberId: {}", studyId, memberId);

        // 벌크 소프트 삭제 실행 (조회 없이)
        int affectedRows = jpaRepository.bulkSoftDeleteByStudyIdAndMemberId(
                studyId,
                memberId,
                OffsetDateTime.now()
        );

        if (affectedRows == 0) {
            log.warn("Command: No participant found to delete - studyId: {}, memberId: {}",
                    studyId, memberId);
            // 필요에 따라 예외 발생 또는 조용히 처리
            throw new InfrastructureException(ExceptionStatus.PROJECT_INFRASTRUCTURE_NOT_FOUND,
                    "삭제할 참가자를 찾을 수 없습니다");
        }

        log.debug("Command: Participant request bulk cancelled successfully - studyId: {}, memberId: {}",
                studyId, memberId);
    }
}