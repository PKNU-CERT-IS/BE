package org.certis.studyplatform.blog.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Blog Detail Response DTO
 *
 * 블로그 상세 조회 시 사용되는 상세 정보 응답 DTO
 * Presentation Layer의 응답 DTO
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BlogDetailResponseDto {

    private Long id;

    private String title;

    private String content;

    private String description;

    private String category;

    private Integer viewCount;

    private String creatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime createdAt;

    // toString for logging
    @Override
    public String toString() {
        return "BlogDetailResponseDto{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", creatorName='" + creatorName + '\'' +
                ", viewCount=" + viewCount +
                '}';
    }
} 