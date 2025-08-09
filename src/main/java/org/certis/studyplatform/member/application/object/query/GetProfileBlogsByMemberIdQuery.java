package org.certis.studyplatform.member.application.object.query;

/**
 * Get Profile Blogs By Member ID Query
 * 회원 ID로 관련 블로그 목록 조회를 위한 Query 객체
 */
public record GetProfileBlogsByMemberIdQuery(
        Long memberId
) {
}