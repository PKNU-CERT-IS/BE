package org.certis.studyplatform.project.infrastructure.mapper;

import org.certis.studyplatform.project.domain.vo.*;
import org.certis.studyplatform.project.domain.ProjectStatus;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectEntity;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectParticipantEntity;
import org.jooq.Record;
import org.springframework.stereotype.Component;

import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.certis.generated.jooq.Tables.*;
import org.certis.studyplatform.member.domain.MemberGrade;

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
                .externalUrl(vo.externalUrl() != null ? 
                    "{\"title\":\"" + vo.externalUrl().title() + "\",\"url\":\"" + vo.externalUrl().url() + "\"}" : null)
                .demoUrl(vo.demoUrl())
                .thumbnailUrl(vo.thumbnailUrl())
                .startedAt(vo.startDate()) // startDate → startedAt
                .endedAt(vo.endDate()) // endDate → endedAt
                // 기본 상태값 설정 (NOT NULL 제약 대응)
                .resultSubmitStatus(ResultSubmitStatus.READY)
                .build();
    }

    /**
     * ProjectEntity를 ProjectVo로 변환 (조회용)
     */
    public ProjectVo toVo(ProjectEntity entity) {
        if (entity == null) {
            return null;
        }

        // ExternalUrl 문자열을 ExternalUrlVo로 변환
        ExternalUrlVo externalUrlVo = null;
        if (entity.getExternalUrl() != null && !entity.getExternalUrl().trim().isEmpty()) {
            try {
                // JSON 파싱 로직 (간단한 구현)
                String externalUrl = entity.getExternalUrl();
                if (externalUrl.startsWith("{") && externalUrl.endsWith("}")) {
                    // JSON에서 title과 url 추출 (간단한 파싱)
                    String title = extractJsonValue(externalUrl, "title");
                    String url = extractJsonValue(externalUrl, "url");
                    if (title != null && url != null) {
                        externalUrlVo = new ExternalUrlVo(title, url);
                    }
                }
            } catch (Exception e) {
                // JSON 파싱 실패 시 null로 설정
                externalUrlVo = null;
            }
        }

        OffsetDateTime endedAt = entity.getEndedAt();
        // 우선순위: Entity에 저장된 명시적 status가 있으면 그것을 우선 사용
        String resolvedStatus = entity.getStatus() != null
                ? entity.getStatus().name()
                : calculateStatusString(entity.getStartedAt(), endedAt, entity.getDeletedAt(), entity.getResultSubmitStatus());

        return ProjectVo.of(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getContent(),
                entity.getCategory(),
                entity.getSubcategory(), // subcategory → subCategory
                entity.getStartedAt(), // startedAt → startDate
                endedAt, // endedAt → endDate
                entity.getMemberId(), // memberId → creatorId
                null, // creatorName은 별도 조회 필요
                null, // creatorGrade는 별도 조회 필요
                calculateSemester(endedAt), // semester 계산
                resolvedStatus, // status 계산 (APPROVED 등 명시 상태 우선)
                entity.getResultSubmitStatus(),
                entity.getGithubUrl(),
                externalUrlVo,
                entity.getDemoUrl(),
                entity.getThumbnailUrl(),
                entity.getMaxParticipantsNumber(), // maxParticipantsNumber → maxParticipants
                0, // currentParticipants는 별도 계산 필요
                determineParticipantable(entity.getStartedAt(), endedAt,
                    entity.getMaxParticipantsNumber(), 0),
                Collections.emptyList(), // attached는 별도 조회 필요
                null // meetingSummaryVos는 별도 조회 필요
        );
    }

    /**
     * JSON 문자열에서 특정 키의 값을 추출하는 헬퍼 메서드
     */
    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            // 파싱 실패 시 null 반환
        }
        return null;
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

        // ExternalUrl 문자열을 ExternalUrlVo로 변환
        ExternalUrlVo externalUrlVo = null;
        String externalUrlStr = record.get(PROJECT.EXTERNAL_URL);
        if (externalUrlStr != null && !externalUrlStr.trim().isEmpty()) {
            try {
                if (externalUrlStr.startsWith("{") && externalUrlStr.endsWith("}")) {
                    String title = extractJsonValue(externalUrlStr, "title");
                    String url = extractJsonValue(externalUrlStr, "url");
                    if (title != null && url != null) {
                        externalUrlVo = new ExternalUrlVo(title, url);
                    }
                }
            } catch (Exception e) {
                externalUrlVo = null;
            }
        }

        OffsetDateTime endedAt = record.get(PROJECT.ENDED_AT);
        OffsetDateTime deletedAt = record.get(PROJECT.DELETED_AT);
        ResultSubmitStatus submitStatus = record.get("result_submit_status", ResultSubmitStatus.class);
        if (submitStatus == null) {
            submitStatus = ResultSubmitStatus.READY;
        }
        
        String calculatedStatus = calculateStatusString(record.get(PROJECT.STARTED_AT), endedAt, deletedAt, submitStatus);
        String resolvedStatusFromRecord = resolveStatusFromRecord(record, calculatedStatus);

        return ProjectVo.of(
                record.get(PROJECT.ID),
                record.get(PROJECT.TITLE),
                record.get(PROJECT.DESCRIPTION),
                record.get(PROJECT.CONTENT),
                record.get(PROJECT.CATEGORY),
                record.get(PROJECT.SUBCATEGORY),
                record.get(PROJECT.STARTED_AT),
                endedAt,
                record.get(PROJECT.MEMBER_ID),
                record.get(MEMBER.NAME), // JOIN된 creatorName
                record.get(MEMBER.GRADE, String.class), // JOIN된 creatorGrade
                calculateSemester(endedAt), // semester 계산
                resolvedStatusFromRecord, // status 계산 (DB status 우선)
                submitStatus,
                record.get(PROJECT.GITHUB_URL), // githubUrl은 별도 관리
                externalUrlVo,
                record.get(PROJECT.DEMO_URL),
                record.get(PROJECT.THUMBNAIL_URL),
                record.get(PROJECT.MAX_PARTICIPANTS_NUMBER),
                record.get("current_participants", Integer.class), // 서브쿼리 결과
                determineParticipantable(record.get(PROJECT.STARTED_AT), endedAt,
                    record.get(PROJECT.MAX_PARTICIPANTS_NUMBER), 
                    record.get("current_participants", Integer.class)),
                Collections.emptyList(), // attached는 별도 조회 필요
                null
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
                record.get(PROJECT.ENDED_AT),
                record.get(PROJECT.DELETED_AT),
                record.get("result_submit_status", ResultSubmitStatus.class)
        );
        status = resolveStatusFromRecord(record, status);

        // 참여 가능 여부 계산
        boolean isParticipantable = determineParticipantable(
                record.get(PROJECT.STARTED_AT),
                record.get(PROJECT.ENDED_AT),
                record.get(PROJECT.MAX_PARTICIPANTS_NUMBER),
                record.get("current_participants", Integer.class)
        );

        // ExternalUrl 문자열을 ExternalUrlVo로 변환
        ExternalUrlVo externalUrlVo = null;
        String externalUrlStr = record.get(PROJECT.EXTERNAL_URL);
        if (externalUrlStr != null && !externalUrlStr.trim().isEmpty()) {
            try {
                if (externalUrlStr.startsWith("{") && externalUrlStr.endsWith("}")) {
                    String title = extractJsonValue(externalUrlStr, "title");
                    String url = extractJsonValue(externalUrlStr, "url");
                    if (title != null && url != null) {
                        externalUrlVo = new ExternalUrlVo(title, url);
                    }
                }
            } catch (Exception e) {
                externalUrlVo = null;
            }
        }

        return ProjectSummaryVo.of(
                record.get(PROJECT.ID),
                record.get(PROJECT.TITLE),
                record.get(PROJECT.DESCRIPTION),
                record.get(PROJECT.CATEGORY),
                record.get(PROJECT.SUBCATEGORY),
                record.get(PROJECT.STARTED_AT),
                record.get(PROJECT.ENDED_AT),
                record.get(MEMBER.NAME), // JOIN된 creatorName
                MemberGrade.fromGradeString(record.get(MEMBER.GRADE, String.class)),
                calculateSemester(record.get(PROJECT.ENDED_AT)), // semester 계산
                status, // status 계산
                isParticipantable,
                record.get(PROJECT.GITHUB_URL), // githubUrl
                externalUrlVo,
                record.get(PROJECT.THUMBNAIL_URL),
                record.get(PROJECT.DEMO_URL), // demoUrl
                record.get(PROJECT.MAX_PARTICIPANTS_NUMBER), // maxParticipantNumber
                record.get("current_participants", Integer.class), // currentParticipantNumber
                record.get("result_submit_status", ResultSubmitStatus.class),
                java.util.Collections.emptyList()
        );
    }

    /**
     * 단순 VO 변환 (ProjectQueryRepositoryImpl.findById용)
     */
    public ProjectVo toProjectVo(Record record) {
        return toProjectVoFromRecord(record); // 동일한 로직 재사용
    }

    /**
     * jOOQ Record 리스트를 ProjectVo로 변환 (첨부파일 포함)
     * ProjectQueryRepositoryImpl.findById에서 사용
     */
    public ProjectVo toProjectVoFromRecordsWithAttachments(List<Record> records) {
        if (records == null || records.isEmpty()) {
            return null;
        }

        Record firstRecord = records.get(0);
        
        // ExternalUrl 문자열을 ExternalUrlVo로 변환
        ExternalUrlVo externalUrlVo = null;
        String externalUrlStr = firstRecord.get(PROJECT.EXTERNAL_URL);
        if (externalUrlStr != null && !externalUrlStr.trim().isEmpty()) {
            try {
                if (externalUrlStr.startsWith("{") && externalUrlStr.endsWith("}")) {
                    String title = extractJsonValue(externalUrlStr, "title");
                    String url = extractJsonValue(externalUrlStr, "url");
                    if (title != null && url != null) {
                        externalUrlVo = new ExternalUrlVo(title, url);
                    }
                }
            } catch (Exception e) {
                externalUrlVo = null;
            }
        }

        // 첨부파일 정보 변환 (여러 첨부파일 처리)
        List<ProjectAttachedVo> attachedVos = new ArrayList<>();
        for (Record record : records) {
            if (record.get("attached_id", Long.class) != null) {
                attachedVos.add(ProjectAttachedVo.of(
                        record.get("attached_id", Long.class),
                        record.get("attached_name", String.class),
                        record.get("attached_type", String.class),
                        record.get("attached_size", String.class),
                        record.get("attached_url", String.class)
                ));
            }
        }

        OffsetDateTime endedAt = firstRecord.get(PROJECT.ENDED_AT);
        OffsetDateTime deletedAt = firstRecord.get(PROJECT.DELETED_AT);
        ResultSubmitStatus submitStatus = firstRecord.get("result_submit_status", ResultSubmitStatus.class);
        if (submitStatus == null) {
            submitStatus = ResultSubmitStatus.READY;
        }
        
        String calculatedStatus2 = calculateStatusString(firstRecord.get(PROJECT.STARTED_AT), endedAt, deletedAt, submitStatus);
        String resolvedStatusFromRecord2 = resolveStatusFromRecord(firstRecord, calculatedStatus2);

        return ProjectVo.of(
                firstRecord.get(PROJECT.ID),
                firstRecord.get(PROJECT.TITLE),
                firstRecord.get(PROJECT.DESCRIPTION),
                firstRecord.get(PROJECT.CONTENT),
                firstRecord.get(PROJECT.CATEGORY),
                firstRecord.get(PROJECT.SUBCATEGORY),
                firstRecord.get(PROJECT.STARTED_AT),
                endedAt,
                firstRecord.get(PROJECT.MEMBER_ID),
                firstRecord.get(MEMBER.NAME), // JOIN된 creatorName
                firstRecord.get(MEMBER.GRADE, String.class), // JOIN된 creatorGrade
                calculateSemester(endedAt), // semester 계산
                resolvedStatusFromRecord2, // status 계산 (DB status 우선)
                submitStatus,
                firstRecord.get(PROJECT.GITHUB_URL), // githubUrl은 별도 관리
                externalUrlVo,
                firstRecord.get(PROJECT.DEMO_URL),
                firstRecord.get(PROJECT.THUMBNAIL_URL),
                firstRecord.get(PROJECT.MAX_PARTICIPANTS_NUMBER),
                firstRecord.get("current_participants", Integer.class), // 서브쿼리 결과
                determineParticipantable(firstRecord.get(PROJECT.STARTED_AT), endedAt,
                    firstRecord.get(PROJECT.MAX_PARTICIPANTS_NUMBER), 
                    firstRecord.get("current_participants", Integer.class)),
                attachedVos, // 첨부파일 정보
                null
        );
    }

    /**
     * jOOQ Record를 ProjectVo로 변환 (첨부파일 포함)
     * ProjectQueryRepositoryImpl.findById에서 사용
     */
    public ProjectVo toProjectVoFromRecordWithAttachments(Record record) {
        if (record == null) {
            return null;
        }

        // ExternalUrl 문자열을 ExternalUrlVo로 변환
        ExternalUrlVo externalUrlVo = null;
        String externalUrlStr = record.get(PROJECT.EXTERNAL_URL);
        if (externalUrlStr != null && !externalUrlStr.trim().isEmpty()) {
            try {
                if (externalUrlStr.startsWith("{") && externalUrlStr.endsWith("}")) {
                    String title = extractJsonValue(externalUrlStr, "title");
                    String url = extractJsonValue(externalUrlStr, "url");
                    if (title != null && url != null) {
                        externalUrlVo = new ExternalUrlVo(title, url);
                    }
                }
            } catch (Exception e) {
                externalUrlVo = null;
            }
        }

        // 첨부파일 정보 변환 (여러 첨부파일 처리)
        List<ProjectAttachedVo> attachedVos = new ArrayList<>();
        if (record.get("attached_id", Long.class) != null) {
            attachedVos.add(ProjectAttachedVo.of(
                    record.get("attached_id", Long.class),
                    record.get("attached_name", String.class),
                    record.get("attached_type", String.class),
                    record.get("attached_size", String.class),
                    record.get("attached_url", String.class)
            ));
        }

        OffsetDateTime endedAt = record.get(PROJECT.ENDED_AT);
        OffsetDateTime deletedAt = record.get(PROJECT.DELETED_AT);
        ResultSubmitStatus submitStatus = record.get("result_submit_status", ResultSubmitStatus.class);
        if (submitStatus == null) {
            submitStatus = ResultSubmitStatus.READY;
        }
        
        String calculatedStatus3 = calculateStatusString(record.get(PROJECT.STARTED_AT), endedAt, deletedAt, submitStatus);
        String resolvedStatusFromRecord3 = resolveStatusFromRecord(record, calculatedStatus3);

        return ProjectVo.of(
                record.get(PROJECT.ID),
                record.get(PROJECT.TITLE),
                record.get(PROJECT.DESCRIPTION),
                record.get(PROJECT.CONTENT),
                record.get(PROJECT.CATEGORY),
                record.get(PROJECT.SUBCATEGORY),
                record.get(PROJECT.STARTED_AT),
                endedAt,
                record.get(PROJECT.MEMBER_ID),
                record.get(MEMBER.NAME), // JOIN된 creatorName
                record.get(MEMBER.GRADE, String.class), // JOIN된 creatorGrade
                calculateSemester(endedAt), // semester 계산
                resolvedStatusFromRecord3, // status 계산 (DB status 우선)
                submitStatus,
                record.get(PROJECT.GITHUB_URL), // githubUrl은 별도 관리
                externalUrlVo,
                record.get(PROJECT.DEMO_URL),
                record.get(PROJECT.THUMBNAIL_URL),
                record.get(PROJECT.MAX_PARTICIPANTS_NUMBER),
                record.get("current_participants", Integer.class), // 서브쿼리 결과
                determineParticipantable(record.get(PROJECT.STARTED_AT), endedAt,
                    record.get(PROJECT.MAX_PARTICIPANTS_NUMBER), 
                    record.get("current_participants", Integer.class)),
                attachedVos, // 첨부파일 정보
                null
        );
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
     * 프로젝트 상태를 ProjectStatus enum으로 계산
     */
    private String calculateStatusString(OffsetDateTime startDate, OffsetDateTime endDate,
                                         OffsetDateTime deletedAt,
                                         ResultSubmitStatus resultSubmitStatus) {
        if (deletedAt != null) {
            return ProjectStatus.REJECTED.name();
        }
        OffsetDateTime now = OffsetDateTime.now();
        // 종료 승인 또는 종료 시간이 현재와 같거나 이전이면 완료 처리
        if (resultSubmitStatus == ResultSubmitStatus.COMPLETED) {
            return ProjectStatus.COMPLETED.name();
        }
        if (endDate != null && (now.isAfter(endDate) || now.isEqual(endDate))) {
            return ProjectStatus.COMPLETED.name();
        }
        if (startDate == null || endDate == null) {
            return ProjectStatus.READY.name();
        }
        if (now.isBefore(startDate)) {
            return ProjectStatus.READY.name();
        }
        if (now.isBefore(endDate)) {
            return ProjectStatus.INPROGRESS.name();
        }
        return ProjectStatus.INPROGRESS.name();
    }

    /**
     * Record에 DB의 명시적 status 컬럼이 포함되어 있으면 그 값을 우선 사용한다.
     * 없거나 비어있으면 계산된 상태 문자열을 반환한다.
     */
    private String resolveStatusFromRecord(Record record, String calculatedFallback) {
        if (record == null) {
            return calculatedFallback;
        }
        try {
            if (record.field("status") != null) {
                String dbStatus = record.get("status", String.class);
                if (dbStatus != null && !dbStatus.isBlank()) {
                    return dbStatus.trim().toUpperCase();
                }
            }
        } catch (Exception ignore) {
        }
        return calculatedFallback;
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
                record.getValue("project_id", Long.class),
                record.getValue("member_id", Long.class),
                record.getValue("member_name", String.class),
                record.getValue("member_grade", org.certis.studyplatform.member.domain.MemberGrade.class),
                record.getValue("project_title", String.class),
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
                entity.getProjectId(),
                entity.getMemberId(),
                null, // memberName은 별도 조회 필요
                null, // memberGrade는 별도 조회 필요
                null, // projectTitle은 별도 조회 필요
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    // ================================================================
    // 비즈니스 로직 계산 메서드들
    // ================================================================

    /**
     * ended_at을 기준으로 semester 계산
     * 3월~8월: 해당 년도 1학기
     * 9월~다음해 2월: 해당 년도 2학기
     */
    private String calculateSemester(OffsetDateTime endedAt) {
        if (endedAt == null) {
            return null;
        }
        
        LocalDate endDate = endedAt.toLocalDate();
        int year = endDate.getYear();
        int month = endDate.getMonthValue();
        
        if (month >= 3 && month <= 8) {
            return year + "-1"; // 1학기
        } else {
            return year + "-2"; // 2학기
        }
    }


    /**
     * 참여 가능 여부를 계산합니다.
     * - 시작일이 현재 시간보다 미래이거나 현재 시간과 같으면 참여 가능
     * - 종료일이 현재 시간보다 미래이거나 현재 시간과 같으면 참여 가능
     * - 현재 참여자 수가 최대 참여자 수보다 적으면 참여 가능
     */
    private boolean determineParticipantable(OffsetDateTime startDate, OffsetDateTime endDate, 
                                           Integer maxParticipants, Integer currentParticipants) {
        if (startDate == null || endDate == null || maxParticipants == null || currentParticipants == null) {
            return false;
        }
        
        OffsetDateTime now = OffsetDateTime.now();
        
        // 프로젝트/스터디가 종료되지 않아야 함 (종료일이 현재 시간보다 미래)
        if (endDate.isBefore(now) || endDate.isEqual(now)) {
            return false;
        }
        
        // 현재 참여자 수가 최대 참여자 수보다 작아야 함
        return currentParticipants < maxParticipants;
    }
}