package org.certis.studyplatform.member.application.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.application.object.query.GetProfileBlogsByMemberIdQuery;
import org.certis.studyplatform.member.application.object.query.GetProfileByMemberIdQuery;
import org.certis.studyplatform.member.application.object.query.GetProfileProjectsByMemberIdQuery;
import org.certis.studyplatform.member.application.object.query.GetProfileStudiesByMemberIdQuery;
import org.certis.studyplatform.member.domain.service.ProfileDomainService;
import org.certis.studyplatform.member.domain.vo.ProfileVo;
import org.certis.studyplatform.member.domain.vo.ProfileBlogVo;
import org.certis.studyplatform.member.domain.vo.ProfileProjectVo;
import org.certis.studyplatform.member.domain.vo.ProfileStudyVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Profile Query Service
 *
 * CQRS Query 측면의 서비스 (Application Layer)
 * 현재 로그인한 사용자의 프로필 관련 읽기 작업을 담당
 *
 * ✅ DomainService를 통한 데이터 흐름
 * ✅ VO 직접 반환: ResponseDTO 변환 제거
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileQueryService {

    private final ProfileDomainService profileDomainService;

    /**
     * 회원 ID로 프로필 조회 (내 프로필 조회용)
     *
     * @param query GetProfileByMemberIdQuery
     * @return 프로필 정보 (VO 직접 반환)
     */
    @Transactional(readOnly = true)
    public ProfileVo getProfileByMemberId(GetProfileByMemberIdQuery query) {
        log.info("Query: Getting profile by member ID: {}", query.memberId());

        // Domain Service를 통한 프로필 조회
        ProfileVo profileVo = profileDomainService.getProfileVoByMemberId(query);

        return profileVo;
    }

    /**
     * 회원이 관여한 스터디 목록 조회
     *
     * @param query 회원 ID
     * @return 스터디 목록 (VO 직접 반환)
     */
    @Transactional(readOnly = true)
    public List<ProfileStudyVo> getStudiesByMemberId(GetProfileStudiesByMemberIdQuery query) {
        log.info("Query: Getting studies by member ID: {}", query.memberId());

        // Domain Service를 통한 스터디 목록 조회
        List<ProfileStudyVo> studyVos = profileDomainService.getStudiesByMemberId(query);

        log.info("Query: Found {} studies for member ID: {}", studyVos.size(), query);

        return studyVos;
    }

    /**
     * 회원이 관여한 프로젝트 목록 조회
     *
     * @param query 회원 ID
     * @return 프로젝트 목록 (VO 직접 반환)
     */
    @Transactional(readOnly = true)
    public List<ProfileProjectVo> getProjectsByMemberId(GetProfileProjectsByMemberIdQuery query) {
        log.info("Query: Getting projects by member ID: {}", query.memberId());

        // Domain Service를 통한 프로젝트 목록 조회
        List<ProfileProjectVo> projectVos = profileDomainService.getProjectsByMemberId(query);

        log.info("Query: Found {} projects for member ID: {}", projectVos.size(), query.memberId());

        return projectVos;
    }

    /**
     * 회원이 작성한 블로그 목록 조회
     *
     * @param query 회원 ID
     * @return 블로그 목록 (VO 직접 반환)
     */
    @Transactional(readOnly = true)
    public List<ProfileBlogVo> getBlogsByMemberId(GetProfileBlogsByMemberIdQuery query) {
        log.info("Query: Getting blogs by member ID: {}", query.memberId());

        // Domain Service를 통한 블로그 목록 조회
        List<ProfileBlogVo> blogVos = profileDomainService.getBlogsByMemberId(query);

        log.info("Query: Found {} blogs for member ID: {}", blogVos.size(), query.memberId());

        return blogVos;
    }
}