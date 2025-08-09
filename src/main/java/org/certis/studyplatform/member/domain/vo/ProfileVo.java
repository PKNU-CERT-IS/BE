package org.certis.studyplatform.member.domain.vo;

import java.time.OffsetDateTime;

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
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * 정적 팩토리 메서드
     */
    public static ProfileVo of(Long id, Long memberId, String name, String description, 
                               String profileImage, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return new ProfileVo(id, memberId, name, description, profileImage, createdAt, updatedAt);
    }
    
    // =================================================================
    // Getter 메서드들
    // =================================================================
    
    /**
     * ID 반환
     */
    public Long getId() {
        return id;
    }
    
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
     * 수정일시 반환
     */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
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
        OffsetDateTime weekAgo = OffsetDateTime.now().minusDays(7);
        return updatedAt.isAfter(weekAgo);
    }

    /**
     * 새로 생성된 프로필인지 확인 (30일 이내)
     */
    public boolean isRecentlyCreated() {
        if (createdAt == null) return false;
        OffsetDateTime monthAgo = OffsetDateTime.now().minusDays(30);
        return createdAt.isAfter(monthAgo);
    }
} 