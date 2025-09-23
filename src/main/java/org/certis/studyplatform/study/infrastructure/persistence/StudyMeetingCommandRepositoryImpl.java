package org.certis.studyplatform.study.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.InfrastructureException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.study.domain.repository.StudyMeetingCommandRepository;
import org.certis.studyplatform.study.domain.repository.StudyMeetingQueryRepository;
import org.certis.studyplatform.study.domain.vo.StudyMeetingCreatedVo;
import org.certis.studyplatform.study.domain.vo.StudyMeetingUpdatedVo;
import org.certis.studyplatform.study.domain.vo.StudyMeetingVo;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyMeetingEntity;
import org.certis.studyplatform.study.infrastructure.persistence.jpa.StudyMeetingJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;


/**
 * Study Meeting Command Repository Implementation
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
public class StudyMeetingCommandRepositoryImpl implements StudyMeetingCommandRepository {

    private final StudyMeetingJpaRepository studyMeetingJpaRepository;
    // ✅ CQRS: 검증을 위한 QueryRepository 의존성 추가
    private final StudyMeetingQueryRepository studyMeetingQueryRepository;

    @Override
    public StudyMeetingCreatedVo save(StudyMeetingVo studyMeetingVo) {
        log.info("Infrastructure: Saving study meeting - title: {}", studyMeetingVo.title());

        // VO를 Entity로 변환
        StudyMeetingEntity entity = StudyMeetingEntity.builder()
            .studyId(studyMeetingVo.studyId())
            .memberId(studyMeetingVo.writerId())
            .title(studyMeetingVo.title())
            .content(studyMeetingVo.content())
            .participants(convertParticipantNumberToArray(studyMeetingVo.participantNumber())) // Integer -> String[] 변환
            .build();

        // 데이터베이스에 저장
        StudyMeetingEntity savedEntity = studyMeetingJpaRepository.save(entity);

        StudyMeetingCreatedVo result = StudyMeetingCreatedVo.of(
            savedEntity.getId(),
            savedEntity.getStudyId(),
            savedEntity.getTitle(),
            savedEntity.getContent(),
            studyMeetingVo.participantNumber(),
            savedEntity.getMemberId(),
            savedEntity.getCreatedAt()
        );

        log.info("Infrastructure: Study meeting saved successfully - ID: {}", savedEntity.getId());
        return result;
    }

    @Override
    public StudyMeetingUpdatedVo update(StudyMeetingVo studyMeetingVo) {
        log.info("Infrastructure: Bulk updating study meeting - ID: {}", studyMeetingVo.id());

        // ✅ CQRS: QueryRepository를 통한 검증
        if (!studyMeetingQueryRepository.existsById(studyMeetingVo.id())) {
            throw new InfrastructureException(ExceptionStatus.STUDY_INFRASTRUCTURE_NOT_FOUND, "회의록을 찾을 수 없습니다");
        }

        if (!studyMeetingQueryRepository.hasEditPermission(studyMeetingVo.id(), studyMeetingVo.writerId())) {
            throw new InfrastructureException(ExceptionStatus.STUDY_INFRASTRUCTURE_ACCESS_DENIED, "회의록 수정 권한이 없습니다");
        }

        // 벌크 업데이트 실행 (조회 없이)
        int affectedRows = studyMeetingJpaRepository.bulkUpdateMeeting(
                studyMeetingVo.id(),
                studyMeetingVo.title(),
                studyMeetingVo.content(),
                convertParticipantNumberToArray(studyMeetingVo.participantNumber()),
                OffsetDateTime.now()
        );

        if (affectedRows == 0) {
            throw new InfrastructureException(ExceptionStatus.STUDY_INFRASTRUCTURE_NOT_FOUND,
                    "회의록 업데이트 실패 - 동시성 문제 또는 존재하지 않음");
        }

        // 업데이트된 결과는 QueryRepository로 조회해서 반환
        // 또는 VO 정보를 기반으로 결과 생성
        StudyMeetingUpdatedVo result = StudyMeetingUpdatedVo.of(
                studyMeetingVo.id(),
                studyMeetingVo.title(),
                studyMeetingVo.content(),
                studyMeetingVo.participantNumber(),
                OffsetDateTime.now()
        );

        log.info("Infrastructure: Study meeting bulk updated successfully - ID: {}", studyMeetingVo.id());
        return result;
    }

    /**
     * 벌크 삭제로 변경 - 조회 없이 바로 소프트 삭제
     */
    @Override
    public void deleteByIdWithPermission(Long meetingId, Long requesterId) {
        log.info("Infrastructure: Bulk deleting study meeting with permission check - meetingId: {}, requesterId: {}", meetingId, requesterId);

        // ✅ CQRS: QueryRepository를 통한 검증
        if (!studyMeetingQueryRepository.existsById(meetingId)) {
            throw new InfrastructureException(ExceptionStatus.STUDY_INFRASTRUCTURE_NOT_FOUND);
        }

        if (!studyMeetingQueryRepository.hasEditPermission(meetingId, requesterId)) {
            throw new InfrastructureException(ExceptionStatus.STUDY_INFRASTRUCTURE_PERMISSION_DENINED);
        }

        // 벌크 소프트 삭제 실행 (조회 없이)
        int affectedRows = studyMeetingJpaRepository.bulkSoftDeleteById(meetingId, OffsetDateTime.now());

        if (affectedRows == 0) {
            throw new InfrastructureException(ExceptionStatus.STUDY_INFRASTRUCTURE_NOT_FOUND,
                    "회의록 삭제 실패 - 동시성 문제 또는 이미 삭제됨");
        }

        log.info("Infrastructure: Study meeting bulk deleted successfully - ID: {}", meetingId);
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
     * List<Long> participantIds를 String[] participants로 변환
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
} 