package org.certis.studyplatform.member.domain;

import org.certis.studyplatform.member.domain.vo.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

//TODO: Lombok Getter 가 접근 가능한지 알아보기
// => 원시타입만 가능함
@Deprecated(forRemoval = true)
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
    private OffsetDateTime createdAt;
    @Getter
    private OffsetDateTime updatedAt;

    // 새 프로필 생성
    public Profile(Long memberId, String name, String description, 
                   String profileImage) {
        this.memberId = memberId;
        this.name = name;
        this.description = description;
        this.profileImage = ProfileImageVo.of(profileImage);
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    // 기존 프로필 복원 (Repository 계층에서 사용)
    public Profile(Long id, Long memberId, String name, 
                   String description, ProfileImageVo profileImage,
                   OffsetDateTime createdAt, OffsetDateTime updatedAt) {
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
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateName(String name) {
        this.name = name;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateDescription(String description) {
        this.description = description;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateProfileImageUrl(String profileImage) {
        this.profileImage = ProfileImageVo.of(profileImage);
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters - Domain VOs 반환
    //public Long getId() { return id; }
    //public Long getMemberId() { return memberId; }
    //public DescriptionVo getDescription() { return description; }
    //public ProfileImageUrlVo getProfileImageUrl() { return profileImage; }
    //public VisibilityVo getVisibility() { return visibility; }
    //public OffsetDateTime getCreatedAt() { return createdAt; }
    //public OffsetDateTime getUpdatedAt() { return updatedAt; }

    // Primitive 값 반환 메서드 (편의용)
    public String getProfileImageValue() { return profileImage != null ? profileImage.value() : null; }
}