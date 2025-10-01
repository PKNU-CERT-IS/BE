package org.certis.studyplatform.blog.infrastructure.mapper;

import org.certis.studyplatform.blog.domain.vo.*;
import org.certis.studyplatform.blog.domain.ArticleReferenceType;
import org.certis.studyplatform.blog.infrastructure.persistence.entity.BlogEntity;
import org.certis.studyplatform.blog.domain.repository.BlogRedisRepository;
import org.jooq.Record;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.certis.generated.jooq.Tables.*;

/**
 * Blog Infrastructure Mapper
 *
 * Clean Architecture Infrastructure Layer
 * Entity ↔ VO 변환 및 jOOQ Record → VO 변환
 *
 * ✅ BlogEntity의 studyId, projectId를 ArticleReferenceType으로 변환
 * ✅ JOIN된 참조 제목 정보 처리
 * ✅ ViewCount는 단건 조회에서만 포함
 * ✅ BlogVo 내부 검증 로직과 연동
 */
@Component
@Slf4j
public class BlogInfrastructureMapper {

    private final BlogRedisRepository blogRedisRepository;

    public BlogInfrastructureMapper(BlogRedisRepository blogRedisRepository) {
        this.blogRedisRepository = blogRedisRepository;
    }

    // ================================================================
    // COMMAND REPOSITORY 매핑 (Entity ↔ VO)
    // ================================================================

    /**
     * BlogVo를 BlogEntity로 변환 (저장용)
     * referenceType과 referenceId를 studyId, projectId로 변환
     */
    public BlogEntity toEntity(BlogVo vo) {
        if (vo == null) {
            return null;
        }

        // BlogVo의 참조 정보를 Entity 필드로 변환
        Long studyId = null;
        Long projectId = null;

        if (vo.referenceType() == ArticleReferenceType.STUDY && vo.referenceId() != null) {
            studyId = vo.referenceId();
        } else if (vo.referenceType() == ArticleReferenceType.PROJECT && vo.referenceId() != null) {
            projectId = vo.referenceId();
        }

        return BlogEntity.builder()
                .id(vo.id() != null ? vo.id().value() : null)
                .memberId(vo.creatorId()) // creatorId → memberId
                .studyId(studyId) // referenceType에 따라 설정
                .projectId(projectId) // referenceType에 따라 설정
                .title(vo.title())
                .content(vo.content())
                .category(vo.category()) // String 그대로 사용
                .isPublic(vo.isPublic() != null ? vo.isPublic() : true) // VO의 isPublic 사용
                .description(vo.description())
                .build();
    }

    /**
     * BlogEntity를 BlogVo로 변환 (조회용)
     * studyId, projectId를 ArticleReferenceType으로 변환
     */
    public BlogVo toVo(BlogEntity entity) {
        if (entity == null) {
            return null;
        }

        // Entity의 studyId, projectId를 참조 정보로 변환
        ArticleReferenceType referenceType = null;
        Long referenceId = null;

        if (entity.getStudyId() != null) {
            referenceType = ArticleReferenceType.STUDY;
            referenceId = entity.getStudyId();
        } else if (entity.getProjectId() != null) {
            referenceType = ArticleReferenceType.PROJECT;
            referenceId = entity.getProjectId();
        }

        return BlogVo.of(
                BlogIdVo.of(entity.getId()),
                entity.getTitle(),
                entity.getDescription(),
                entity.getContent(),
                entity.getCategory(), // String 그대로 사용
                referenceType,
                referenceId,
                null, // referenceTitle은 별도 조회 필요
                entity.getMemberId(), // memberId → creatorId
                null, // creatorName은 별도 조회 필요
                null, // viewCount는 별도 조회 필요
                entity.getCreatedAt(),
                entity.getIsPublic()
        );
    }

    // ================================================================
    // QUERY REPOSITORY 매핑 (jOOQ Record → VO)
    // ================================================================

    /**
     * jOOQ Record를 BlogVo로 변환 (상세 조회용)
     * JOIN된 참조 제목 정보 포함
     */
    public BlogVo toBlogVoFromRecord(Record record) {
        if (record == null) {
            return null;
        }

        Long blogId = record.get(BLOG.ID);
        String title = record.get(BLOG.TITLE);
        String description = record.get(BLOG.DESCRIPTION);
        String content = record.get(BLOG.CONTENT, String.class); // Safe get with type
        String category = record.get(BLOG.CATEGORY); // String 그대로 사용
        Long studyId = record.get(BLOG.STUDY_ID);
        Long projectId = record.get(BLOG.PROJECT_ID);
        String studyTitle = record.get("study_title", String.class);
        String projectTitle = record.get("project_title", String.class);
        Long memberId = record.get(BLOG.MEMBER_ID);
        String creatorName = record.get("creator_name", String.class);
        String creatorProfileImage = record.get("creator_profile_image", String.class);
        OffsetDateTime createdAt = record.get(BLOG.CREATED_AT);
        OffsetDateTime updatedAt = record.get(BLOG.UPDATED_AT);
        Boolean isPublic = record.get(BLOG.IS_PUBLIC);

        // 참조 타입 및 정보 결정
        ArticleReferenceType referenceType = null;
        Long referenceId = null;
        String referenceTitle = null;

        if (studyId != null) {
            referenceType = ArticleReferenceType.STUDY;
            referenceId = studyId;
            referenceTitle = studyTitle;
        } else if (projectId != null) {
            referenceType = ArticleReferenceType.PROJECT;
            referenceId = projectId;
            referenceTitle = projectTitle;
        }

        return BlogVo.of(
                BlogIdVo.of(blogId),
                title,
                description,
                content,
                category, // String 그대로 사용
                referenceType,
                referenceId,
                referenceTitle, // JOIN으로 조회된 참조 제목
                memberId, // memberId → creatorId
                creatorName, // JOIN된 작성자명
                null, // viewCount는 별도 조회
                createdAt,
                isPublic
        );
    }

    /**
     * jOOQ Record를 BlogSummaryVo로 변환 (목록 조회용)
     * JOIN된 참조 제목 정보 포함, ViewCount는 데이터베이스에서 조회
     * category는 String으로 직접 사용
     */
    public BlogSummaryVo toSummaryVoFromRecord(Record record) {
        return toBlogSummaryVoFromRecord(record);
    }

    /**
     * jOOQ Record를 BlogSummaryVo로 변환 (목록 조회용)
     * JOIN된 참조 제목 정보 포함, ViewCount는 데이터베이스에서 조회
     * category는 String으로 직접 사용
     */
    public BlogSummaryVo toBlogSummaryVoFromRecord(Record record) {
        if (record == null) {
            return null;
        }

        Long blogId = record.get(BLOG.ID);
        String title = record.get(BLOG.TITLE);
        String description = record.get(BLOG.DESCRIPTION);
        String category = record.get(BLOG.CATEGORY); // String으로 직접 사용
        OffsetDateTime createdAt = record.get(BLOG.CREATED_AT);
        OffsetDateTime updatedAt = record.get(BLOG.UPDATED_AT);
        String creatorName = record.get("creator_name", String.class);
        String creatorProfileImage = record.get("creator_profile_image", String.class);
        if (creatorProfileImage == null) {
            creatorProfileImage = record.get("profile_image", String.class);
        }
        
        // 디버그 로그 추가 - 모든 필드 확인
        log.debug("BlogSummaryVo mapping - blogId: {}, creatorName: {}, creatorProfileImage: {}", 
                blogId, creatorName, creatorProfileImage);
        log.debug("Record fields: {}", Arrays.stream(record.fields())
                .map(field -> field.getName() + "=" + record.get(field))
                .collect(Collectors.joining(", ")));
        
        // 데이터베이스에서 view_count 조회 (JOIN으로 가져온 값)
        Integer views = record.get("view_count", Integer.class);
        if (views == null) {
            // 데이터베이스에 조회수 정보가 없으면 Redis에서 조회 시도
            views = getViewCountFromRedis(blogId);
        }

        Long studyId = record.get(BLOG.STUDY_ID);
        Long projectId = record.get(BLOG.PROJECT_ID);
        String studyTitle = record.get("study_title", String.class);
        String projectTitle = record.get("project_title", String.class);

        // 참조 타입 및 제목 결정
        ArticleReferenceType referenceType = null;
        String referenceTitle = null;

        if (studyId != null) {
            referenceType = ArticleReferenceType.STUDY;
            referenceTitle = studyTitle;
        } else if (projectId != null) {
            referenceType = ArticleReferenceType.PROJECT;
            referenceTitle = projectTitle;
        }

        log.debug("Creating BlogSummaryVo with creatorProfileImage: {}", creatorProfileImage);
        log.debug("About to call BlogSummaryVo.of with creatorProfileImage parameter: '{}'", creatorProfileImage);
        
        BlogSummaryVo result = BlogSummaryVo.of(
                BlogIdVo.of(blogId),
                title,
                description,
                category, // String 그대로 사용
                createdAt,
                updatedAt,
                creatorName,
                creatorProfileImage,
                referenceType,
                referenceTitle,
                views,
                studyId,
                projectId,
                studyTitle,
                projectTitle
        );
        
        log.debug("Created BlogSummaryVo with blogCreatorProfileImageUrl: {}", result.blogCreatorProfileImageUrl());
        log.debug("BlogSummaryVo toString: {}", result);
        return result;
    }

    /**
     * 단순 VO 변환 (BlogQueryRepositoryImpl.findById용)
     * BlogVo의 다양한 변환 메소드를 위한 공통 로직
     */
    public BlogVo toBlogVo(Record record) {
        return toBlogVoFromRecord(record); // 동일한 로직 재사용
    }

    // ================================================================
    // VIEWCOUNT 관련 매핑 (단건 조회용)
    // ================================================================

    /**
     * ViewCount가 포함된 BlogVo 변환 (단건 조회 전용)
     * getBlogById에서만 사용
     */
    public BlogVo toBlogVoWithViewCount(Record record, Integer viewCount) {
        BlogVo baseBlogVo = toBlogVoFromRecord(record);

        if (baseBlogVo == null) {
            return null;
        }

        // ViewCount가 포함된 새로운 BlogVo 생성
        return BlogVo.of(
                baseBlogVo.id(),
                baseBlogVo.title(),
                baseBlogVo.description(),
                baseBlogVo.content(),
                baseBlogVo.category(),
                baseBlogVo.referenceType(),
                baseBlogVo.referenceId(),
                baseBlogVo.referenceTitle(),
                baseBlogVo.creatorId(),
                baseBlogVo.creatorName(),
                viewCount, // ViewCount 설정
                baseBlogVo.createdAt(),
                baseBlogVo.isPublic()
        );
    }

    // ================================================================
    // PRIVATE HELPER METHODS
    // ================================================================

    /**
     * Redis에서 view_count 조회 (fallback 포함)
     */
    private Integer getViewCountFromRedis(Long blogId) {
        try {
            BlogIdVo blogIdVo = BlogIdVo.of(blogId);
            Long redisViewCount = blogRedisRepository.getViewCount(blogIdVo);
            return redisViewCount != null ? redisViewCount.intValue() : 0;
        } catch (Exception e) {
            // Redis 조회 실패 시 0 반환
            return 0;
        }
    }

    /**
     * BlogEntity의 studyId, projectId를 이용해서 ArticleReferenceType 결정
     */
    private ArticleReferenceType determineReferenceType(Long studyId, Long projectId) {
        if (studyId != null) {
            return ArticleReferenceType.STUDY;
        } else if (projectId != null) {
            return ArticleReferenceType.PROJECT;
        }
        return null;
    }

    /**
     * BlogEntity의 studyId, projectId를 이용해서 referenceId 결정
     */
    private Long determineReferenceId(Long studyId, Long projectId) {
        if (studyId != null) {
            return studyId;
        } else if (projectId != null) {
            return projectId;
        }
        return null;
    }

    /**
     * Category 문자열을 List<String>으로 변환 (BlogVo용 - 여전히 List<String> 사용)
     */
    private List<String> convertCategoryStringToList(String categoryStr) {
        if (categoryStr == null || categoryStr.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(categoryStr.split(","));
    }

    /**
     * Category List<String>을 문자열로 변환 (Entity 저장용)
     */
    private String convertCategoryListToString(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return "";
        }
        return String.join(",", categories);
    }

    /**
     * 참조 제목 결정 (JOIN 결과에서)
     */
    private String determineReferenceTitle(Long studyId, Long projectId, String studyTitle, String projectTitle) {
        if (studyId != null && studyTitle != null) {
            return studyTitle;
        } else if (projectId != null && projectTitle != null) {
            return projectTitle;
        }
        return null;
    }

    // ================================================================
    // ALTERNATIVE CONVERSION METHODS (유연성을 위한 추가 메소드들)
    // ================================================================

    /**
     * BlogEntity를 BlogSummaryVo로 변환 (간단한 변환용)
     * Entity → SummaryVo 직접 변환이 필요한 경우
     */
    public BlogSummaryVo toBlogSummaryVo(BlogEntity entity) {
        if (entity == null) {
            return null;
        }

        ArticleReferenceType referenceType = determineReferenceType(entity.getStudyId(), entity.getProjectId());

        return BlogSummaryVo.of(
                BlogIdVo.of(entity.getId()),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCategory(), // String 그대로 사용
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                null, // blogCreatorName은 별도 조회 필요
                null, // blogCreatorProfileImageUrl 별도 조회 필요
                referenceType,
                null, // referenceTitle은 별도 조회 필요
                null, // views는 별도 조회 필요
                entity.getStudyId(),
                entity.getProjectId(),
                null, // studyTitle은 별도 조회 필요
                null  // projectTitle은 별도 조회 필요
        );
    }

    /**
     * BlogVo를 BlogSummaryVo로 변환 (VO 간 변환)
     */
    public BlogSummaryVo toBlogSummaryVo(BlogVo blogVo) {
        if (blogVo == null) {
            return null;
        }

        return BlogSummaryVo.of(
                blogVo.id(),
                blogVo.title(),
                blogVo.description(),
                blogVo.category(), // String 그대로 사용
                blogVo.createdAt(),
                null, // updatedAt은 BlogVo에 없음
                blogVo.creatorName(),
                null, // blogCreatorProfileImageUrl 없음
                blogVo.referenceType(),
                blogVo.referenceTitle(),
                null, // views는 BlogVo에 없음
                null, // studyId
                null, // projectId
                null, // studyTitle
                null  // projectTitle
        );
    }

    // ================================================================
    // SPECIALIZED CONVERSION METHODS
    // ================================================================

    /**
     * jOOQ Record를 참조 정보가 풍부한 BlogVo로 변환
     * QueryRepository의 복잡한 JOIN 쿼리 결과 변환용
     */
    public BlogVo toEnrichedBlogVo(Record record) {
        BlogVo baseBlogVo = toBlogVoFromRecord(record);

        // 추가 정보가 있다면 여기서 처리
        // 예: 좋아요 수, 댓글 수 등

        return baseBlogVo;
    }

    /**
     * 여러 Record를 BatchBlogSummaryVo로 변환
     * 배치 처리용 최적화된 변환 메소드
     */
    public List<BlogSummaryVo> toBlogSummaryVoList(List<Record> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        return records.stream()
                .map(this::toBlogSummaryVoFromRecord)
                .toList();
    }
}