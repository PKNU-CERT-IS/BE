package org.certis.studyplatform.member.application.object.query;

/**
 * Get Profile By Member ID Query
 * 회원 ID로 프로필 조회를 위한 Query 객체
 */
public record GetProfileByMemberIdQuery(
        Long memberId
) {
}
