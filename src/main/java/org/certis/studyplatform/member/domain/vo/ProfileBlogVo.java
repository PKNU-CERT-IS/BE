package org.certis.studyplatform.member.domain.vo;

import org.certis.studyplatform.project.domain.ProjectStatus;

import java.time.OffsetDateTime;
import java.util.Arrays;

/**
 * 블로그 프로필 Value Object (Record)
 * Domain Layer에서 사용하는 불변 데이터 객체
 *
 * Record를 사용하여 불변성, equals, hashCode, toString 자동 제공
 */
public record ProfileBlogVo(
        Long blogId,
        String title,
        String description,
        ProjectStatus projectStatus,
        OffsetDateTime blogStartDate,
        OffsetDateTime blogEndDate,
        String[] tags,
        Integer viewCount,
        Integer likeCount
) {

    /**
     * 방어적 복사를 위한 정규화 생성자
     */
    public ProfileBlogVo {
        // 배열의 불변성 보장
        tags = tags != null ? tags.clone() : new String[0];

        // 카운트 필드들의 기본값 설정
        viewCount = viewCount != null ? viewCount : 0;
        likeCount = likeCount != null ? likeCount : 0;
    }

    /**
     * 편의 생성자 - 최소 필드만
     */
    public ProfileBlogVo(Long blogId, String title, String description, ProjectStatus projectStatus,
                         OffsetDateTime blogStartDate, OffsetDateTime blogEndDate, String[] tags) {
        this(blogId, title, description, projectStatus, blogStartDate, blogEndDate, tags, 0, 0);
    }

    /**
     * 발행된 블로그인지 확인
     */
    public boolean isPublished() {
        return ProjectStatus.COMPLETED.equals(projectStatus);
    }

    /**
     * 진행 중인 블로그인지 확인
     */
    public boolean isInProgress() {
        return ProjectStatus.INPROGRESS.equals(projectStatus);
    }

    /**
     * 준비 단계인지 확인
     */
    public boolean isReady() {
        return ProjectStatus.READY.equals(projectStatus);
    }

    /**
     * 중단된 블로그인지 확인
     */
    public boolean isRejected() {
        return ProjectStatus.REJECTED.equals(projectStatus);
    }

    /**
     * 활성 상태인지 확인
     */
    public boolean isActive() {
        return projectStatus != null && projectStatus.isActive();
    }

    /**
     * tags를 String 배열로 반환
     */
    public String[] getTagsArray() {
        return tags.clone();
    }

    /**
     * 특정 태그가 포함되어 있는지 확인
     */
    public boolean hasTag(String tag) {
        if (tag == null || tags == null) {
            return false;
        }
        return Arrays.asList(tags).contains(tag);
    }

    /**
     * 특정 기술을 사용하는지 확인
     */
    public boolean usesTechnology(String technology) {
        if (technology == null || tags == null) {
            return false;
        }
        return Arrays.stream(tags)
                .anyMatch(tech -> tech.equalsIgnoreCase(technology));
    }

    /**
     * 조회수가 특정 수치 이상인지 확인
     */
    public boolean hasMinimumViews(int minimumViews) {
        return viewCount >= minimumViews;
    }

    /**
     * 좋아요 수가 특정 수치 이상인지 확인
     */
    public boolean hasMinimumLikes(int minimumLikes) {
        return likeCount >= minimumLikes;
    }

    /**
     * 인기 블로그인지 확인 (조회수 100 이상 또는 좋아요 10 이상)
     */
    public boolean isPopular() {
        return viewCount >= 100 || likeCount >= 10;
    }
}