package org.certis.studyplatform.schedule.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.exception.InfrastructureException;
import org.certis.studyplatform.schedule.domain.model.vo.ScheduleIdVo;
import org.certis.studyplatform.schedule.domain.model.vo.ScheduleStatusVo;
import org.certis.studyplatform.schedule.domain.model.vo.ScheduleVo;
import org.certis.studyplatform.schedule.domain.repository.ScheduleCommandRepository;
import org.certis.studyplatform.schedule.infrastructure.mapper.ScheduleInfrastructureMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
@RequiredArgsConstructor
@Slf4j
public class ScheduleCommandRepositoryImpl implements ScheduleCommandRepository {

    private final ScheduleJpaRepository scheduleJpaRepository;
    private final ScheduleStatusJpaRepository scheduleStatusJpaRepository;
    private final ScheduleInfrastructureMapper scheduleInfrastructureMapper;

    @Override
    @Transactional
    public ScheduleVo createSchedule(ScheduleVo scheduleVo) {
        log.debug("Infrastructure: Creating schedule with title: {}", scheduleVo.title());

        try {
            ScheduleEntity entityToSave = scheduleInfrastructureMapper.toEntity(scheduleVo);
            ScheduleEntity savedEntity = scheduleJpaRepository.save(entityToSave);

            log.debug("Infrastructure: Schedule created successfully with ID: {}", savedEntity.getId());
            return scheduleInfrastructureMapper.toScheduleVo(savedEntity, null);

        } catch (Exception e) {
            log.error("Infrastructure: Failed to create schedule: {}", e.getMessage(), e);
            throw new InfrastructureException(ExceptionStatus.SCHEDULE_INFRASTRUCTURE_DATABASE_ERROR);
        }
    }

    @Override
    @Transactional
    public void deleteById(ScheduleIdVo scheduleId) {
        log.debug("Infrastructure: Deleting schedule with ID: {}", scheduleId.value());

        try {
            if (!scheduleJpaRepository.existsById(scheduleId.value())) {
                throw new InfrastructureException(ExceptionStatus.SCHEDULE_INFRASTRUCTURE_DATABASE_ERROR,
                        "Schedule not found: " + scheduleId.value());
            }

            scheduleJpaRepository.deleteById(scheduleId.value());
            log.debug("Infrastructure: Schedule deleted successfully with ID: {}", scheduleId.value());

        } catch (Exception e) {
            log.error("Infrastructure: Failed to delete schedule {}: {}", scheduleId.value(), e.getMessage(), e);
            throw new InfrastructureException(ExceptionStatus.SCHEDULE_INFRASTRUCTURE_STATUS_DATABASE_ERROR,
                    "Failed to delete schedule: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void createScheduleStatus(ScheduleIdVo scheduleId, ScheduleStatusVo status) {
        log.debug("Infrastructure: Creating schedule status - Schedule ID: {}, Status: {}",
                scheduleId.value(), status.value());

        try {
            ScheduleStatusEntity statusEntity = scheduleInfrastructureMapper
                    .toStatusEntity(scheduleId.value(), status.value());

            scheduleStatusJpaRepository.save(statusEntity);
            log.debug("Infrastructure: Schedule status created successfully");

        } catch (Exception e) {
            log.error("Infrastructure: Failed to create schedule status: {}", e.getMessage(), e);
            throw new InfrastructureException(ExceptionStatus.SCHEDULE_INFRASTRUCTURE_STATUS_DATABASE_ERROR,
                    "Failed to create schedule status: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateScheduleStatus(ScheduleIdVo scheduleId, ScheduleStatusVo newStatus) {
        log.debug("Infrastructure: Updating schedule status - Schedule ID: {}, New Status: {}",
                scheduleId.value(), newStatus.value());

        try {
            ScheduleStatusEntity existingStatus = scheduleStatusJpaRepository
                    .findByScheduleId(scheduleId.value())
                    .orElseThrow(() -> new InfrastructureException(
                            ExceptionStatus.SCHEDULE_INFRASTRUCTURE_STATUS_NOT_FOUND,
                            "Schedule status not found: " + scheduleId.value()));

            ScheduleStatusEntity updatedStatus = existingStatus.toBuilder()
                    .status(newStatus.value())
                    .build();

            scheduleStatusJpaRepository.save(updatedStatus);
            log.debug("Infrastructure: Schedule status updated successfully");

        } catch (Exception e) {
            log.error("Infrastructure: Failed to update schedule status: {}", e.getMessage(), e);
            throw new InfrastructureException(ExceptionStatus.SCHEDULE_INFRASTRUCTURE_DATABASE_ERROR,
                    "Failed to update schedule status: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteScheduleStatusByScheduleId(ScheduleIdVo scheduleId) {
        log.debug("Infrastructure: Deleting schedule status for Schedule ID: {}", scheduleId.value());

        try {
            scheduleStatusJpaRepository.deleteByScheduleId(scheduleId.value());
            log.debug("Infrastructure: Schedule status deleted successfully");

        } catch (Exception e) {
            log.error("Infrastructure: Failed to delete schedule status: {}", e.getMessage(), e);
            throw new InfrastructureException(ExceptionStatus.SCHEDULE_INFRASTRUCTURE_STATUS_DATABASE_ERROR,
                    "Failed to delete schedule status: " + e.getMessage());
        }
    }
}