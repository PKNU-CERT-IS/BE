package org.certis.studyplatform.member.domain.vo;

import java.time.ZonedDateTime;

/**
 * 회원 수정 결과 Value Object
 * 
 * 회원 정보 수정 성공 후 반환되는 정보를 담는 불변 객체
 * 수정된 주요 정보만 포함하여 응답 크기 최적화
 */
public record MemberUpdatedVo(
        MemberIdVo id,                    // 수정된 회원 ID
        NameVo name,                      // 수정된 이름
        ProfileImageVo profileImage,      // 수정된 프로필 이미지 (nullable)
        ZonedDateTime updatedAt           // 수정 시각
) {
    
    public MemberUpdatedVo {
        if (id == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다");
        }
        if (name == null) {
            throw new IllegalArgumentException("이름은 필수입니다");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("수정 시각은 필수입니다");
        }
    }
    
    /**
     * 정적 팩토리 메서드 (프로필 이미지 포함)
     */
    public static MemberUpdatedVo of(MemberIdVo id, NameVo name, 
                                     ProfileImageVo profileImage, ZonedDateTime updatedAt) {
        return new MemberUpdatedVo(id, name, profileImage, updatedAt);
    }
    
    /**
     * 정적 팩토리 메서드 (프로필 이미지 없음)
     */
    public static MemberUpdatedVo of(MemberIdVo id, NameVo name, ZonedDateTime updatedAt) {
        return new MemberUpdatedVo(id, name, null, updatedAt);
    }
    
    /**
     * 정적 팩토리 메서드 (primitive 값들로)
     */
    public static MemberUpdatedVo of(Long id, String name, String profileImage, ZonedDateTime updatedAt) {
        return new MemberUpdatedVo(
            MemberIdVo.of(id), 
            NameVo.of(name), 
            profileImage != null ? ProfileImageVo.of(profileImage) : null, 
            updatedAt
        );
    }
    
    // =================================================================
    // Getter 메서드들
    // =================================================================
    
    /**
     * 회원 ID VO 반환
     */
    public MemberIdVo getId() {
        return id;
    }
    
    /**
     * 이름 VO 반환
     */
    public NameVo getName() {
        return name;
    }
    
    /**
     * 프로필 이미지 VO 반환
     */
    public ProfileImageVo getProfileImage() {
        return profileImage;
    }
    
    /**
     * 수정 시각 반환
     */
    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * 수정된 회원 ID 반환
     */
    public Long getMemberId() {
        return id.value();
    }
    
    /**
     * 수정된 이름 반환
     */
    public String getNameValue() {
        return name.value();
    }
    
    /**
     * 프로필 이미지 URL 반환 (nullable)
     */
    public String getProfileImageValue() {
        return profileImage != null ? profileImage.value() : null;
    }
    
    /**
     * 프로필 이미지가 있는지 확인
     */
    public boolean hasProfileImage() {
        return profileImage != null;
    }
}