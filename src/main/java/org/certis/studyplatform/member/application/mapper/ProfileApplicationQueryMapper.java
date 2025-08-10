package org.certis.studyplatform.member.application.mapper;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.member.application.object.query.GetProfileByMemberIdQuery;
import org.certis.studyplatform.member.application.object.query.GetProfileStudiesByMemberIdQuery;
import org.certis.studyplatform.member.application.object.query.GetProfileProjectsByMemberIdQuery;
import org.certis.studyplatform.member.application.object.query.GetProfileBlogsByMemberIdQuery;
import org.springframework.stereotype.Component;

/**
 * Profile Application Query Mapper
 *
 * ✅ Parameter → Query Object 변환 담당
 * ✅ Application Layer의 Profile Query 전용 매퍼
 * ✅ 네이밍 컨벤션: ProfileApplicationQueryMapper
 */
@Component
@RequiredArgsConstructor
public class ProfileApplicationQueryMapper {

    /**
     * memberId → GetProfileByMemberIdQuery 변환
     */
    public GetProfileByMemberIdQuery toGetProfileByMemberIdQuery(Long memberId) {
        return new GetProfileByMemberIdQuery(memberId);
    }

    /**
     * memberId → GetProfileStudiesByMemberIdQuery 변환
     */
    public GetProfileStudiesByMemberIdQuery toGetProfileStudiesByMemberIdQuery(Long memberId) {
        return new GetProfileStudiesByMemberIdQuery(memberId);
    }

    /**
     * memberId → GetProfileProjectsByMemberIdQuery 변환
     */
    public GetProfileProjectsByMemberIdQuery toGetProfileProjectsByMemberIdQuery(Long memberId) {
        return new GetProfileProjectsByMemberIdQuery(memberId);
    }

    /**
     * memberId → GetProfileBlogsByMemberIdQuery 변환
     */
    public GetProfileBlogsByMemberIdQuery toGetProfileBlogsByMemberIdQuery(Long memberId) {
        return new GetProfileBlogsByMemberIdQuery(memberId);
    }
}