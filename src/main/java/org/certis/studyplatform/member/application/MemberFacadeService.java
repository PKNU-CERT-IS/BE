package org.certis.studyplatform.member.application;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.application.command.*;
import org.certis.studyplatform.member.application.mapper.MemberAdminApplicationMapper;
import org.certis.studyplatform.member.application.object.command.*;
import org.certis.studyplatform.member.application.object.query.GetMemberByIdQuery;
import org.certis.studyplatform.member.application.object.query.GetMembersQuery;
import org.certis.studyplatform.member.application.object.query.SearchMembersForAdminQuery;
import org.certis.studyplatform.member.application.object.query.SearchMembersQuery;
import org.certis.studyplatform.member.application.query.*;
import org.certis.studyplatform.member.application.mapper.MemberApplicationMapper;
import org.certis.studyplatform.member.application.mapper.MemberApplicationCommandMapper;
import org.certis.studyplatform.member.application.mapper.MemberApplicationQueryMapper;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.presentation.dto.request.*;
import org.certis.studyplatform.member.presentation.dto.response.AdminMemberUpdateResponseDto;
import org.certis.studyplatform.member.presentation.dto.response.MemberDataForAdminResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberFacadeService {

    private final MemberCommandService memberCommandService;
    private final MemberQueryService memberQueryService;
    private final MemberApplicationMapper memberApplicationMapper;
    private final MemberApplicationCommandMapper memberApplicationCommandMapper;
    private final MemberApplicationQueryMapper memberApplicationQueryMapper;
    private final MemberAdminApplicationMapper memberAdminApplicationMapper;

    // ================================================================
    // COMMAND OPERATIONS - 상태 변경 작업
    // ================================================================

    /**
     * 회원 생성
     */
    public MemberCreatedVo createMember(MemberCreateRequestDto requestDto) {
        log.info("Facade: Creating member with student number: {}", requestDto.getStudentNumber());

        // DTO → Command Object 변환 (새로운 매퍼 사용)
        CreateMemberCommand command = memberApplicationCommandMapper.toCreateMemberCommand(requestDto);

        // Command Service 호출
        MemberCreatedVo createdVo = memberCommandService.createMember(command);

        log.info("Facade: Member created successfully with ID: {}", createdVo.id());
        return createdVo;
    }

    /**
     * 회원 정보 수정 (통합: 기본정보 + 프로필 + 기술스택)
     */
    public MemberUpdatedVo updateMember(Long memberId, MemberUpdateRequestDto requestDto) {
        log.info("Facade: Updating member with ID: {}", memberId);

        // DTO → Command Object 변환 (새로운 매퍼 사용)
        UpdateMemberCommand command = memberApplicationCommandMapper.toMemberUpdateCommand(memberId, requestDto);

        // Command Service 호출
        MemberUpdatedVo updatedVo = memberCommandService.updateMember(command);

        log.info("Facade: Member updated successfully with ID: {}", memberId);
        return updatedVo;
    }

    /**
     * 회원 삭제
     */
    public void deleteMember(Long memberId) {
        log.info("Facade: Deleting member with ID: {}", memberId);

        // DTO → Command Object 변환 (새로운 매퍼 사용)
        DeleteMemberCommand command = memberApplicationCommandMapper.toDeleteMemberCommand(memberId);

        // Command Service 호출
        memberCommandService.deleteMember(command);

        log.info("Facade: Member deleted successfully with ID: {}", memberId);
    }

    // ================================================================
    // QUERY OPERATIONS - 조회 작업
    // ================================================================

    /**
     * 회원 상세 조회
     */
    public MemberVo getMemberDetail(Long memberId) {
        log.info("Facade: Getting member detail with ID: {}", memberId);

        // DTO → Query Object 변환 (새로운 매퍼 사용)
        GetMemberByIdQuery query = memberApplicationQueryMapper.toGetMemberByIdQuery(memberId);

        // Query Service 호출
        MemberVo memberVo = memberQueryService.getMemberById(query);

        log.info("Facade: Member detail retrieved successfully for ID: {}", memberId);
        return memberVo;
    }

    /**
     * 회원 검색 (페이징 포함)
     */
    public Page<MemberSummaryVo> searchMembers(MemberSearchRequestDto requestDto, Pageable pageable) {
        log.info("Facade: Searching members with criteria: {}", requestDto.getKeyword());

        // DTO → Query Object 변환 (새로운 매퍼 사용)
        SearchMembersQuery query = memberApplicationQueryMapper.toSearchMembersQuery(requestDto, pageable);

        // Query Service 호출
        Page<MemberSummaryVo> memberSummaryVos = memberQueryService.searchMembers(query);

        log.info("Facade: Member search completed successfully. Found {} members", memberSummaryVos.getTotalElements());
        return memberSummaryVos;
    }

    /**
     * 회원 검색 (기본 페이징 사용)
     */
    public Page<MemberSummaryVo> searchMembers(MemberSearchRequestDto requestDto) {
        log.info("Facade: Searching members with criteria: {}", requestDto.getKeyword());

        // DTO → Query Object 변환 (새로운 매퍼 사용)
        SearchMembersQuery query = memberApplicationQueryMapper.toSearchMembersQuery(requestDto);

        // Query Service 호출
        Page<MemberSummaryVo> memberSummaryVos = memberQueryService.searchMembers(query);

        log.info("Facade: Member search completed successfully. Found {} members", memberSummaryVos.getTotalElements());
        return memberSummaryVos;
    }

    /**
     * 키워드로 회원 검색
     */
    public Page<MemberSummaryVo> searchMembersByKeyword(String keyword, Pageable pageable) {
        log.info("Facade: Searching members by keyword: {}", keyword);

        // DTO → Query Object 변환 (새로운 매퍼 사용)
        SearchMembersQuery query = memberApplicationQueryMapper.toSearchMembersQuery(keyword, pageable);

        // Query Service 호출
        Page<MemberSummaryVo> memberSummaryVos = memberQueryService.searchMembers(query);

        log.info("Facade: Keyword search completed successfully. Found {} members", memberSummaryVos.getTotalElements());
        return memberSummaryVos;
    }

    /**
     * 전체 회원 조회
     */
    public Page<MemberSummaryVo> getAllMembers(Pageable pageable) {
        log.info("Facade: Getting all members with pagination");

        // DTO → Query Object 변환 (새로운 매퍼 사용)
        GetMembersQuery query = memberApplicationQueryMapper.toGetMembersQuery(pageable);

        // Query Service 호출
        Page<MemberSummaryVo> memberSummaryVos = memberQueryService.getAllMembers(query);

        log.info("Facade: All members retrieved successfully. Found {} members", memberSummaryVos.getTotalElements());
        return memberSummaryVos;
    }

    // ================================================================
    // PRIVATE HELPER METHODS
    // ================================================================

    /**
     * 페이징 정보 생성
     */
    private Pageable createPageable(Integer page, Integer size, String sortBy, String sortDirection) {
        // 기존 구현 유지
        return null; // 실제 구현은 기존 코드 유지
    }

    public AdminMemberUpdateResponseDto updateMemberAdminFields(Long executorId,
                                                                MemberRole executorRole,
                                                                AdminMemberUpdateRequestDto request) {
        return null;
    }

    @Transactional
    public List<MemberDataForAdminResponseDto> searchMembersForAdmin(String search) {
        log.info("Facade: Searching members for admin with keyword: {}", search);

        // Query Service 호출
        SearchMembersForAdminQuery query = SearchMembersForAdminQuery.of(search);
        List<MemberSearchForAdminVo> memberVos = memberQueryService.searchMembersForAdmin(query);

        // VO → DTO 변환
        List<MemberDataForAdminResponseDto> memberDtos = memberVos.stream()
                .map(memberAdminApplicationMapper::convertToMemberDataForAdminResponseDto)
                .toList();

        log.info("Facade: Admin member search completed. Found {} members", memberDtos.size());

        return memberDtos;
    }

    @Transactional
    public void grantGracePeriod(GrantGracePeriodRequestDto request) {
        log.info("Facade: Granting grace period - memberId: {}, gracePeriod: {}",
                request.getMemberId(), request.getGracePeriod());

        // DTO → Command Object 변환
        UpdateGracePeriodCommand command = UpdateGracePeriodCommand.of(
                request.getMemberId(),
                request.getGracePeriod()
        );

        // Command Service 호출
        memberCommandService.grantGracePeriod(command);

        log.info("Facade: Grace period granted successfully for member: {}", request.getMemberId());
    }

    @Transactional
    public void assignPenalty(PenaltyRequestDto request) {
        log.info("Facade: Assigning penalty - memberId: {}, penaltyPoints: {}",
                request.getMemberId(), request.getPenaltyPoints());

        // DTO → Command Object 변환
        UpdatePenaltyCommand command = UpdatePenaltyCommand.of(
                request.getMemberId(),
                request.getPenaltyPoints()
        );

        // Command Service 호출
        memberCommandService.assignPenalty(command);

        log.info("Facade: Penalty assigned successfully for member: {}", request.getMemberId());
    }
}