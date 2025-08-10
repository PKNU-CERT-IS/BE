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
     * 회원 생성 -> auth 에 만들어 놓았습니다.
     * 
     * @param request 회원 생성 요청 DTO
     * @return 생성된 회원 정보 (VO 직접 반환)
     */
    @PostMapping
    public ResponseEntity<GlobalResponseHandler<MemberCreatedVo>> createMember(
            @Valid @RequestBody MemberCreateRequestDto request) {
        log.info("REST: Creating member - {}", request.getName());
        
        // RequestDTO를 Facade에 전달하고 VO로 받음
        MemberCreatedVo createdVo = memberFacadeService.createMember(request);
        
        log.info("REST: Member created successfully - ID: {}", createdVo.id());
        
        return GlobalResponseHandler.success(ResponseStatus.MEMBER_CREATE_SUCCESS, createdVo);
    }
    
    /**
     * 회원 상세 조회
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
    
    /**
     * 회원 검색
     * 
     * @param searchRequest 검색 조건 DTO
     * @param pageable 페이징 정보
     * @return 검색된 회원 목록과 페이징 정보 (VO 직접 반환)
     */
    @GetMapping("/search")
    public ResponseEntity<GlobalResponseHandler<Page<MemberSummaryVo>>> searchMembers(
            MemberSearchRequestDto searchRequest,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.info("REST: Searching members - grade: {}, role: {}, keyword: {}, page: {}, size: {}", 
                searchRequest.getGrade(), 
                searchRequest.getRole(), 
                searchRequest.getKeyword(),
                pageable.getPageNumber(),
                pageable.getPageSize());
        
        // RequestDTO를 Facade에 전달하고 VO로 받음
        Page<MemberSummaryVo> result = memberFacadeService.searchMembers(searchRequest);
        
        log.info("REST: Search completed - found {} results", result.getTotalElements());
        
        return GlobalResponseHandler.success(ResponseStatus.MEMBER_SEARCH_SUCCESS, result);
    }
    
    /**
     * 키워드로 회원 검색
     * 
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 회원 목록과 페이징 정보 (VO 직접 반환)
     */
    @GetMapping("/search/keyword")
    public ResponseEntity<GlobalResponseHandler<Page<MemberSummaryVo>>> searchMembersByKeyword(
            @RequestParam String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.info("REST: Searching members by keyword - keyword: {}, page: {}, size: {}", 
                keyword, pageable.getPageNumber(), pageable.getPageSize());
        
        // Facade를 통한 키워드 검색
        Page<MemberSummaryVo> result = memberFacadeService.searchMembersByKeyword(keyword, pageable);
        
        log.info("REST: Keyword search completed - found {} results", result.getTotalElements());
        
        return GlobalResponseHandler.success(ResponseStatus.MEMBER_SEARCH_SUCCESS, result);
    }
    
    /**
     * 전체 회원 조회
     * 
     * @param pageable 페이징 정보
     * @return 전체 회원 목록과 페이징 정보 (VO 직접 반환)
     */
    @GetMapping
    public ResponseEntity<GlobalResponseHandler<Page<MemberSummaryVo>>> getAllMembers(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.info("REST: Getting all members - page: {}, size: {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        // Facade를 통한 전체 조회
        Page<MemberSummaryVo> result = memberFacadeService.getAllMembers(pageable);
        
        log.info("REST: All members retrieved - found {} results", result.getTotalElements());
        
        return GlobalResponseHandler.success(ResponseStatus.MEMBER_SEARCH_SUCCESS, result);
    }
}