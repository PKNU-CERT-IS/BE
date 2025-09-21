package org.certis.studyplatform.blog.domain.vo;

import org.certis.studyplatform.blog.domain.ArticleReferenceType;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

import java.time.OffsetDateTime;

/**
 * Blog Value Object
 *
 * Clean Architecture Domain Layer
 * 불변 데이터 전송 객체
 *
 * ✅ 도메인 레벨에서는 추상화된 referenceType/referenceId 사용
 * ✅ Infrastructure 레이어에서 Entity의 studyId/projectId로 변환
 * ✅ 결합도 최소화를 위해 참조 제목은 별도 처리
 */
public record BlogVo(
        BlogIdVo id,
        String title,
        String description,
        String content,
        String category,
        ArticleReferenceType referenceType,
        Long referenceId,
        String referenceTitle,
        Long creatorId,
        String creatorName,
        Integer viewCount,
        OffsetDateTime createdAt,
        Boolean isPublic
) {

    /**
     * Compact constructor with validation
     */
    public BlogVo {
        // 제목 검증
        if (title == null || title.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.BLOG_DOMAIN_INVALID_TITLE, "블로그 제목이 올바르지 않습니다.");
        }

        // 설명 검증
        if (description == null || description.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.BLOG_DOMAIN_INVALID_DESCRIPTION, "블로그 설명이 올바르지 않습니다.");
        }

        // 내용 검증
        if (content == null || content.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.BLOG_DOMAIN_INVALID_CONTENT, "블로그 내용이 올바르지 않습니다.");
        }

        // 카테고리 검증
        if (category == null || category.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.BLOG_DOMAIN_INVALID_CATEGORY, "블로그 카테고리가 올바르지 않습니다.");
        }

        // 참조 타입 검증
        if (referenceType == null) {
            throw new DomainException(ExceptionStatus.BLOG_DOMAIN_INVALID_REFERENCE, "참조 타입이 올바르지 않습니다.");
        }

        // 참조 ID 검증
        if (referenceId == null || referenceId <= 0) {
            throw new DomainException(ExceptionStatus.BLOG_DOMAIN_INVALID_REFERENCE, "유효한 참조 ID는 필수입니다");
        }

        // Creator ID 검증
        if (creatorId == null) {
            throw new DomainException(ExceptionStatus.BLOG_DOMAIN_INVALID_CREATOR, "블로그 작성자 ID는 필수입니다");
        }

        // 조회수 검증
        if (viewCount != null && viewCount < 0) {
            throw new DomainException(ExceptionStatus.BLOG_DOMAIN_INVALID_VIEW_COUNT, "조회수는 0 이상이어야 합니다");
        }
    }

    /**
     * 기본 팩토리 메서드 (referenceTitle 포함)
     */
    public static BlogVo of(
            BlogIdVo id,
            String title,
            String description,
            String content,
            String category,
            ArticleReferenceType referenceType,
            Long referenceId,
            String referenceTitle,
            Long creatorId,
            String creatorName,
            Integer viewCount,
            OffsetDateTime createdAt,
            Boolean isPublic
    ) {
        return new BlogVo(
                id, title, description, content, category,
                referenceType, referenceId, referenceTitle, creatorId, creatorName,
                viewCount, createdAt, isPublic
        );
    }

    /**
     * referenceTitle 없이 생성하는 팩토리 메서드 (Infrastructure 레이어용)
     */
    public static BlogVo ofWithoutReferenceTitle(
            BlogIdVo id,
            String title,
            String description,
            String content,
            String category,
            ArticleReferenceType referenceType,
            Long referenceId,
            Long creatorId,
            String creatorName,
            Integer viewCount,
            OffsetDateTime createdAt,
            Boolean isPublic
    ) {
        return new BlogVo(
                id, title, description, content, category,
                referenceType, referenceId, null, // referenceTitle은 null로 설정
                creatorId, creatorName, viewCount, createdAt, isPublic
        );
    }

    /**
     * 새 블로그 생성용 팩토리 메서드
     */
    public static BlogVo createNew(
            String title,
            String description,
            String content,
            String category,
            ArticleReferenceType referenceType,
            Long referenceId,
            String referenceTitle,
            Long creatorId,
            String creatorName,
            Boolean isPublic
    ) {
        return new BlogVo(
                null, // id는 null (새 생성)
                title, description, content, category,
                referenceType, referenceId, referenceTitle, creatorId, creatorName,
                0, // 초기 조회수는 0
                null, // createdAt은 저장 시 자동 설정
                isPublic != null ? isPublic : true // 기본값은 공개
        );
    }

    /**
     * 업데이트용 팩토리 메서드
     */
    public static BlogVo updateFrom(
            BlogVo existing,
            String title,
            String description,
            String content,
            String category,
            ArticleReferenceType referenceType,
            Long referenceId,
            String referenceTitle,
            Boolean isPublic
    ) {
        return new BlogVo(
                existing.id(),
                title != null ? title : existing.title(),
                description != null ? description : existing.description(),
                content != null ? content : existing.content(),
                category != null ? category : existing.category(),
                referenceType != null ? referenceType : existing.referenceType(),
                referenceId != null ? referenceId : existing.referenceId(),
                referenceTitle != null ? referenceTitle : existing.referenceTitle(),
                existing.creatorId(),
                existing.creatorName(),
                existing.viewCount(),
                existing.createdAt(),
                isPublic != null ? isPublic : existing.isPublic()
        );
    }
}