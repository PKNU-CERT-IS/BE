package org.certis.studyplatform.member.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.application.command.*;
import org.certis.studyplatform.member.application.mapper.MemberAdminApplicationMapper;
import org.certis.studyplatform.member.application.object.command.*;
import org.certis.studyplatform.member.application.object.query.*;
import org.certis.studyplatform.member.application.query.*;
import org.certis.studyplatform.member.application.mapper.MemberApplicationMapper;
import org.certis.studyplatform.member.application.mapper.MemberApplicationCommandMapper;
import org.certis.studyplatform.member.application.mapper.MemberApplicationQueryMapper;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.presentation.dto.request.*;
import org.certis.studyplatform.member.presentation.dto.response.AdminMemberUpdateResponseDto;
import org.certis.studyplatform.member.presentation.dto.response.MemberDataForAdminResponseDto;
import org.certis.studyplatform.member.presentation.dto.response.MemberSearchResponseDto;
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


    public AdminMemberUpdateResponseDto updateMemberAdminFields(Long executorId,
                                                                MemberRole executorRole,
                                                                AdminMemberUpdateRequestDto request) {

        UpdateMemberAdminFieldsCommand command = UpdateMemberAdminFieldsCommand.of(executorId,
                executorRole,
                request.getTargetMemberId(),
                request.getNewRole(),
                request.getNewGrade());
        AdminMemberUpdateResultVo resultVo = memberCommandService.updateMemberAdminFields(command);

        return new AdminMemberUpdateResponseDto(
                resultVo.memberId(),
                resultVo.newRole(),
                resultVo.newGrade()
        );
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

    public List<MemberSearchResponseDto> searchMembers(MemberSearchRequestDto request) {
        log.info("Facade: Searching members - keyword: {}, grade: {}, role: {}",
                request.getKeyword(), request.getGrade(), request.getRole());

        SearchMembersWithContactQuery query = SearchMembersWithContactQuery.of(
                request.getKeyword(),
                request.getGrade(),
                request.getRole()
        );

        List<MemberWithContactVo> members = memberQueryService.searchMembersWithContact(query);

        List<MemberSearchResponseDto> response = members.stream()
                .map(memberApplicationMapper::toMemberSearchResponseDto)
                .toList();

        log.info("Facade: Search completed - found {} members", response.size());
        return response;
    }
}