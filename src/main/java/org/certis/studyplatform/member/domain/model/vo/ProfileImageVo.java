package org.certis.studyplatform.member.domain.model.vo;

public record ProfileImageVo(String value) {
    public ProfileImageVo {
        if (value != null && value.trim().isEmpty()) {
            throw new IllegalArgumentException("프로필 이미지 URL이 비어있습니다");
        }
    }
}