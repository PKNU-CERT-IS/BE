package org.certis.studyplatform.project.infrastructure.mapper;

import org.certis.studyplatform.project.domain.vo.*;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectEntity;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectParticipantEntity;
import org.jooq.Record;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.certis.generated.jooq.Tables.*;

/**
 * Project Infrastructure Mapper
 *
 * Clean Architecture Infrastructure Layer
 * Entity ↔ VO 변환 및 jOOQ Record → VO 변환
 *
 * ✅ 실제 사용되는 매핑 메서드만 유지
 * ✅ ProjectVo 내부 검증 로직과 연동
 */
@Component
public class ProjectInfrastructureMapper {

    // ================================================================
    // COMMAND REPOSITORY 매핑 (Entity ↔ VO)
    // ================================================================

    /**
     * ProjectVo를 ProjectEntity로 변환 (저장용)
     */
    public ProjectEntity toEntity(ProjectVo vo) {
        if (vo == null) {
            return null;
        }

        return ProjectEntity.builder()
                .id(vo.id())
                .memberId(vo.creatorId()) // creatorId → memberId
                .title(vo.title())
                .description(vo.description())
                .content(vo.content()) // content 필드 사용
                .category(vo.category())
                .subcategory(vo.subCategory()) // subCategory → subcategory
                .maxParticipantsNumber(vo.maxParticipants()) // maxParticipants → maxParticipantsNumber
                .githubUrl(vo.githubUrl())
                .externalUrl(vo.externalUrl())
                .thumbnailUrl(vo.thumbnailUrl())
                .startedAt(vo.startDate()) // startDate → startedAt
                .endedAt(vo.endDate()) // endDate → endedAt
                .build();
    }

    /**
     * ProjectEntity를 ProjectVo로 변환 (조회용)
     */
    public ProjectVo toVo(ProjectEntity entity) {
        if (entity == null) {
            return null;
        }

        return ProjectVo.of(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getContent(),
                entity.getCategory(),
                entity.getSubcategory(), // subcategory → subCategory
                entity.getStartedAt(), // startedAt → startDate
                entity.getEndedAt(), // endedAt → endDate
                entity.getMemberId(), // memberId → creatorId
                null, // creatorName은 별도 조회 필요
                entity.getGithubUrl(),
                entity.getExternalUrl(),
                entity.getThumbnailUrl(),
                entity.getMaxParticipantsNumber(), // maxParticipantsNumber → maxParticipants
                0 // currentParticipants는 별도 계산 필요
        );
    }

    // ================================================================
    // QUERY REPOSITORY 매핑 (jOOQ Record → VO)
    // ================================================================

    /**
     * jOOQ Record를 ProjectVo로 변환 (상세 조회용)
     * QueryRepositoryImpl에서 사용
     */
    public ProjectVo toProjectVoFromRecord(Record record) {
        if (record == null) {
            return null;
        }

        return ProjectVo.of(
                record.get(PROJECT.ID),
                record.get(PROJECT.TITLE),
                record.get(PROJECT.DESCRIPTION),
                record.get(PROJECT.CONTENT),
                record.get(PROJECT.CATEGORY),
                record.get(PROJECT.SUBCATEGORY),
                record.get(PROJECT.STARTED_AT),
                record.get(PROJECT.ENDED_AT),
                record.get(PROJECT.MEMBER_ID),
                record.get(MEMBER.NAME), // JOIN된 creatorName
                record.get(PROJECT.GITHUB_URL), // githubUrl은 별도 관리
                record.get(PROJECT.EXTERNAL_URL), // externalUrl은 별도 관리
                record.get(PROJECT.THUMBNAIL_URL),
                record.get(PROJECT.MAX_PARTICIPANTS_NUMBER),
                record.get("current_participants", Integer.class) // 서브쿼리 결과
        );
    }

    /**
     * jOOQ Record를 ProjectSummaryVo로 변환 (목록 조회용)
     * QueryRepositoryImpl에서 사용
     */
    public ProjectSummaryVo toProjectSummaryVoFromRecord(Record record) {
        if (record == null) {
            return null;
        }

        // 동적 상태 계산
        String status = calculateStatusString(
                record.get(PROJECT.STARTED_AT),
                record.get(PROJECT.ENDED_AT)
        );

        // 참여 가능 여부 계산
        boolean isParticipantable = "모집중".equals(status);

        return ProjectSummaryVo.of(
                record.get(PROJECT.ID),
                record.get(PROJECT.TITLE),
                record.get(PROJECT.DESCRIPTION),
                buildCategoryList(record.get(PROJECT.CATEGORY), record.get(PROJECT.SUBCATEGORY)),
                record.get(PROJECT.STARTED_AT),
                record.get(PROJECT.ENDED_AT),
                record.get(MEMBER.NAME), // JOIN된 creatorName
                "LEADER", // 기본 역할
                isParticipantable,
                null, // githubUrl은 별도 관리
                null  // externalUrl은 별도 관리
        );
    }

    /**
     * 단순 VO 변환 (ProjectQueryRepositoryImpl.findById용)
     */
    public ProjectVo toProjectVo(Record record) {
        return toProjectVoFromRecord(record); // 동일한 로직 재사용
    }

    // ================================================================
    // PRIVATE HELPER METHODS
    // ================================================================

    /**
     * PostgreSQL 배열을 List<String>으로 변환
     */
    private List<String> convertArrayToList(Object skillsArray) {
        if (skillsArray == null) {
            return new ArrayList<>();
        }

        if (skillsArray instanceof String[]) {
            return Arrays.asList((String[]) skillsArray);
        }

        if (skillsArray instanceof Object[]) {
            Object[] objects = (Object[]) skillsArray;
            List<String> skills = new ArrayList<>();
            for (Object obj : objects) {
                if (obj != null) {
                    skills.add(obj.toString());
                }
            }
            return skills;
        }

        return new ArrayList<>();
    }

    /**
     * category, subCategory를 List로 변환 (ProjectSummaryVo용)
     */
    private List<String> buildCategoryList(String category, String subCategory) {
        List<String> categories = new ArrayList<>();
        if (category != null) {
            categories.add(category);
        }
        if (subCategory != null) {
            categories.add(subCategory);
        }
        return categories;
    }

    /**
     * 프로젝트 상태를 문자열로 계산
     */
    private String calculateStatusString(OffsetDateTime startDate, OffsetDateTime endDate) {
        if (startDate == null || endDate == null) {
            return "알 수 없음";
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (now.isBefore(startDate)) {
            return "모집중";
        } else if (now.isAfter(endDate)) {
            return "완료";
        } else {
            return "진행중";
        }
    }

    /**
     * ProjectParticipantVo → ProjectParticipantEntity 변환 (생성용)
     */
    public ProjectParticipantEntity toEntity(ProjectParticipantVo vo) {
        return ProjectParticipantEntity.builder()
                .id(vo.id())
                .projectId(vo.projectId())
                .memberId(vo.memberId())
                .status(vo.status())
                .createdAt(vo.createdAt())
                .updatedAt(vo.updatedAt())
                .build();
    }

    /**
     * ProjectParticipantEntity → ProjectParticipantVo 변환
     */
    public ProjectParticipantVo toVo(ProjectParticipantEntity entity) {
        return new ProjectParticipantVo(
                entity.getId(),
                entity.getProjectId(),
                entity.getMemberId(),
                null, // memberName은 별도 조회 필요
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * ProjectParticipantEntity → ProjectParticipantCreatedVo 변환
     */
    public ProjectParticipantCreatedVo toCreatedVo(ProjectParticipantEntity entity) {
        return new ProjectParticipantCreatedVo(
                entity.getId(),
                entity.getProjectId(),
                entity.getMemberId(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    /**
     * ProjectParticipantEntity → ProjectParticipantStatusUpdatedVo 변환
     */
    public ProjectParticipantStatusUpdatedVo toStatusUpdatedVo(
            ProjectParticipantEntity entity,
            org.certis.studyplatform.project.domain.ProjectParticipantStatus previousStatus) {
        return new ProjectParticipantStatusUpdatedVo(
                entity.getId(),
                entity.getProjectId(),
                entity.getMemberId(),
                previousStatus,
                entity.getStatus(),
                entity.getUpdatedAt()
        );
    }

    // ================================================================
    // jOOQ RECORD → VO MAPPINGS
    // ================================================================

    /**
     * jOOQ Record → ProjectParticipantVo 변환 (상세 조회용)
     */
    public ProjectParticipantVo toVoFromRecord(Record record) {
        return new ProjectParticipantVo(
                record.getValue("id", Long.class),
                record.getValue("project_id", Long.class),
                record.getValue("member_id", Long.class),
                record.getValue("member_name", String.class),
                record.getValue("status", org.certis.studyplatform.project.domain.ProjectParticipantStatus.class),
                record.getValue("created_at", java.time.OffsetDateTime.class),
                record.getValue("updated_at", java.time.OffsetDateTime.class)
        );
    }

    /**
     * jOOQ Record → ProjectParticipantSummaryVo 변환 (목록 조회용)
     */
    public ProjectParticipantSummaryVo toSummaryVoFromRecord(Record record) {
        return new ProjectParticipantSummaryVo(
                record.getValue("id", Long.class),
                record.getValue("member_id", Long.class),
                record.getValue("member_name", String.class),
                record.getValue("status", org.certis.studyplatform.project.domain.ProjectParticipantStatus.class),
                record.getValue("created_at", java.time.OffsetDateTime.class)
        );
    }

    // ================================================================
    // ENTITY → SUMMARY VO MAPPINGS
    // ================================================================

    /**
     * ProjectParticipantEntity → ProjectParticipantSummaryVo 변환
     */
    public ProjectParticipantSummaryVo toSummaryVo(ProjectParticipantEntity entity) {
        return new ProjectParticipantSummaryVo(
                entity.getId(),
                entity.getMemberId(),
                null, // memberName은 별도 조회 필요
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}