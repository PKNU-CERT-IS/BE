package org.certis.studyplatform.member.presentation;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.presentation.dto.request.GrantGracePeriodRequestDto;
import org.certis.studyplatform.member.presentation.dto.request.PenaltyRequestDto;
import org.certis.studyplatform.member.presentation.dto.response.MemberDataForAdminResponseDto;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.certis.studyplatform.exception.ApplicationException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.application.MemberFacadeService;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.presentation.dto.request.AdminMemberUpdateRequestDto;
import org.certis.studyplatform.member.presentation.dto.response.AdminMemberUpdateResponseDto;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/member")
public class AdminMemberController {

    private final MemberFacadeService memberFacadeService;

    @PostMapping("/update")
//    @PreAuthorize("hasRole('STAFF') or hasRole('VICECHAIRMAN') or hasRole('CHAIRMAN') or hasRole('ADMIN')") for test
    public ResponseEntity<GlobalResponseHandler<AdminMemberUpdateResponseDto>> updateMemberAdminFields(
            @Valid @RequestBody AdminMemberUpdateRequestDto request
//            @AuthenticationPrincipal CurrentUser currentUser
    ){
        CurrentUser currentUser = new CurrentUser(
                1L,                    // 관리자 ID
                "testAdmin",           // username
                "test@admin.com",      // email
                "테스트 관리자",        // name
                "ADMIN");            // role);

        // 자기 자신의 권한/학년 변경 방지
        if (currentUser.getId().equals(request.getTargetMemberId())) {
            throw new ApplicationException(ExceptionStatus.MEMBER_APPLICATION_CANNOT_CHANGE_OWN_ADMIN_FIELDS);
        }

        AdminMemberUpdateResponseDto response = memberFacadeService.updateMemberAdminFields(
                currentUser.getId(),
                MemberRole.valueOf(currentUser.getRole()),
                request
        );


        return GlobalResponseHandler.success(ResponseStatus.MEMBER_ADMIN_PROFILE_UPDATE_SUCCESS, response);
    }

    /**
     * 회원 키워드 검색
     */
    @GetMapping("/keyword")
    @Operation(summary = "회원 키워드 검색", description = "이름, 학번, 전공으로 회원을 검색합니다")
    public ResponseEntity<GlobalResponseHandler<List<MemberDataForAdminResponseDto>>> searchMembers(
            @RequestParam(value = "search", required = false) String search) {

        List<MemberDataForAdminResponseDto> result = memberFacadeService.searchMembersForAdmin(search);

        return GlobalResponseHandler.success(ResponseStatus.MEMBER_ADMIN_SEARCH_SUCCESS,result);
    }

    /**
     * 유예기간 부여
     */
    @PostMapping("/grace-period")
    @Operation(summary = "유예기간 부여", description = "특정 회원에게 유예기간을 부여합니다")
    public ResponseEntity<GlobalResponseHandler<Void>> grantGracePeriod(
            @Valid @RequestBody GrantGracePeriodRequestDto request) {

        memberFacadeService.grantGracePeriod(request);

        return GlobalResponseHandler.success(ResponseStatus.MEMBER_ADMIN_GRACE_PERIOD_UPDATE_SUCCESS);
    }

    /**
     * 벌점 부여
     */
    @PostMapping("/penalty")
    @Operation(summary = "벌점 부여", description = "특정 회원에게 벌점을 부여합니다")
    public ResponseEntity<GlobalResponseHandler<Void>> assignPenalty(
            @Valid @RequestBody PenaltyRequestDto request) {

        memberFacadeService.assignPenalty(request);
        return GlobalResponseHandler.success(ResponseStatus.MEMBER_ADMIN_PENALTY_UPDATE_SUCCESS);
    }

    /**
     * 회원 삭제
     *
     * @param id 회원 ID
     * @return 성공 응답
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalResponseHandler<Void>> deleteMember(@PathVariable Long id) {
        log.info("REST: Deleting member - {}", id);

        // Facade를 통한 삭제
        memberFacadeService.deleteMember(id);

        return GlobalResponseHandler.success(ResponseStatus.MEMBER_DELETE_SUCCESS);
    }
}
