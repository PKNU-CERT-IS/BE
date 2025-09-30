package org.certis.studyplatform.blog.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.blog.domain.ArticleReferenceType;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Blog Summary Response DTO
 *
 * 블로그 목록 조회 시 사용되는 요약 정보 응답 DTO
 * Presentation Layer의 응답 DTO
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BlogSummaryResponseDto {

    private Long id;

    private String title;

    private String description;

    private String category;

    private ArticleReferenceType referenceType;

    private String referenceTitle;

    private Long referenceId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime updatedAt;

    private String blogCreatorName;

    private String blogCreatorProfileImageUrl;

    private Integer views;

    // toString for logging
    @Override
    public String toString() {
        return "BlogSummaryResponseDto{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", blogCreatorName='" + blogCreatorName +
                '}';
    }
}