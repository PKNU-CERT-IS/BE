package org.certis.studyplatform.member.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.application.MemberFacadeService;
import org.certis.studyplatform.member.domain.vo.MemberUpdatedVo;
import org.certis.studyplatform.member.presentation.dto.request.MemberSearchRequestDto;
import org.certis.studyplatform.member.presentation.dto.request.MemberUpdateRequestDto;
import org.certis.studyplatform.member.presentation.dto.response.MemberSearchResponseDto;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     */
    @GetMapping("/search")
    public ResponseEntity<GlobalResponseHandler<List<MemberSearchResponseDto>>> searchMembers(
            @ModelAttribute MemberSearchRequestDto searchRequest) {

        log.info("REST: Searching members - keyword: {}, grade: {}, role: {}",
                searchRequest.getKeyword(),
                searchRequest.getGrade(),
                searchRequest.getRole());

        List<MemberSearchResponseDto> result = memberFacadeService.searchMembers(searchRequest);

        log.info("REST: Search completed - found {} results", result.size());

        return GlobalResponseHandler.success(ResponseStatus.MEMBER_SEARCH_SUCCESS, result);
    }

}