package org.certis.studyplatform.member.infrastructure.persistence;

import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.Profile;
import org.certis.studyplatform.member.domain.repository.query.ProfileQueryRepository;
import org.certis.studyplatform.member.domain.vo.ProfileStudyVo;
import org.certis.studyplatform.member.domain.vo.ProfileProjectVo;
import org.certis.studyplatform.member.domain.vo.ProfileBlogVo;
import org.certis.studyplatform.member.domain.vo.ActivitySummaryVo;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Profile Query Repository 임시 구현체
 *
 * 실제 Infrastructure Layer 구현 전까지 사용할 Mock Repository
 * 테스트와 개발을 위한 임시 데이터 제공
 *
 * TODO: JPA/jOOQ 실제 구현으로 교체 필요
 */
@Repository
@Slf4j
public class ProfileQueryRepositoryImpl implements ProfileQueryRepository {

    @Override
    public Optional<Profile> findByMemberId(Long memberId) {
        log.info("Mock: Finding profile by member ID: {}", memberId);

        // 임시 Mock 데이터
        if (memberId != null && memberId > 0) {
            Profile mockProfile = new Profile(
                    memberId,
                    "Mock User " + memberId,
                    "Mock description for user " + memberId,
                    "https://mock-image.com/profile" + memberId + ".jpg"
            );
            return Optional.of(mockProfile);
        }

        return Optional.empty();
    }

    @Override
    public List<ProfileStudyVo> findStudiesByMemberId(Long memberId) {
        log.info("Mock: Finding studies by member ID: {}", memberId);

        // 임시 Mock 데이터
        ZonedDateTime now = ZonedDateTime.now();
        return List.of(
                new ProfileStudyVo(
                        1L,
                        "Mock Spring Boot 스터디",
                        "Mock Spring Boot 기초부터 실무까지",
                        "IN_PROGRESS",
                        "LEADER",
                        now.minusDays(30),
                        now.minusDays(30),
                        now.plusDays(30),
                        "https://meet.google.com/mock-spring",
                        5,
                        List.of("Spring", "Java", "Backend"),
                        "Backend"
                ),
                new ProfileStudyVo(
                        2L,
                        "Mock React 스터디",
                        "Mock React 프론트엔드 개발",
                        "COMPLETED",
                        "MEMBER",
                        now.minusDays(90),
                        now.minusDays(90),
                        now.minusDays(30),
                        "https://meet.google.com/mock-react",
                        6,
                        List.of("React", "JavaScript", "Frontend"),
                        "Frontend"
                )
        );
    }

    @Override
    public List<ProfileProjectVo> findProjectsByMemberId(Long memberId) {
        log.info("Mock: Finding projects by member ID: {}", memberId);

        // 임시 Mock 데이터
        ZonedDateTime now = ZonedDateTime.now();
        return List.of(
                new ProfileProjectVo(
                        1L,
                        "Mock E-Commerce Platform",
                        "Mock 온라인 쇼핑몰 개발 프로젝트",
                        "IN_PROGRESS",
                        "PROJECT_LEADER",
                        now.minusDays(60),
                        now.minusDays(60),
                        now.plusDays(60),
                        "https://github.com/mock/ecommerce",
                        "https://mock-ecommerce.com",
                        4,
                        List.of("Spring Boot", "React", "MySQL", "AWS")
                ),
                new ProfileProjectVo(
                        2L,
                        "Mock Blog Platform",
                        "Mock 개인 블로그 플랫폼",
                        "COMPLETED",
                        "TECH_LEADER",
                        now.minusDays(120),
                        now.minusDays(120),
                        now.minusDays(30),
                        "https://github.com/mock/blog",
                        "https://mock-blog.com",
                        3,
                        List.of("Next.js", "Node.js", "MongoDB")
                )
        );
    }

    @Override
    public List<ProfileBlogVo> findBlogsByMemberId(Long memberId) {
        log.info("Mock: Finding blogs by member ID: {}", memberId);

        // 임시 Mock 데이터
        ZonedDateTime now = ZonedDateTime.now();
        return List.of(
                new ProfileBlogVo(
                        1L,
                        "Mock Spring Boot 시작하기",
                        "Mock Spring Boot 프레임워크 기초 설명...",
                        "PUBLISHED",
                        now.minusDays(15),
                        now.minusDays(10),
                        now.minusDays(5),
                        150,
                        25,
                        8,
                        List.of("Backend", "Tutorial"),
                        List.of("Spring", "Java", "Tutorial"),
                        "https://mock-thumbnail1.jpg"
                ),
                new ProfileBlogVo(
                        2L,
                        "Mock Clean Architecture 적용기",
                        "Mock 실제 프로젝트에 Clean Architecture 적용한 경험...",
                        "PUBLISHED",
                        now.minusDays(7),
                        now.minusDays(5),
                        now.minusDays(2),
                        89,
                        12,
                        5,
                        List.of("Architecture", "Experience"),
                        List.of("Clean Architecture", "Design Pattern"),
                        "https://mock-thumbnail2.jpg"
                ),
                new ProfileBlogVo(
                        3L,
                        "Mock 진행 중인 글",
                        "Mock 아직 작성 중인 블로그 글...",
                        "DRAFT",
                        now.minusDays(3),
                        null,
                        now.minusDays(1),
                        0,
                        0,
                        0,
                        List.of("Draft"),
                        List.of("WIP"),
                        null
                )
        );
    }

    @Override
    public boolean existsByMemberId(Long memberId) {
        log.info("Mock: Checking if profile exists for member ID: {}", memberId);
        return memberId != null && memberId > 0;
    }

    @Override
    public long countStudiesByMemberId(Long memberId) {
        log.info("Mock: Counting studies for member ID: {}", memberId);
        return 2L; // Mock count
    }

    @Override
    public long countProjectsByMemberId(Long memberId) {
        log.info("Mock: Counting projects for member ID: {}", memberId);
        return 2L; // Mock count
    }

    @Override
    public long countBlogsByMemberId(Long memberId) {
        log.info("Mock: Counting blogs for member ID: {}", memberId);
        return 3L; // Mock count
    }

    @Override
    public ActivitySummaryVo getRecentActivitySummary(Long memberId) {
        log.info("Mock: Getting activity summary for member ID: {}", memberId);

        // 임시 Mock 데이터 (최근 30일 활동)
        return new ActivitySummaryVo(2L, 1L, 2L);
    }
}