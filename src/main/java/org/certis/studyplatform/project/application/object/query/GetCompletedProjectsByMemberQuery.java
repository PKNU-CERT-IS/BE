package org.certis.studyplatform.project.application.object.query;

import lombok.Builder;
import org.springframework.data.domain.Pageable;

/**
 * Get Completed Projects By Member Query
 *
 * 특정 멤버가 생성한 완료된 프로젝트 목록 조회 Query
 */
@Builder
public record GetCompletedProjectsByMemberQuery(
        Long memberId,
        Pageable pageable
) {
    public static GetCompletedProjectsByMemberQuery of(Long memberId, Pageable pageable) {
        return GetCompletedProjectsByMemberQuery.builder()
                .memberId(memberId)
                .pageable(pageable)
                .build();
    }
}