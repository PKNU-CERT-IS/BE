package org.certis.studyplatform.study.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.MemberGrade;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Study Value Object
 *
 * Clean Architecture Domain Layer
 * 불변 데이터 전송 객체
 * StudyDetailResponseDto와 매칭되는 완전한 프로젝트 정보
 */
public record StudyVo(
        Long id,
        String title,
        String description,
        String content,
        String category,
        String subCategory,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long creatorId,
        String creatorName,
        MemberGrade creatorGrade,
        Integer maxParticipants,
        Integer currentParticipants,
        List<StudyAttachedVo> attached,
        List<StudyMeetingSummaryVo> summaryVoList,
        List<StudyParticipantVo> participantVoList
) {

    /**
     * Compact constructor with validation
     */
    public StudyVo {
        // 제목 검증
        if (title == null || title.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_TITLE, "프로젝트 제목은 필수입니다");
        }

        // 기간 검증
        if (startDate == null || endDate == null) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_DATE, "프로젝트 시작일과 종료일은 필수입니다");
        }

        if (startDate.isAfter(endDate)) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_DATE, "시작일은 종료일보다 빨라야 합니다");
        }

        // 과거 날짜 검증 (id가 null인 경우만 - 새로 생성하는 경우)
        if (id == null && startDate.isBefore(OffsetDateTime.now())) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_DATE, "프로젝트 시작일은 현재보다 미래여야 합니다");
        }

        // 최대 참가자 수 검증
        if (maxParticipants == null || maxParticipants < 1) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_PARTICIPANTS, "최대 참가자 수는 1명 이상이어야 합니다");
        }

        // 현재 참가자 수 검증
        if (currentParticipants != null && currentParticipants < 0) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_PARTICIPANTS, "현재 참가자 수는 0명 이상이어야 합니다");
        }

        // 현재 참가자 수가 최대 참가자 수를 초과하는지 검증
        if (currentParticipants != null && currentParticipants > maxParticipants) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_PARTICIPANTS, "현재 참가자 수는 최대 참가자 수를 초과할 수 없습니다");
        }

        // Creator ID 검증
        if (creatorId == null) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_CREATOR, "프로젝트 생성자 ID는 필수입니다");
        }
    }

    /**
     * 기본 팩토리 메서드 (스터디 핵심 정보로 VO 생성)
     * 목록 정보는 빈 리스트로 초기화됩니다.
     */
    public static StudyVo of(
            Long id,
            String title,
            String description,
            String content,
            String category,
            String subCategory,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            Long creatorId,
            String creatorName,
            MemberGrade creatorGrade,
            Integer maxParticipants,
            Integer currentParticipants
    ) {
        return new StudyVo(
                id, title, description, content, category, subCategory,
                startDate, endDate, createdAt, updatedAt,
                creatorId, creatorName, creatorGrade,
                maxParticipants, currentParticipants,
                Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList()
        );
    }

    /**
     * 새 스터디 생성용 팩토리 메서드
     */
    public static StudyVo createNew(
            String title,
            String description,
            String content,
            String category,
            String subCategory,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Long creatorId,
            String creatorName,
            MemberGrade creatorGrade,
            Integer maxParticipants
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        return new StudyVo(
                null, // id는 null (새 생성)
                title, description, content, category, subCategory,
                startDate, endDate, now, now, // createdAt, updatedAt
                creatorId, creatorName, creatorGrade,
                maxParticipants, 0, // 초기 참가자는 0명
                Collections.emptyList(),
                Collections.emptyList(), // 초기 회의록 목록은 비어 있음
                Collections.emptyList()  // 초기 참가자 목록은 비어 있음
        );
    }

    /**
     * 업데이트용 팩토리 메서드
     */
    public static StudyVo updateFrom(StudyVo existing,
                                     String title,
                                     String description,
                                     String content,
                                     String category,
                                     String subCategory,
                                     OffsetDateTime startDate,
                                     OffsetDateTime endDate,
                                     Integer maxParticipants) {
        return new StudyVo(
                existing.id(),
                title != null ? title : existing.title(),
                description != null ? description : existing.description(),
                content != null ? content : existing.content(),
                category != null ? category : existing.category(),
                subCategory != null ? subCategory : existing.subCategory(),
                startDate != null ? startDate : existing.startDate(),
                endDate != null ? endDate : existing.endDate(),
                existing.createdAt(), // 기존 createdAt 유지
                OffsetDateTime.now(), // updatedAt은 현재 시간으로 갱신
                existing.creatorId(),
                existing.creatorName(),
                existing.creatorGrade(), // 기존 creatorGrade 유지
                maxParticipants != null ? maxParticipants : existing.maxParticipants(),
                existing.currentParticipants(),
                existing.attached(),
                existing.summaryVoList(),    // 기존 summaryVoList 유지
                existing.participantVoList() // 기존 participantVoList 유지

        );
    }
}
