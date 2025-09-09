package org.certis.studyplatform.study.infrastructure.mapper;

import org.certis.studyplatform.study.domain.vo.StudyAttachedVo;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyAttachedEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Study Attached Infrastructure Mapper
 *
 * StudyAttachedEntity와 VO/DTO 간의 변환을 담당하는 매퍼
 * Infrastructure Layer
 */
@Component
public class StudyAttachedInfrastructureMapper {

    /**
     * StudyAttachedEntity를 StudyAttachedVo로 변환 (Query Service용)
     */
    public StudyAttachedVo toVo(StudyAttachedEntity entity) {
        if (entity == null) {
            return null;
        }

        return StudyAttachedVo.of(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getSize(),
                entity.getAttachedUrl()
        );
    }

    /**
     * StudyAttachedEntity 리스트를 StudyAttachedVo 리스트로 변환 (Query Service용)
     */
    public List<StudyAttachedVo> toVoList(List<StudyAttachedEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(this::toVo)
                .toList();
    }
} 