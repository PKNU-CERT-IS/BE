package org.certis.studyplatform.member.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.application.MemberFacadeService;
import org.certis.studyplatform.member.domain.Member;
import org.certis.studyplatform.member.presentation.dto.request.CreateMemberRequestDto;
import org.certis.studyplatform.member.presentation.dto.request.MemberSearchRequestDto;
import org.certis.studyplatform.member.presentation.dto.response.MemberSearchResponseDto;
import org.certis.studyplatform.member.presentation.dto.request.UpdateMemberProfileRequestDto;
import org.certis.studyplatform.member.presentation.dto.request.UpdateMemberSkillsRequestDto;
import org.certis.studyplatform.member.presentation.dto.response.MemberInfoResponseDto;
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
 * 의존성 흐름:
 * Presentation → Application (Facade) → Application (Services) → Domain → Infrastructure
 */
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Slf4j
public class MemberController {
    
    private final MemberFacadeService memberFacadeService;
    
    /**
     * 회원 생성
     * 
     * @param request 회원 생성 요청 DTO (Bean Validation으로 최초 검증)
     * @return 생성된 회원 정보
     */
    @PostMapping
    public ResponseEntity<GlobalResponseHandler<MemberInfoResponseDto>> createMember(
            @Valid @RequestBody CreateMemberRequestDto request) {
        log.info("REST: Creating member - {}", request.getName());
        
        // DTO 검증 (DTO에서는 기본적인 null/empty 체크만)
        if (!request.hasRequiredFields()) {
            throw new org.certis.studyplatform.exception.PresentationException(
                ExceptionStatus.PRESENTATION_VALIDATION_INVALID_REQUEST_DATA,
                "필수 필드가 누락되었습니다"
            );
        }
        
        // Facade를 통한 생성 실행
        Member createdMember = memberFacadeService.createMember(
                request.getName(),
                request.getStudentNumber(),
                request.getGrade(),
                request.getSkills(),
                request.getRole(),
                request.getMajor(),
                request.getDescription()
        );
        
        // Domain Entity를 Response DTO로 변환
        MemberInfoResponseDto response = MemberInfoResponseDto.builder()
                .id(createdMember.getId() != null ? createdMember.getId() : null)
                .name(createdMember.getName().value())
                .description(createdMember.getDescription())
                .studentNumber(createdMember.getStudentNumber().value())
                .grade(createdMember.getGrade().value())
                .role(createdMember.getRole())
                .major(createdMember.getMajor().value())
                .skills(createdMember.getSkills().values())
                .profileImage(createdMember.getProfileImage() != null ? createdMember.getProfileImage().value() : null)
                .createdAt(createdMember.getCreatedAt())
                .updatedAt(createdMember.getUpdatedAt())
                .build();
        
        log.info("REST: Member created successfully - ID: {}", response.getId());
        
        return GlobalResponseHandler.success(ResponseStatus.MEMBER_CREATE_SUCCESS, response);
    }
    
    /**
     * 회원 상세 조회
     * 
     * @param id 회원 ID
     * @return 회원 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<GlobalResponseHandler<MemberInfoResponseDto>> getMember(@PathVariable Long id) {
        log.info("REST: Getting member - {}", id);
        
        // Facade를 통한 조회
        Member member = memberFacadeService.getMemberById(id);
        
        // Domain Entity를 Response DTO로 변환
        MemberInfoResponseDto response = MemberInfoResponseDto.builder()
                .id(member.getId())
                .name(member.getName().value())
                .description(member.getDescription())
                .studentNumber(member.getStudentNumber().value())
                .grade(member.getGrade().value())
                .role(member.getRole())
                .major(member.getMajor().value())
                .skills(member.getSkills().values())
                .profileImage(member.getProfileImage() != null ? member.getProfileImage().value() : null)
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
        
        return GlobalResponseHandler.success(ResponseStatus.MEMBER_FIND_SUCCESS, response);
    }
    
    /**
     * 회원 프로필 수정
     * 
     * @param id 회원 ID
     * @param request 프로필 수정 요청 DTO
     * @return 성공 응답
     */
    @PutMapping("/{id}/profile")
    public ResponseEntity<GlobalResponseHandler<Void>> updateMemberProfile(
            @PathVariable Long id, 
            @Valid @RequestBody UpdateMemberProfileRequestDto request) {
        log.info("REST: Updating member profile - {}", id);
        
        // Facade를 통한 수정
        memberFacadeService.updateMemberProfile(id, request.getName(), request.getProfileImageUrl());
        
        return GlobalResponseHandler.success(ResponseStatus.MEMBER_PROFILE_UPDATE_SUCCESS);
    }
    
    /**
     * 회원 기술 스택 수정
     * 
     * @param id 회원 ID
     * @param request 기술 스택 수정 요청 DTO
     * @return 성공 응답
     */
    @PutMapping("/{id}/skills")
    public ResponseEntity<GlobalResponseHandler<Void>> updateMemberSkills(
            @PathVariable Long id, 
            @Valid @RequestBody UpdateMemberSkillsRequestDto request) {
        log.info("REST: Updating member skills - {}", id);
        
        // Facade를 통한 수정
        memberFacadeService.updateMemberSkills(id, request.getSkills());
        
        return GlobalResponseHandler.success(ResponseStatus.MEMBER_SKILLS_UPDATE_SUCCESS);
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
     * 회원 검색 (v1 API)
     * 
     * @param searchRequest 검색 조건 (grade, role, keyword)
     * @param pageable 페이징 정보 (page, size, sort)
     * @return 검색된 회원 목록과 페이징 정보
     */
    @GetMapping("/search")
    public ResponseEntity<GlobalResponseHandler<MemberSearchResponseDto>> searchMembers(
            MemberSearchRequestDto searchRequest,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.info("REST: Searching members - grade: {}, role: {}, keyword: {}, page: {}, size: {}", 
                searchRequest.getSafeGrade(), 
                searchRequest.getSafeRole(), 
                searchRequest.getSafeKeyword(),
                pageable.getPageNumber(),
                pageable.getPageSize());
        
        // 검색 조건이 없으면 BadRequest (DTO에서 기본 검증만 하므로 여기서 체크)
        if (!searchRequest.hasAnyFilter()) {
            log.warn("REST: No search criteria provided");
            // Global Exception Handler가 처리하도록 예외 발생
            throw new org.certis.studyplatform.exception.PresentationException(
                ExceptionStatus.PRESENTATION_VALIDATION_INVALID_REQUEST_DATA,
                "최소 하나의 검색 조건(grade, role, keyword)이 필요합니다"
            );
        }
        
        // Facade를 통한 검색 실행
        Page<Member> result = memberFacadeService.searchMembers(
                searchRequest.getSafeKeyword(),
                searchRequest.getSafeGrade(),
                searchRequest.getRole(),
                pageable
        );
        
        // Domain Entity를 Presentation DTO로 변환
        MemberSearchResponseDto response = buildSearchResponse(result, searchRequest);
        
        log.info("REST: Search completed - found {} results", result.getTotalElements());
        
        return GlobalResponseHandler.success(ResponseStatus.MEMBER_SEARCH_SUCCESS, response);
    }
    
    /**
     * Page<Member>를 MemberSearchResponseDto로 변환
     * 
     * @param result Domain Layer 검색 결과
     * @param searchRequest 원본 검색 요청
     * @return Presentation Layer 응답 DTO
     */
    private MemberSearchResponseDto buildSearchResponse(Page<Member> result, MemberSearchRequestDto searchRequest) {
        // 회원 목록 변환
        var memberSummaries = result.getContent().stream()
                .map(member -> MemberSearchResponseDto.MemberSummaryDto.builder()
                        .id(member.getId())
                        .name(member.getName().value())
                        .studentNumber(member.getStudentNumber().value())
                        .grade(member.getGrade().value())
                        .role(member.getRole())
                        .major(member.getMajor().value())
                        .skills(member.getSkills().values())
                        .createdAt(member.getCreatedAt())
                        .build())
                .toList();
        
        // 페이지네이션 정보 구성
        var pageInfo = MemberSearchResponseDto.PageInfoDto.builder()
                .currentPage(result.getNumber())
                .pageSize(result.getSize())
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .hasNext(result.hasNext())
                .hasPrevious(result.hasPrevious())
                .build();
        
        // 검색 조건 정보 구성
        var searchInfo = MemberSearchResponseDto.SearchInfoDto.builder()
                .grade(searchRequest.getSafeGrade())
                .role(searchRequest.getSafeRole())
                .keyword(searchRequest.getSafeKeyword())
                .resultCount(result.getNumberOfElements())
                .build();
        
        return MemberSearchResponseDto.builder()
                .members(memberSummaries)
                .pageInfo(pageInfo)
                .searchInfo(searchInfo)
                .build();
    }
}