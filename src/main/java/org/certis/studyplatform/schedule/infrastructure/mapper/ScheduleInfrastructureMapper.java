package org.certis.studyplatform.schedule.infrastructure.mapper;

import org.certis.studyplatform.schedule.domain.model.vo.AdminScheduleVo;
import org.certis.studyplatform.schedule.domain.model.vo.ScheduleVo;
import org.certis.studyplatform.schedule.infrastructure.persistence.ScheduleEntity;
import org.certis.studyplatform.schedule.infrastructure.persistence.ScheduleStatusEntity;
import org.springframework.stereotype.Component;
import org.jooq.Record;

import java.time.OffsetDateTime;

@Component
public class ScheduleInfrastructureMapper {

    // =================================================================
    // Domain VO → Entity 변환 (JPA 저장 시 사용)
    // =================================================================

    /**
     * ScheduleVo → ScheduleEntity 변환
     * ScheduleVo에 memberId가 포함되어 있어야 함
     */
    public ScheduleEntity toEntity(ScheduleVo scheduleVo) {
        return ScheduleEntity.builder()
                .id(scheduleVo.id())
                .memberId(scheduleVo.memberId())  // ScheduleVo에서 memberId 가져오기
                .type(scheduleVo.type())
                .title(scheduleVo.title())
                .description(scheduleVo.description())
                .place(scheduleVo.place())
                .startedAt(scheduleVo.startedAt())
                .endedAt(scheduleVo.endedAt())
                .build();
    }

    /**
     * 스케줄 상태 Entity 생성
     */
    public ScheduleStatusEntity toStatusEntity(Long scheduleId, String status) {
        return ScheduleStatusEntity.builder()
                .scheduleId(scheduleId)
                .status(status)
                .build();
    }

    // =================================================================
    // Entity → Domain VO 변환 (JPA 조회 시 사용)
    // =================================================================

    /**
     * ScheduleEntity → ScheduleVo 변환
     */
    public ScheduleVo toScheduleVo(ScheduleEntity entity, String status) {
        return ScheduleVo.of(
                entity.getId(),
                entity.getMemberId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getType(),
                entity.getPlace(),
                entity.getStartedAt(),
                entity.getEndedAt(),
                status,
                entity.getCreatedAt()
        );
    }

    // =================================================================
    // jOOQ Record → Domain VO 변환 (읽기 전용 조회 시 사용)
    // =================================================================

    /**
     * jOOQ Record → ScheduleVo 변환
     */
    public ScheduleVo toScheduleVo(Record record) {
        return ScheduleVo.of(
                record.get("id", Long.class),
                record.get("member_id", Long.class),
                record.get("title", String.class),
                record.get("description", String.class),
                record.get("type", String.class),
                record.get("place", String.class),
                record.get("started_at", OffsetDateTime.class),
                record.get("ended_at", OffsetDateTime.class),
                record.get("status", String.class),
                record.get("created_at", OffsetDateTime.class)
        );
    }

    /**
     * jOOQ Record → AdminScheduleVo 변환 (복잡한 조인 결과)
     */
    public AdminScheduleVo toAdminScheduleVo(Record record) {
        return AdminScheduleVo.of(
                record.get("schedule_id", Long.class),
                record.get("title", String.class),
                record.get("description", String.class),
                record.get("type", String.class),
                record.get("place", String.class),
                record.get("started_at", OffsetDateTime.class),
                record.get("ended_at", OffsetDateTime.class),
                record.get("status", String.class),
                record.get("created_at", OffsetDateTime.class),
                record.get("member_id", Long.class),
                record.get("member_name", String.class)
        );
    }
}
