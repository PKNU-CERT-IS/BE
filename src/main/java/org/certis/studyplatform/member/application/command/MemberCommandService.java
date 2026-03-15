package org.certis.studyplatform.member.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.application.object.command.*;
import org.certis.studyplatform.member.domain.service.MemberDomainService;
import org.certis.studyplatform.member.domain.vo.AdminMemberUpdateResultVo;
import org.certis.studyplatform.member.domain.vo.MemberCreatedVo;
import org.certis.studyplatform.member.domain.vo.MemberUpdatedVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Member Command Service
 * - Command 객체를 받아 비즈니스 로직을 수행하고, 그 결과를 VO로 반환합니다.
 * - 오직 '쓰기(Write)' 작업과 관련된 책임만 가집니다.
 *
 * ✅ 업데이트 메서드 통합: 하나의 updateMember 메서드로 모든 수정 처리
 * ✅ 새로운 매퍼 시스템 사용: DomainToVoMapper
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MemberCommandService {

    private final MemberDomainService memberDomainService;

    /**
     * 회원 생성
     */
    @Transactional
    public MemberCreatedVo createMember(CreateMemberCommand command) {
        log.info("Command: Creating member with student number: {}", command.studentNumber());

        MemberCreatedVo savedMember = memberDomainService.createMember(command);

        log.info("✅ Member Command Service: Member created successfully with ID: {}",
                savedMember.id());

        return savedMember;
    }


    /**
     * 회원 삭제
     */
    @Transactional
    public void deleteMember(DeleteMemberCommand command) {
        log.info("Command: Deleting member with ID: {}", command.id());
        memberDomainService.deleteMember(command);
        log.info("Member deleted successfully: {}", command.id());
    }

    // 회원 정보 ( grade, role ) 수정
    // Admin
    @Transactional
    public AdminMemberUpdateResultVo updateMemberAdminFields(UpdateMemberAdminFieldsCommand command) {
        log.info("Command Service: 관리자 회원 필드 업데이트 시작 - 실행자: {}, 대상자: {}",
                command.executorId(), command.targetMemberId());

        // ✅ Domain Service 호출
        AdminMemberUpdateResultVo adminMemberUpdateResultVo = memberDomainService.updateMemberAdminFields(command);

        log.info("Command Service: 관리자 회원 필드 업데이트 완료 - 대상자: {}",
                command.targetMemberId());

        return adminMemberUpdateResultVo;
    }

    public void grantGracePeriod(UpdateGracePeriodCommand command) {
        log.info("Command: Granting grace period - memberId={}, gracePeriod={}",
                command.memberId(), command.gracePeriod());
        memberDomainService.grantGracePeriod(command);
        log.info("✅ Grace period granted - memberId={}", command.memberId());

    }

    public void assignPenalty(UpdatePenaltyCommand command) {
        log.info("Command: Assigning penalty - memberId={}, points={}",
                command.memberId(), command.penaltyPoints());
       memberDomainService.assignPenalty(command);
        log.info("✅ Penalty assigned - memberId={}, points={}",
                command.memberId(), command.penaltyPoints());
    }
}