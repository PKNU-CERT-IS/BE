package org.certis.studyplatform.study.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.repository.StudyAttachedCommandRepository;
import org.certis.studyplatform.study.domain.vo.StudyAttachedVo;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyAttachedEntity;
import org.certis.studyplatform.study.infrastructure.persistence.jpa.StudyAttachedJpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Study Attached Command Repository Implementation
 *
 * Infrastructure Layer
 * JPA를 사용한 실제 데이터 접근 구현
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class StudyAttachedCommandRepositoryImpl implements StudyAttachedCommandRepository {

    private final StudyAttachedJpaRepository studyAttachedJpaRepository;

    @Override
    public void save(Long studyId, StudyAttachedVo attachment) {
        log.debug("Infrastructure: Saving study attachment for study ID: {}, attachment: {}", studyId, attachment.name());
        
        StudyAttachedEntity entity = StudyAttachedEntity.builder()
                .studyId(studyId)
                .memberId(0L) // TODO: memberId should be passed from command
                .attachedUrl(attachment.attachedUrl())
                .name(attachment.name())
                .type(attachment.type())
                .size(attachment.size())
                .build();
        studyAttachedJpaRepository.save(entity);
        
        log.debug("Infrastructure: Study attachment saved successfully");
    }

    @Override
    public void deleteByStudyId(Long studyId) {
        log.debug("Infrastructure: Deleting study attachments for study ID: {}", studyId);
        
        studyAttachedJpaRepository.deleteByStudyId(studyId);
        
        log.debug("Infrastructure: Study attachments deleted successfully for study ID: {}", studyId);
    }

}
