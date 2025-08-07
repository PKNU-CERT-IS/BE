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
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class MemberSearchResponseDto {

    private final List<MemberSummaryResponseDto> members;

    private final int currentPage;

    private final int totalPages;

    private final long totalElements;

    private final boolean hasNext;

    private final boolean hasPrevious;

    public static MemberSearchResponseDto of(Page<MemberSummaryResponseDto> memberPage) {
        return MemberSearchResponseDto.builder()
                .members(memberPage.getContent())
                .currentPage(memberPage.getNumber())
                .totalPages(memberPage.getTotalPages())
                .totalElements(memberPage.getTotalElements())
                .hasNext(memberPage.hasNext())
                .hasPrevious(memberPage.hasPrevious())
                .build();
    }

    // toString for logging
    @Override
    public String toString() {
        return "MemberSearchResponseDto{" +
                "membersCount=" + (members != null ? members.size() : 0) +
                ", currentPage=" + currentPage +
                ", totalPages=" + totalPages +
                ", totalElements=" + totalElements +
                '}';
    }
}