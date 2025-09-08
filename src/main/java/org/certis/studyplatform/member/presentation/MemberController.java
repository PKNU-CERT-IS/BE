package org.certis.studyplatform.member.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.application.MemberFacadeService;
import org.certis.studyplatform.member.domain.vo.MemberVo;
import org.certis.studyplatform.member.domain.vo.MemberCreatedVo;
import org.certis.studyplatform.member.domain.vo.MemberUpdatedVo;
import org.certis.studyplatform.member.domain.vo.MemberSummaryVo;
import org.certis.studyplatform.member.presentation.dto.request.MemberCreateRequestDto;
import org.certis.studyplatform.member.presentation.dto.request.MemberSearchRequestDto;
import org.certis.studyplatform.member.presentation.dto.request.MemberUpdateRequestDto;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Member REST Controller
 *
 * Clean Architecture Presentation Layer
 * Global Response Handler를 사용한 일관된 API 응답 제공
 * Clean Architecture 계층별 예외 처리
 *
 * ✅ RequestDTO → Facade → VO → Controller (VO 직접 반환) 패턴 적용
 * ✅ 통합 수정 엔드포인트: PUT /api/v1/members/{id}
 *
 * 의존성 흐름:
 * Presentation → Application (Facade) → Application (Services) → Domain → Infrastructure
 */
@RestController
@RequestMapping("/api/v1/member")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberFacadeService memberFacadeService;

    /**
     * 회원 상세 조회 -> members 페이지 조회로 구현
     *
     * @param id 회원 ID
     * @return 회원 상세 정보 (VO 직접 반환)
     */
    @GetMapping("/{id}")
    public ResponseEntity<GlobalResponseHandler<MemberVo>> getMember(@PathVariable Long id) {
        log.info("REST: Getting member - {}", id);

        // Facade를 통한 조회 (VO로 받음)
        MemberVo memberVo = memberFacadeService.getMemberDetail(id);

        return GlobalResponseHandler.success(ResponseStatus.MEMBER_FIND_SUCCESS, memberVo);
    }

    /**
     * 회원 정보 통합 수정
     *
     * 프로필, 기술스택, 기본정보를 하나의 엔드포인트에서 처리
     *
     * @param id 회원 ID
     * @param request 회원 정보 수정 요청 DTO (통합)
     * @return 수정된 회원 정보 (VO 직접 반환)
     */
    @PutMapping("/{id}")
    public ResponseEntity<GlobalResponseHandler<MemberUpdatedVo>> updateMember(
            @PathVariable Long id,
            @Valid @RequestBody MemberUpdateRequestDto request) {
        log.info("REST: Updating member - ID: {}, fields: name={}, profileImage={}, grade={}, role={}, major={}, skills={}",
                id,
                request.getName() != null,
                request.getProfileImage() != null,
                request.getGrade() != null,
                request.getRole() != null,
                request.getMajor() != null,
                request.getSkills() != null);

        // RequestDTO를 Facade에 전달하고 VO로 받음
        MemberUpdatedVo updatedVo = memberFacadeService.updateMember(id, request);

        return GlobalResponseHandler.success(ResponseStatus.MEMBER_UPDATE_SUCCESS, updatedVo);
    }
}