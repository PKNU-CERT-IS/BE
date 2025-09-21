package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.domain.MemberRole;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 프로필 Value Object (Record)
 * Domain Layer에서 사용하는 불변 데이터 객체
 *
 * Record를 사용하여 불변성, equals, hashCode, toString 자동 제공
 */
public record ProfileVo(
        Long memberId,
        String name,
        String description,
        String profileImage,
        List<ScheduleInfoVo> todaySchedules,
        Integer penaltyCount,
        OffsetDateTime gracePeriod,
        MemberRole memberRole,
        MemberGrade memberGrade,
        List<String> skills,
        OffsetDateTime createdAt,
        
        // Enhanced profile fields
        String major,
        OffsetDateTime birthday,
        String phoneNumber,
        String studentNumber,
        String email,
        String githubUrl,
        String linkedUrl
) {

    /**
     * 방어적 복사를 위한 정규화 생성자
     */
    public ProfileVo {
        // List들의 불변성 보장
        todaySchedules = todaySchedules != null ? List.copyOf(todaySchedules) : List.of();
        skills = skills != null ? List.copyOf(skills) : List.of();
    }

    /**
     * 정적 팩토리 메서드
     */
    public static ProfileVo of(Long memberId, String name, String description,
                               String profileImage, List<ScheduleInfoVo> todaySchedules,
                               Integer penaltyCount, OffsetDateTime gracePeriod,
                               MemberRole memberRole,
                               MemberGrade memberGrade,
                               List<String> skills,
                               OffsetDateTime createdAt,
                               String major, OffsetDateTime birthday, String phoneNumber,
                               String studentNumber, String email, String githubUrl, String linkedUrl) {
        return new ProfileVo(memberId, name, description, profileImage, todaySchedules,
                penaltyCount, gracePeriod, memberRole, memberGrade, skills, createdAt,
                major, birthday, phoneNumber, studentNumber, email, githubUrl, linkedUrl);
    }

    // =================================================================
    // Getter 메서드들
    // =================================================================

    /**
     * 회원 ID 반환
     */
    public Long getMemberId() {
        return memberId;
    }

    /**
     * 이름 반환
     */
    public String getName() {
        return name;
    }

    /**
     * 설명 반환
     */
    public String getDescription() {
        return description;
    }

    /**
     * 프로필 이미지 반환
     */
    public String getProfileImage() {
        return profileImage;
    }

    /**
     * 생성일시 반환
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 프로필 이미지가 있는지 확인
     */
    public boolean hasProfileImage() {
        return profileImage != null && !profileImage.trim().isEmpty();
    }

    /**
     * 설명이 있는지 확인
     */
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    /**
     * 새로 생성된 프로필인지 확인 (30일 이내)
     */
    public boolean isRecentlyCreated() {
        if (createdAt == null) return false;
        OffsetDateTime monthAgo = OffsetDateTime.now().minusDays(30);
        return createdAt.isAfter(monthAgo);
    }

    /**
     * 특정 기술을 보유하고 있는지 확인
     */
    public boolean hasSkill(String skill) {
        return skills.contains(skill);
    }

    /**
     * 유예기간이 활성화되어 있는지 확인
     */
    public boolean isInGracePeriod() {
        if (gracePeriod == null) return false;
        return OffsetDateTime.now().isBefore(gracePeriod);
    }

    /**
     * 유예기간이 만료되었는지 확인
     */
    public boolean isGracePeriodExpired() {
        if (gracePeriod == null) return true;
        return OffsetDateTime.now().isAfter(gracePeriod);
    }
}