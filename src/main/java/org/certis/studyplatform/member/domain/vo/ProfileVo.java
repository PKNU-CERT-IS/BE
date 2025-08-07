package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.member.domain.Profile;

import java.time.ZonedDateTime;

/**
 * 프로필 Value Object (Record)
 * Domain Layer에서 사용하는 불변 데이터 객체
 *
 * Record를 사용하여 불변성, equals, hashCode, toString 자동 제공
 */
public record ProfileVo(
        Long id,
        Long memberId,
        String name,
        String description,
        String profileImage,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
) {

    /**
     * Profile Domain Entity에서 ProfileVo 생성
     */
    public static ProfileVo from(Profile profile) {
        return new ProfileVo(
                profile.getId(),
                profile.getMemberId(),
                profile.getName(),
                profile.getDescription(),
                profile.getProfileImageValue(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
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
     * 최근에 업데이트되었는지 확인 (7일 이내)
     */
    public boolean isRecentlyUpdated() {
        if (updatedAt == null) return false;
        ZonedDateTime weekAgo = ZonedDateTime.now().minusDays(7);
        return updatedAt.isAfter(weekAgo);
    }

    /**
     * 새로 생성된 프로필인지 확인 (30일 이내)
     */
    public boolean isRecentlyCreated() {
        if (createdAt == null) return false;
        ZonedDateTime monthAgo = ZonedDateTime.now().minusDays(30);
        return createdAt.isAfter(monthAgo);
    }
} 