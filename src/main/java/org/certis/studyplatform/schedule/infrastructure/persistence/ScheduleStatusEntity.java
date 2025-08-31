package org.certis.studyplatform.schedule.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Schedule Status Entity
 *
 * 일정 상태를 저장하는 JPA Entity
 */
@Entity
@Table(name = "schedule_status")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ScheduleStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "status", nullable = false)
    private String status;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder(toBuilder = true)
    private ScheduleStatusEntity(Long id, Long scheduleId, String status, OffsetDateTime updatedAt) {
        this.id = id;
        this.scheduleId = scheduleId;
        this.status = status;
        this.updatedAt = updatedAt;
    }
}