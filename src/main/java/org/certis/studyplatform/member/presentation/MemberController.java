package org.certis.studyplatform.member.presentation;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.member.application.MemberApplicationService;
import org.certis.studyplatform.member.domain.model.Member;
import org.certis.studyplatform.member.presentation.dto.CreateMemberRequest;
import org.certis.studyplatform.member.presentation.dto.MemberResponse;
import org.certis.studyplatform.member.presentation.dto.UpdateMemberProfileRequest;
import org.certis.studyplatform.member.presentation.dto.UpdateMemberSkillsRequest;
import org.certis.studyplatform.member.presentation.dto.request.CreateMemberRequestDto;
import org.certis.studyplatform.member.presentation.dto.response.MemberResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberApplicationService memberApplicationService;

    @PostMapping
    public ResponseEntity<MemberResponseDto> createMember(@Valid @RequestBody CreateMemberRequestDto request) {
        Member newMember = memberApplicationService.createMember(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponseDto.fromDomain(newMember));
    }

    @PutMapping("/{memberId}/profile")
    public ResponseEntity<Void> updateMemberProfile(
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberProfileRequest request) {

        memberApplicationService.updateMemberProfile(
                memberId,
                request.name(),
                request.profileImageUrl()
        );

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{memberId}/skills")
    public ResponseEntity<Void> updateMemberSkills(
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberSkillsRequest request) {

        memberApplicationService.updateMemberSkills(memberId, request.skills());
        return ResponseEntity.ok().build();
    }
}