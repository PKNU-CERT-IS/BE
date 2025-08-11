package org.certis.studyplatform.member.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/member")
public class AdminMemberController {

    private final MemberFacadeService memberFacadeService;


    @RequestMapping("/update")
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
}
