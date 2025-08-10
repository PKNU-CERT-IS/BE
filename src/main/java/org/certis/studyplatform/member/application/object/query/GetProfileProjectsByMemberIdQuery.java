package org.certis.studyplatform.member.application.object.query;

/**
 * Get Profile Projects By Member ID Query
 * 회원 ID로 관련 프로젝트 목록 조회를 위한 Query 객체
 */
public record GetProfileProjectsByMemberIdQuery(
        Long memberId
) {
}