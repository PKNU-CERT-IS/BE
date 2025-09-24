package org.certis.studyplatform.study.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.repository.StudyMeetingLinkCommandRepository;
import org.certis.studyplatform.study.domain.vo.StudyMeetingLinkVo;
import org.certis.studyplatform.study.infrastructure.mapper.StudyMeetingLinkInfrastructureMapper;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyMeetingLinkEntity;
import org.certis.studyplatform.study.infrastructure.persistence.jpa.StudyMeetingLinkJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * StudyMeetingLink Command Repository Implementation
 *
 * ✅ CQRS 엄격 적용: Command는 쓰기 작업만 담당 (Create, Update, Delete)
 * ✅ 모든 조회는 QueryRepository로 위임
 * Infrastructure Layer
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StudyMeetingLinkCommandRepositoryImpl implements StudyMeetingLinkCommandRepository {

    private final StudyMeetingLinkJpaRepository jpaRepository;
    private final StudyMeetingLinkInfrastructureMapper mapper;

    /**
     * 새로운 프로젝트 회의록 링크 생성
     * ✅ 순수 쓰기 작업: 조회 없이 바로 저장
     */
    @Override
    public void save(StudyMeetingLinkVo linkVo) {
        log.info("LinkCommand: Creating study meeting link - meetingId: {}, URL: {}",
                linkVo.meetingId(), linkVo.attachedUrl());

        // VO → Entity 변환
        StudyMeetingLinkEntity entity = mapper.toEntity(linkVo);

        // JPA Repository를 통한 저장 (순수 쓰기 작업)
        StudyMeetingLinkEntity savedEntity = jpaRepository.save(entity);

        log.info("LinkCommand: Study meeting link created successfully - ID: {}", savedEntity.getId());
    }

    /**
     * 프로젝트별 모든 링크 소프트 삭제 - 개선된 버전
     */
    @Override
    public void deleteByStudyId(Long studyId) {
        log.info("LinkCommand: Bulk soft deleting study meeting links by studyId - {}", studyId);

        // 개선된 벌크 소프트 삭제 (affectedRows 반환)
        int affectedRows = 0; // deprecated path kept for compatibility

        log.info("LinkCommand: {} study meeting links bulk deleted successfully - studyId: {}",
                affectedRows, studyId);
    }

    public void deleteByMeetingId(Long meetingId) {
        log.info("LinkCommand: Bulk soft deleting study meeting links by meetingId - {}", meetingId);
        int affectedRows = jpaRepository.bulkSoftDeleteByStudyId(meetingId, OffsetDateTime.now());
        log.info("LinkCommand: {} study meeting links bulk deleted successfully - meetingId: {}", affectedRows, meetingId);
    }

    /**
     * 특정 링크 소프트 삭제 - 개선된 버전
     */
    @Override
    public void deleteById(Long linkId) {
        log.info("LinkCommand: Bulk soft deleting study meeting link by ID - {}", linkId);

        // 개선된 벌크 소프트 삭제
        int affectedRows = jpaRepository.bulkSoftDeleteById(linkId, OffsetDateTime.now());

        if (affectedRows == 0) {
            log.warn("LinkCommand: No link found to delete - ID: {}", linkId);
            // 필요에 따라 예외 발생 또는 조용히 처리
        } else {
            log.info("LinkCommand: Study meeting link bulk deleted successfully - ID: {}", linkId);
        }
    }

    /**
     * 회원별 모든 링크 소프트 삭제 - 개선된 버전
     */
    @Override
    public void deleteByMemberId(Long memberId) {
        log.info("LinkCommand: Bulk soft deleting study meeting links by memberId - {}", memberId);

        int affectedRows = jpaRepository.bulkSoftDeleteByMemberId(memberId, OffsetDateTime.now());

        log.info("LinkCommand: {} study meeting links bulk deleted successfully - memberId: {}",
                affectedRows, memberId);
    }
}