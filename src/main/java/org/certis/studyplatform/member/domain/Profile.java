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
    private Long id;
    @Getter
    private Long memberId;
    @Getter
    private String name;
    @Getter
    private String description;

    private ProfileImageVo profileImage;
    @Getter
    private ZonedDateTime createdAt;
    @Getter
    private ZonedDateTime updatedAt;

    // 새 프로필 생성
    public Profile(Long memberId, String name, String description, 
                   String profileImage) {
        this.memberId = memberId;
        this.name = name;
        this.description = description;
        this.profileImage = ProfileImageVo.of(profileImage);
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    // 기존 프로필 복원 (Repository 계층에서 사용)
    public Profile(Long id, Long memberId, String name, 
                   String description, ProfileImageVo profileImage,
                   ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        this.id = id;
        this.memberId = memberId;
        this.name = name;
        this.description = description;
        this.profileImage = profileImage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Builder
    public static Profile create(Long memberId, String name, String description, 
                                ProfileImageVo profileImage) {
        return new Profile(memberId, name, description, profileImage != null ? profileImage.value() : null);
    }

    // 비즈니스 로직
    public void updateProfile(String description, String profileImage) {
        if (description != null) {
            this.description = description;
        }
        if (profileImage != null) {
            this.profileImage = ProfileImageVo.of(profileImage);
        }
        this.updatedAt = ZonedDateTime.now();
    }

    public void updateName(String name) {
        this.name = name;
        this.updatedAt = ZonedDateTime.now();
    }

    public void updateDescription(String description) {
        this.description = description;
        this.updatedAt = ZonedDateTime.now();
    }

    public void updateProfileImageUrl(String profileImage) {
        this.profileImage = ProfileImageVo.of(profileImage);
        this.updatedAt = ZonedDateTime.now();
    }

    // Getters - Domain VOs 반환
    //public Long getId() { return id; }
    //public Long getMemberId() { return memberId; }
    //public DescriptionVo getDescription() { return description; }
    //public ProfileImageUrlVo getProfileImageUrl() { return profileImage; }
    //public VisibilityVo getVisibility() { return visibility; }
    //public ZonedDateTime getCreatedAt() { return createdAt; }
    //public ZonedDateTime getUpdatedAt() { return updatedAt; }

    // Primitive 값 반환 메서드 (편의용)
    public String getProfileImageValue() { return profileImage != null ? profileImage.value() : null; }
}