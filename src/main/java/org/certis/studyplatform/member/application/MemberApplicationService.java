//package org.certis.studyplatform.member.application;
//
//import lombok.RequiredArgsConstructor;
//import org.certis.studyplatform.member.domain.model.Member;
//import org.certis.studyplatform.member.domain.repository.MemberRepository;
//import org.certis.studyplatform.member.domain.model.vo.*;
//import org.certis.studyplatform.member.presentation.dto.request.CreateMemberRequestDto;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//@Service
//@Transactional
//@RequiredArgsConstructor
//public class MemberApplicationService {
//
////    private final MemberRepository memberRepository;
//
//    // TODO: 레이어 변경 필요
////    @Transactional
////    public Member createMember(CreateMemberRequestDto request) {
////        StudentNumberVo studentNumberVo = new StudentNumberVo(request.getStudentNumber());
////
////        // 1. 서비스 고유 로직 수행 (중복 학번 확인)
////        memberRepository.findByStudentNumber(studentNumberVo)
////                .ifPresent(existingMember -> {
////                    throw new IllegalArgumentException("이미 존재하는 학번입니다.");
////                });
////
////        // 2. 도메인 객체 생성 위임
////        // DTO로부터 받은 검증된 데이터를 사용하여 도메인 객체를 생성합니다.
////        Member member = new Member(
////                request.getName(),
////                request.getStudentNumber(),
////                request.getGrade(),
////                request.getRole(),
////                request.getMajor()
////        );
////
////        // 3. 영속화
////        return memberRepository.save(member);
////    }
//
//    public void updateMemberProfile(Long memberId, String name, String profileImageUrl) {
//        Member member = memberRepository.findById(new MemberIdVo(memberId))
//                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));
//
//        ProfileImageVo profileImage = profileImageUrl != null ? new ProfileImageVo(profileImageUrl) : null;
//        member.updateProfile(name, profileImage);
//
//        memberRepository.save(member);
//    }
//
//    public void updateMemberSkills(Long memberId, List<String> skillList) {
//        Member member = memberRepository.findById(new MemberIdVo(memberId))
//                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));
//
//        SkillsVo skills = SkillsVo.of(skillList);
//        member.updateSkills(skills);
//
//        memberRepository.save(member);
//    }
//}