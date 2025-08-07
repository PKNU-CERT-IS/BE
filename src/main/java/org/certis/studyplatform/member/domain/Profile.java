package org.certis.studyplatform.member.domain;

import org.certis.studyplatform.member.domain.vo.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

//TODO: Lombok Getter 가 접근 가능한지 알아보기
// => 원시타입만 가능함
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile {
    
    @Getter
    private Long memberId;
    @Getter
    private String name;
    @Getter
    private String description;

    private ProfileImageVo profileImageUrl;
    @Getter
    private ZonedDateTime createdAt;
    @Getter
    private ZonedDateTime updatedAt;

    // 새 프로필 생성
    public Profile(Long memberId, String name, String description, 
                   String profileImageUrl, Boolean isPublic) {
        this.memberId = memberId;
        this.name = name;
        this.description = description;
        this.profileImageUrl = ProfileImageVo.of(profileImageUrl);
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    // 기존 프로필 복원 (Repository 계층에서 사용)
    public Profile(Long memberId, String name, 
                   String description, ProfileImageVo profileImageUrl, 
                   ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        this.memberId = memberId;
        this.name = name;
        this.description = description;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Builder
    public static Profile create(Long memberId, String name, String description, 
                                ProfileImageVo profileImageUrl) {
        return new Profile(memberId, name, description, profileImageUrl, ZonedDateTime.now(), ZonedDateTime.now());
    }

    // 비즈니스 로직
    public void updateProfile(String description, String profileImageUrl) {
        if (description != null) {
            this.description = description;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = ProfileImageVo.of(profileImageUrl);
        }
        this.updatedAt = ZonedDateTime.now();
    }

    public void updateDescription(String description) {
        this.description = description;
        this.updatedAt = ZonedDateTime.now();
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = ProfileImageVo.of(profileImageUrl);
        this.updatedAt = ZonedDateTime.now();
    }

    public void updateVisibility(Boolean isPublic) {
        this.updatedAt = ZonedDateTime.now();
    }

    // Getters - Domain VOs 반환
    //public Long getId() { return id; }
    //public Long getMemberId() { return memberId; }
    //public DescriptionVo getDescription() { return description; }
    //public ProfileImageUrlVo getProfileImageUrl() { return profileImageUrl; }
    //public VisibilityVo getVisibility() { return visibility; }
    //public ZonedDateTime getCreatedAt() { return createdAt; }
    //public ZonedDateTime getUpdatedAt() { return updatedAt; }

    // Primitive 값 반환 메서드 (편의용)
    public String getProfileImageUrlValue() { return profileImageUrl != null ? profileImageUrl.value() : null; }
}