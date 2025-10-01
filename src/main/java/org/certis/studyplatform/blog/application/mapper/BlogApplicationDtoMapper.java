package org.certis.studyplatform.blog.application.mapper;

import org.certis.studyplatform.blog.domain.vo.BlogEnableReferenceVo;
import org.certis.studyplatform.blog.domain.vo.BlogSummaryVo;
import org.certis.studyplatform.blog.domain.vo.BlogVo;
import org.certis.studyplatform.blog.presentation.dto.response.*;
import org.certis.studyplatform.shared.service.S3FileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Blog Application DTO Mapper
 *
 * Clean Architecture Application Layer
 * VO → DTO 변환 담당 (FacadeService에서만 사용)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BlogApplicationDtoMapper {

    private final S3FileService s3FileService;

    /**
     * BlogVo를 BlogDetailResponseDto로 변환
     */
    public BlogDetailResponseDto toBlogDetailResponseDto(BlogVo vo) {
        if (vo == null) {
            return null;
        }

        return BlogDetailResponseDto.builder()
                .id(vo.id().value())
                .title(vo.title())
                .content(vo.content())
                .description(vo.description())
                .category(vo.category())
                .referenceType(vo.referenceType())
                .referenceId(vo.referenceId())
                .referenceTitle(vo.referenceTitle())
                .viewCount(vo.viewCount())
                .creatorName(vo.creatorName())
                .createdAt(vo.createdAt())
                .isPublic(vo.isPublic())
                .build();
    }

    /**
     * BlogSummaryVo를 BlogSummaryResponseDto로 변환
     */
    public BlogSummaryResponseDto toBlogSummaryResponseDto(BlogSummaryVo vo) {
        if (vo == null) {
            return null;
        }

        // 디버그 로그 추가
        log.debug("BlogSummaryResponseDto mapping - vo.blogCreatorProfileImageUrl: {}", vo.blogCreatorProfileImageUrl());
        log.debug("BlogSummaryResponseDto mapping - vo: {}", vo);

        return BlogSummaryResponseDto.builder()
                .id(vo.id().value())
                .title(vo.title())
                .description(vo.description())
                .category(vo.category())
                .referenceType(vo.referenceType())
                .referenceTitle(vo.referenceTitle())
                .referenceId(vo.studyId() != null ? vo.studyId() : vo.projectId())
                .createdAt(vo.createdAt())
                .updatedAt(vo.updatedAt())
                .blogCreatorName(vo.blogCreatorName())
                .blogCreatorProfileImageUrl(normalizeUrl(vo.blogCreatorProfileImageUrl()))
                .views(vo.views())
                .build();
    }

    /**
     * BlogEnableReferenceVo를 BlogEnableReferenceResponseDto로 변환
     */
    public BlogEnableReferenceResponseDto toBlogEnableReferenceResponseDto(BlogEnableReferenceVo vo) {
        if (vo == null) {
            return null;
        }

        return BlogEnableReferenceResponseDto.builder()
                .referenceType(vo.referenceType())
                .referenceId(vo.referenceId())
                .referenceTitle(vo.title())
                .build();
    }

    /**
     * BlogSummaryVo 리스트를 BlogSummaryResponseDto 리스트로 변환
     */
    public List<BlogSummaryResponseDto> toBlogSummaryResponseDtoList(List<BlogSummaryVo> vos) {
        if (vos == null || vos.isEmpty()) {
            return Collections.emptyList();
        }

        return vos.stream()
                .map(this::toBlogSummaryResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * BlogSummaryVo Page를 BlogSummaryResponseDto Page로 변환
     */
    public Page<BlogSummaryResponseDto> toBlogSummaryResponseDtoPage(Page<BlogSummaryVo> voPage) {
        if (voPage == null) {
            return Page.empty();
        }

        List<BlogSummaryResponseDto> dtoList = toBlogSummaryResponseDtoList(voPage.getContent());
        // Normalize Pageable to avoid Unpaged serialization issues
        var pageable = voPage.getPageable().isPaged() ? voPage.getPageable() : org.springframework.data.domain.PageRequest.of(0, dtoList.size() == 0 ? 1 : dtoList.size());
        return new PageImpl<>(dtoList, pageable, voPage.getTotalElements());
    }

    /**
     * BlogEnableReferenceVo 리스트를 BlogEnableReferenceResponseDto 리스트로 변환
     */
    public List<BlogEnableReferenceResponseDto> toBlogEnableReferenceResponseDtoList(List<BlogEnableReferenceVo> vos) {
        if (vos == null || vos.isEmpty()) {
            return Collections.emptyList();
        }

        return vos.stream()
                .map(this::toBlogEnableReferenceResponseDto)
                .collect(Collectors.toList());
    }

    private String normalizeUrl(String url) {
        log.debug("normalizeUrl called with: {}", url);
        if (url == null || url.trim().isEmpty()) {
            log.debug("URL is null or empty, returning null");
            return null;
        }
        String result = s3FileService.toPresignedUrl(url);
        log.debug("normalizeUrl result: {}", result);
        return result;
    }
}