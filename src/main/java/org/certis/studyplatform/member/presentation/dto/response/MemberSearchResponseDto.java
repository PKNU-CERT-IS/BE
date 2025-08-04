package org.certis.studyplatform.member.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 회원 검색 응답 DTO
 * 
 * 검색 결과와 페이지네이션 정보 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSearchResponseDto {
    
    /**
     * 검색된 회원 목록
     */
    private List<MemberSummaryDto> members;
    
    /**
     * 페이지네이션 정보
     */
    private PageInfoDto pageInfo;
    
    /**
     * 검색 조건 정보
     */
    private SearchInfoDto searchInfo;
    
    /**
     * 회원 요약 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberSummaryDto {
        private Long id;
        private String name;
        private String description;
        private String studentNumber;
        private String grade;
        private String role;
        private String major;
        private List<String> skills;
        
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private ZonedDateTime createdAt;
    }
    
    /**
     * 페이지네이션 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfoDto {
        private int currentPage;
        private int pageSize;
        private int totalPages;
        private long totalElements;
        private boolean hasNext;
        private boolean hasPrevious;
    }
    
    /**
     * 검색 조건 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchInfoDto {
        private String grade;
        private String role;
        private String keyword;
        private int resultCount;
    }
} 