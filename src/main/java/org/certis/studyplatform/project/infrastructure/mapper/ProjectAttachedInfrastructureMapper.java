package org.certis.studyplatform.project.infrastructure.mapper;

import org.certis.studyplatform.project.domain.vo.ProjectAttachedVo;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectAttachedEntity;
import org.certis.studyplatform.project.presentation.dto.response.ProjectAttachedResponseDto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Project Attached Infrastructure Mapper
 *
 * ProjectAttachedEntity와 VO/DTO 간의 변환을 담당하는 매퍼
 * Infrastructure Layer
 */
@Component
public class ProjectAttachedInfrastructureMapper {

    /**
     * ProjectAttachedEntity를 ProjectAttachedVo로 변환 (Query Service용)
     */
    public ProjectAttachedVo toVo(ProjectAttachedEntity entity) {
        if (entity == null) {
            return null;
        }

        return ProjectAttachedVo.of(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getSize(),
                entity.getAttachedUrl()
        );
    }

    /**
     * ProjectAttachedEntity 리스트를 ProjectAttachedVo 리스트로 변환 (Query Service용)
     */
    public List<ProjectAttachedVo> toVoList(List<ProjectAttachedEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(this::toVo)
                .toList();
    }

    /**
     * ProjectAttachedEntity를 ProjectAttachedResponseDto로 변환 (Legacy 호환용)
     */
    public ProjectAttachedResponseDto toResponseDto(ProjectAttachedEntity entity) {
        if (entity == null) {
            return null;
        }

        return ProjectAttachedResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .size(entity.getSize())
                .attachedUrl(entity.getAttachedUrl())
                .build();
    }

    /**
     * ProjectAttachedEntity 리스트를 ProjectAttachedResponseDto 리스트로 변환 (Legacy 호환용)
     */
    public List<ProjectAttachedResponseDto> toResponseDtoList(List<ProjectAttachedEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(this::toResponseDto)
                .toList();
    }
} 