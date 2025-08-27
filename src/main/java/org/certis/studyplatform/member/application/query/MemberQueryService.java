package org.certis.studyplatform.member.application.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.application.object.query.GetMemberByIdQuery;
import org.certis.studyplatform.member.application.object.query.GetMembersQuery;
import org.certis.studyplatform.member.application.object.query.SearchMembersQuery;
import org.certis.studyplatform.member.domain.service.MemberDomainService;
import org.certis.studyplatform.member.domain.vo.MemberSummaryVo; // Uses VO
import org.certis.studyplatform.member.domain.vo.MemberVo;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * ✅ Query Object를 받아서 회원 검색
     */
    public Page<MemberSummaryVo> searchMembers(SearchMembersQuery query) {
        log.info("Query: Searching members with criteria - keyword: {}, grade: {}, role: {}",
                query.keyword(), query.grade(), query.role());

        return memberDomainService.searchMemberVos(query);
    }

    /**
     * ✅ Query Object를 받아서 전체 회원 조회
     */
    public Page<MemberSummaryVo> getAllMembers(GetMembersQuery query) {
        log.info("Query: Getting all members with pagination - page: {}, size: {}",
                query.pageable().getPageNumber(), query.pageable().getPageSize());

        return memberDomainService.getAllMemberVos(query);
    }
}