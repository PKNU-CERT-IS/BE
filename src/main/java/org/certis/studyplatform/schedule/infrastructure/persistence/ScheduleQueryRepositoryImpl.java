package org.certis.studyplatform.schedule.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.exception.InfrastructureException;
import org.certis.studyplatform.member.domain.vo.MemberIdVo;
import org.certis.studyplatform.schedule.domain.model.vo.AdminScheduleVo;
import org.certis.studyplatform.schedule.domain.model.vo.ScheduleDateVo;
import org.certis.studyplatform.schedule.domain.model.vo.ScheduleIdVo;
import org.certis.studyplatform.schedule.domain.model.vo.ScheduleVo;
import org.certis.studyplatform.schedule.domain.repository.ScheduleQueryRepository;
import org.certis.studyplatform.schedule.infrastructure.mapper.ScheduleInfrastructureMapper;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.extract;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ScheduleQueryRepositoryImpl implements ScheduleQueryRepository {

    private final DSLContext dsl;
    private final ScheduleInfrastructureMapper scheduleInfrastructureMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<ScheduleVo> findById(ScheduleIdVo scheduleId) {
        log.debug("Infrastructure: Finding schedule by ID: {}", scheduleId.value());

        try {
            @NotNull Result<Record10<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>> records = dsl
                    .select(
                            field("s.id"),
                            field("s.member_id"),
                            field("s.title"),
                            field("s.description"),
                            field("s.type"),
                            field("s.place"),
                            field("s.started_at"),
                            field("s.ended_at"),
                            field("s.created_at"),
                            field("ss.status")
                    )
                    .from(table("schedule").as("s"))
                    .leftJoin(table("schedule_status").as("ss"))
                    .on(field("s.id").eq(field("ss.schedule_id")))
                    .where(field("s.id").eq(scheduleId.value())
                            .and(field("s.deleted_at").isNull()))
                    .fetch();

            if (records.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(scheduleInfrastructureMapper.toScheduleVo(records.get(0)));

        } catch (Exception e) {
            log.error("Infrastructure: Failed to find schedule by ID {}: {}", scheduleId.value(), e.getMessage(), e);
            throw new InfrastructureException(ExceptionStatus.SCHEDULE_INFRASTRUCTURE_DATABASE_ERROR,
                    "Failed to find schedule: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleVo> findAllApprovedByMonth(ScheduleDateVo scheduleDateVo) {
        log.debug("Infrastructure: Finding all approved schedules for date: {}", scheduleDateVo.value());

        try {
            OffsetDateTime targetDate = scheduleDateVo.value();

            @NotNull Result<Record10<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>> records = dsl
                    .select(
                            field("s.id"),
                            field("s.member_id"),
                            field("s.title"),
                            field("s.description"),
                            field("s.type"),
                            field("s.place"),
                            field("s.started_at"),
                            field("s.ended_at"),
                            field("s.created_at"),
                            field("ss.status")
                    )
                    .from(table("schedule").as("s"))
                    .join(table("schedule_status").as("ss"))
                    .on(field("s.id").eq(field("ss.schedule_id")))
                    .where(field("ss.status").eq("APPROVED")
                            .and(extract(field("s.started_at"), DatePart.YEAR).eq(targetDate.getYear()))
                            .and(extract(field("s.started_at"), DatePart.MONTH).eq(targetDate.getMonthValue()))
                            .and(field("s.deleted_at").isNull()))
                    .orderBy(field("s.started_at").asc())
                    .fetch();

            return records.stream()
                    .map(scheduleInfrastructureMapper::toScheduleVo)
                    .toList();

        } catch (Exception e) {
            log.error("Infrastructure: Failed to find approved schedules: {}", e.getMessage(), e);
            throw new InfrastructureException(ExceptionStatus.SCHEDULE_INFRASTRUCTURE_DATABASE_ERROR,
                    "Failed to find approved schedules: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleVo> findAllByMemberId(MemberIdVo memberIdVo) {
        log.debug("Infrastructure: Finding schedules for member ID: {}", memberIdVo.value());

        try {
            @NotNull Result<Record10<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>> records = dsl
                    .select(
                            field("s.id"),
                            field("s.member_id"),
                            field("s.title"),
                            field("s.description"),
                            field("s.type"),
                            field("s.place"),
                            field("s.started_at"),
                            field("s.ended_at"),
                            field("s.created_at"),
                            field("ss.status")
                    )
                    .from(table("schedule").as("s"))
                    .leftJoin(table("schedule_status").as("ss"))
                    .on(field("s.id").eq(field("ss.schedule_id")))
                    .where(field("s.member_id").eq(memberIdVo.value())
                            .and(field("s.deleted_at").isNull()))
                    .orderBy(field("s.created_at").desc())
                    .fetch();

            return records.stream()
                    .map(scheduleInfrastructureMapper::toScheduleVo)
                    .toList();

        } catch (Exception e) {
            log.error("Infrastructure: Failed to find schedules for member {}: {}", memberIdVo.value(), e.getMessage(), e);
            throw new InfrastructureException(ExceptionStatus.SCHEDULE_INFRASTRUCTURE_DATABASE_ERROR,
                    "Failed to find member schedules: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminScheduleVo> findPendingSchedulesWithMemberInfo(MemberIdVo memberIdVo) {
        log.debug("Infrastructure: Finding pending schedules for admin ID: {}", memberIdVo.value());

        try {
            @NotNull Result<Record11<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>> records = dsl
                    .select(
                            field("s.id").as("schedule_id"),
                            field("s.title"),
                            field("s.description"),
                            field("s.type"),
                            field("s.place"),
                            field("s.started_at"),
                            field("s.ended_at"),
                            field("s.created_at"),
                            field("ss.status"),
                            field("m.id").as("member_id"),
                            field("m.name").as("member_name")
                    )
                    .from(table("schedule").as("s"))
                    .join(table("schedule_status").as("ss"))
                    .on(field("s.id").eq(field("ss.schedule_id")))
                    .join(table("member").as("m"))
                    .on(field("s.member_id").eq(field("m.id")))
                    .where(field("ss.status").eq("PENDING")
                            .and(field("s.deleted_at").isNull())
                            .and(field("m.deleted_at").isNull()))
                    .orderBy(field("s.created_at").asc())
                    .fetch();

            return records.stream()
                    .map(scheduleInfrastructureMapper::toAdminScheduleVo)
                    .toList();

        } catch (Exception e) {
            log.error("Infrastructure: Failed to find pending schedules: {}", e.getMessage(), e);
            throw new InfrastructureException(ExceptionStatus.SCHEDULE_INFRASTRUCTURE_DATABASE_ERROR,
                    "Failed to find pending schedules: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(ScheduleIdVo scheduleId) {
        log.debug("Infrastructure: Checking existence of schedule ID: {}", scheduleId.value());

        try {
            Integer count = dsl
                    .selectCount()
                    .from(table("schedule"))
                    .where(field("id").eq(scheduleId.value())
                            .and(field("deleted_at").isNull()))
                    .fetchSingle()
                    .value1();

            return count != null && count > 0;

        } catch (Exception e) {
            log.error("Infrastructure: Failed to check schedule existence {}: {}", scheduleId.value(), e.getMessage(), e);
            throw new InfrastructureException(ExceptionStatus.SCHEDULE_INFRASTRUCTURE_DATABASE_ERROR,
                    "Failed to check schedule existence: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPendingStatus(ScheduleIdVo scheduleId) {
        log.debug("Infrastructure: Checking if schedule {} is in PENDING status", scheduleId.value());

        try {
            Integer count = dsl
                    .selectCount()
                    .from(table("schedule_status"))
                    .where(field("schedule_id").eq(scheduleId.value())
                            .and(field("status").eq("PENDING")))
                    .fetchSingle()
                    .value1();

            return count != null && count > 0;

        } catch (Exception e) {
            log.error("Infrastructure: Failed to check pending status for schedule {}: {}",
                    scheduleId.value(), e.getMessage(), e);
            throw new InfrastructureException(ExceptionStatus.SCHEDULE_INFRASTRUCTURE_DATABASE_ERROR,
                    "Failed to check pending status: " + e.getMessage());
        }
    }
}