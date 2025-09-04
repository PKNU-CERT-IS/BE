package org.certis.studyplatform.study.application.object.query;

import lombok.Builder;

import org.springframework.data.domain.Pageable;


@Builder
public record GetCompletedStudiesByMemberQuery(
        Long memberId,
        Pageable pageable
) {
    public static GetCompletedStudiesByMemberQuery of(Long memberId, Pageable pageable) {
        return GetCompletedStudiesByMemberQuery.builder()
                .memberId(memberId)
                .pageable(pageable)
                .build();
    }
}
