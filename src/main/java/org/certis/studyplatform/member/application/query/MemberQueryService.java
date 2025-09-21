package org.certis.studyplatform.member.application.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.application.command.GetMemberTokenInfoQuery;
import org.certis.studyplatform.member.application.object.query.*;
import org.certis.studyplatform.member.domain.service.MemberDomainService;
import org.certis.studyplatform.member.domain.vo.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Member Query Service
 *
 * ✅ Query Object를 받아서 조회 작업을 수행하고, 그 결과를 VO로 반환합니다.
 * ✅ 오직 '읽기(Read)' 작업과 관련된 책임만 가집니다.
 * ✅ 새로운 매퍼 시스템 사용
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MemberQueryService {

    private final MemberDomainService memberDomainService;

    /**
     * ✅ Query Object를 받아서 단건 회원 조회
     */
    public MemberVo getMemberById(GetMemberByIdQuery query) {
        log.info("Query: Getting member with ID: {}", query.id());

        MemberVo memberVo = memberDomainService.getMemberVo(query);

        log.info("Query: Member VO found: {}", memberVo.name());
        return memberVo;
    }

    /**
     * ✅ JWT 토큰 생성용 회원 정보 조회
     * Domain Service를 통한 클린 아키텍처 구조 준수
     */
    public MemberTokenInfoVo getMemberTokenInfo(GetMemberTokenInfoQuery query) {
        log.info("Query: Getting member token info with ID: {}", query.memberId());

        // Domain Service 호출
        MemberTokenInfoVo tokenInfo = memberDomainService.getMemberTokenInfoVo(query);

        log.info("Query: Member token info found: memberId={}, name={}, hasEmail={}",
                tokenInfo.memberId(), tokenInfo.name(), tokenInfo.hasEmail());
        return tokenInfo;
    }

    public List<MemberSearchForAdminVo> searchMembersForAdmin(SearchMembersForAdminQuery query) {

        log.info("Query: Searching members for admin with keyword={}", query.keyword());

        List<MemberSearchForAdminVo> voList = memberDomainService.searchMembersForAdmin(query);

        log.info("Query: Found {} members for keyword={}", voList.size(), query.keyword());

        return voList;
    }

    public List<MemberWithContactVo> searchMembersWithContact(SearchMembersWithContactQuery query) {
        log.info("Query Service: Searching members with contact - keyword: {}, grade: {}, role: {}",
                query.keyword(), query.grade(), query.role());

        List<MemberWithContactVo> members = memberDomainService.searchMembersWithContact(query);

        log.info("Query Service: Found {} members with contact info", members.size());

        return members;
    }
}