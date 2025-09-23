package org.certis.studyplatform.blog.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.blog.domain.ArticleReferenceType;

/**
 * Blog Enable Reference Response DTO
 *
 * 블로그로 작성 가능한 프로젝트/스터디의 정보를 반환하는 DTO
 * Presentation Layer의 응답 DTO
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BlogEnableReferenceResponseDto {

    private ArticleReferenceType referenceType;

    private Long referenceId;

    private String referenceTitle;

    // toString for logging
    @Override
    public String toString() {
        return "BlogEnableReferenceResponseDto{" +
                "referenceType=" + referenceType + '\'' +
                ", referenceId='" + referenceId +
                ", referenceTitle='" + referenceTitle + '\'' +
                '}';
    }
}