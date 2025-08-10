package org.certis.studyplatform.member.application.object.query;

/**
 * Get Profile Studies By Member ID Query
 * 회원 ID로 관련 스터디 목록 조회를 위한 Query 객체
 */
public record GetProfileStudiesByMemberIdQuery(
        Long memberId
) {
}