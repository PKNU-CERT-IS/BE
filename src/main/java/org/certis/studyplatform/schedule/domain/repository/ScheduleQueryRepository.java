package org.certis.studyplatform.schedule.domain.repository;

import org.certis.studyplatform.member.domain.vo.MemberIdVo;
import org.certis.studyplatform.schedule.application.object.query.GetAllApprovedScheduleRequestsQuery;
import org.certis.studyplatform.schedule.application.object.query.GetMyRequestsQuery;
import org.certis.studyplatform.schedule.application.object.query.GetPendingScheduleRequestsQuery;
import org.certis.studyplatform.schedule.domain.model.vo.AdminScheduleVo;
import org.certis.studyplatform.schedule.domain.model.vo.ScheduleDateVo;
import org.certis.studyplatform.schedule.domain.model.vo.ScheduleIdVo;
import org.certis.studyplatform.schedule.domain.model.vo.ScheduleVo;

import java.util.List;
import java.util.Optional;

public interface ScheduleQueryRepository {

    // 스케줄 ID로 조회
    Optional<ScheduleVo> findById(ScheduleIdVo scheduleId);

    // 모든 승인된 스케줄 조회 (월별, 캘린더용)
    List<ScheduleVo> findAllApprovedByMonth(ScheduleDateVo scheduleDateVo);

    // 회원의 모든 요청 조회
    List<ScheduleVo> findAllByMemberId(MemberIdVo memberIdVo);

    // 대기중인 스케줄 요청 조회 (어드민용) - 회원정보 포함
    List<AdminScheduleVo> findPendingSchedulesWithMemberInfo(MemberIdVo memberIdVo);

    // 스케줄 존재 여부 확인
    boolean existsById(ScheduleIdVo scheduleId);

    //  스케줄이 PENDING 상태인지 확인
    boolean isPendingStatus(ScheduleIdVo scheduleId);

    // 오늘 종료되는 스케줄의 시간 목록 조회 (프로필용)
    List<java.time.OffsetDateTime> findTodaySchedulesByMemberId(MemberIdVo memberIdVo);
}
