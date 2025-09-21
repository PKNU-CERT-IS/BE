package org.certis.studyplatform.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.repository.query.ProfileQueryRepository;
import org.certis.studyplatform.member.domain.repository.query.MemberQueryRepository;
import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.infrastructure.mapper.MemberInfrastructureMapper;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.certis.studyplatform.member.infrastructure.persistence.jpa.MemberJpaRepository;
import org.certis.studyplatform.project.domain.ProjectStatus;
import org.certis.studyplatform.schedule.domain.repository.ScheduleQueryRepository;
import org.certis.studyplatform.schedule.domain.model.vo.ScheduleVo;
import org.certis.studyplatform.study.domain.StudyStatus;
import org.certis.studyplatform.blog.domain.ArticleReferenceType;
import org.certis.studyplatform.shared.util.GracePeriodCalculator;
import org.certis.studyplatform.shared.util.GracePeriodCalculator.ActivityInfo;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Profile Query Repository JPA 구현체
 *
 * Profile 도메인의 읽기 작업을 담당하는 실제 구현체
 * Member Entity를 통해 Profile 정보를 조회하고 Domain 객체로 변환
 *
 * 특징:
 * - JPA를 사용한 실제 데이터베이스 조회
 * - Entity ↔ Profile Domain 객체 변환
 * - 읽기 전용 최적화 쿼리
 * - 목 데이터를 통한 gracePeriod 계산
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ProfileQueryRepositoryImpl implements ProfileQueryRepository {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final MemberInfrastructureMapper memberInfrastructureMapper;
    private final ScheduleQueryRepository scheduleQueryRepository;

    @Override
    public Optional<ProfileVo> findByMemberId(MemberIdVo memberIdVo) {
        log.debug("Command Infrastructure: Finding profile by member ID: {}", memberIdVo.toLong());

        try {
            Optional<MemberEntity> memberEntityOpt = memberJpaRepository.findById(memberIdVo.toLong());

            if (memberEntityOpt.isEmpty()) {
                log.debug("Member not found for ID: {}", memberIdVo.toLong());
                return Optional.empty();
            }

            MemberEntity memberEntity = memberEntityOpt.get();

            // Profile 정보 유무와 상관없이 기본 ProfileVo 생성 (gracePeriod는 null)
            // 조회 시점에 프로필 설명/이미지가 없어도 수정 API에서 생성/갱신 가능해야 함
            ProfileVo baseProfile = memberInfrastructureMapper.toProfile(memberEntity);

            // 목 데이터를 통해 gracePeriod 계산
            OffsetDateTime gracePeriod = calculateGracePeriodFromMockData(memberIdVo.toLong());

            // 오늘의 스케줄 조회
            List<ScheduleVo> todayScheduleVos = scheduleQueryRepository.findTodaySchedulesByMemberId(memberIdVo);
            List<ScheduleInfoVo> todaySchedules = todayScheduleVos.stream()
                    .map(scheduleVo -> ScheduleInfoVo.of(
                            scheduleVo.id(),
                            scheduleVo.title(),
                            scheduleVo.place(),
                            scheduleVo.type(),
                            scheduleVo.startedAt(),
                            scheduleVo.endedAt()
                    ))
                    .toList();

            // 연락처 정보 조회
            Optional<MemberContactVo> contactOpt = memberQueryRepository.findContactByMemberId(memberIdVo.toLong());
            String phoneNumber = contactOpt.map(MemberContactVo::phoneNumber).map(PhoneNumberVo::value).orElse(null);
            String email = contactOpt.map(MemberContactVo::email).map(EmailVo::value).orElse(null);
            String githubUrl = contactOpt.map(MemberContactVo::githubUrl).map(GithubUrlVo::value).orElse(null);
            String linkedUrl = contactOpt.map(MemberContactVo::linkedinUrl).map(LinkedinUrlVo::value).orElse(null);

            // gracePeriod, todaySchedules, contact 정보가 포함된 새로운 ProfileVo 생성
            ProfileVo profileWithEnhancements = new ProfileVo(
                baseProfile.memberId(),
                baseProfile.name(),
                baseProfile.description(),
                baseProfile.profileImage(),
                todaySchedules, // 실제 스케줄 데이터
                baseProfile.penaltyCount(),
                gracePeriod, // 계산된 gracePeriod
                baseProfile.memberRole(),
                baseProfile.memberGrade(),
                baseProfile.skills(),
                baseProfile.createdAt(),
                // Enhanced profile fields
                baseProfile.major(),
                baseProfile.birthday(),
                phoneNumber,
                baseProfile.studentNumber(),
                email,
                githubUrl,
                linkedUrl
            );

            return Optional.of(profileWithEnhancements);

        } catch (Exception e) {
            log.error("Error finding profile by member ID {}: {}", memberIdVo.toLong(), e.getMessage(), e);
            // 예외를 다시 던지지 않고 빈 Optional 반환하여 트랜잭션 롤백 방지
            return Optional.empty();
        }
    }

    @Override
    public List<ProfileStudyVo> findStudiesByMemberId(MemberIdVo memberIdVo) {

        log.debug("Query Infrastructure: Finding studies by member ID: {}", memberIdVo.toLong());

        // TODO: 실제 Study Entity와 연관관계 설정 후 구현
        // StudyParticipant 테이블과 조인하여 해당 회원이 참여한 스터디 목록 조회
        // 새로운 VO 구조: ProfileStudyVo(studyId, title, description, ProjectStatus projectStatus,
        //                                studyStartDate, studyEndDate, List<String> tags)

        // 목데이터 반환
        return createMockStudies(memberIdVo.toLong());
    }

    @Override
    public List<ProfileProjectVo> findProjectsByMemberId(MemberIdVo memberIdVo) {

        log.debug("Query Infrastructure: Finding projects by member ID: {}", memberIdVo.toLong());

        // TODO: 실제 Project Entity와 연관관계 설정 후 구현
        // ProjectParticipant 테이블과 조인하여 해당 회원이 참여한 프로젝트 목록 조회
        // 새로운 VO 구조: ProfileProjectVo(projectId, title, description, ProjectStatus projectStatus,
        //                                  studyStartDate, studyEndDate, List<String> tags)

        // 목데이터 반환
        return createMockProjects(memberIdVo.toLong());
    }

    @Override
    public List<ProfileBlogVo> findBlogsByMemberId(MemberIdVo memberIdVo) {

        log.debug("Query Infrastructure: Finding blogs by member ID: {}", memberIdVo.toLong());

        // TODO: 실제 Blog Entity와 연관관계 설정 후 구현
        // Blog 테이블에서 작성자 ID로 해당 회원이 작성한 블로그 목록 조회
        // 새로운 VO 구조: ProfileBlogVo(blogId, title, description, ProjectStatus projectStatus,
        //                               studyStartDate, studyEndDate, String[] tags, viewCount, likeCount)

        // 목데이터 반환
        return createMockBlogs(memberIdVo.toLong());
    }

    @Override
    public boolean existsByMemberId(MemberIdVo memberIdVo) {

        try {
            return memberJpaRepository.findById(memberIdVo.toLong())
                    .map(memberInfrastructureMapper::hasProfileInformation)
                    .orElse(false);
        } catch (Exception e) {
            log.error("Error checking profile existence for member ID {}: {}", memberIdVo.toLong(), e.getMessage());
            return false;
        }
    }

    @Override
    public long countStudiesByMemberId(MemberIdVo memberIdVo) {

        log.debug("Query Infrastructure: Counting studies for member ID: {}", memberIdVo.toLong());

        // TODO: 실제 Study Entity와 연관관계 설정 후 구현
        // 목데이터 기준으로 카운트 반환
        return createMockStudies(memberIdVo.toLong()).size();
    }

    @Override
    public long countProjectsByMemberId(MemberIdVo memberIdVo) {

        log.debug("Query Infrastructure: Counting projects for member ID: {}", memberIdVo.toLong());

        // TODO: 실제 Project Entity와 연관관계 설정 후 구현
        // 목데이터 기준으로 카운트 반환
        return createMockProjects(memberIdVo.toLong()).size();
    }

    @Override
    public long countBlogsByMemberId(MemberIdVo memberIdVo) {

        log.debug("Query Infrastructure: Counting blogs for member ID: {}", memberIdVo.toLong());

        // TODO: 실제 Blog Entity와 연관관계 설정 후 구현
        // 목데이터 기준으로 카운트 반환
        return createMockBlogs(memberIdVo.toLong()).size();
    }

    @Override
    public ActivitySummaryVo getRecentActivitySummary(MemberIdVo memberIdVo) {

        log.debug("Query Infrastructure: Getting activity summary for member ID: {}", memberIdVo.toLong());

        // TODO: 실제 Study, Project, Blog Entity와 연관관계 설정 후 구현
        // 목데이터 기준으로 활동 요약 반환
        long studyCount = countStudiesByMemberId(memberIdVo);
        long projectCount = countProjectsByMemberId(memberIdVo);
        long blogCount = countBlogsByMemberId(memberIdVo);

        return new ActivitySummaryVo(studyCount, projectCount, blogCount);
    }

    /**
     * 목 데이터를 기반으로 gracePeriod 계산
     *
     * @param memberId 회원 ID
     * @return 계산된 유예기간 (없으면 null)
     */
    private OffsetDateTime calculateGracePeriodFromMockData(Long memberId) {
        try {
            log.debug("Calculating grace period for member ID: {}", memberId);

            // 목 스터디와 프로젝트 데이터 조회
            List<ProfileStudyVo> studies = createMockStudies(memberId);
            List<ProfileProjectVo> projects = createMockProjects(memberId);

            log.debug("Found {} studies and {} projects for member {}", studies.size(), projects.size(), memberId);

            // VO를 ActivityInfo로 변환
            List<ActivityInfo> activities = new ArrayList<>();
            OffsetDateTime now = OffsetDateTime.now();

            // 스터디를 ActivityInfo로 변환
            for (ProfileStudyVo study : studies) {
                if (study.studyStartDate() != null && study.studyEndDate() != null) {
                    // 실제 날짜 기반으로 진행 중인지 판단
                    boolean isOngoing = !now.isBefore(study.studyStartDate()) && now.isBefore(study.studyEndDate());
                    ActivityInfo activityInfo = new ActivityInfo(
                        study.studyStartDate(),
                        study.studyEndDate(),
                        isOngoing
                    );
                    activities.add(activityInfo);

                    log.debug("Added study activity: {} ({} to {}, ongoing: {}, status: {})",
                            study.title(), study.studyStartDate(), study.studyEndDate(), isOngoing, study.studyStatus());
                }
            }

            // 프로젝트를 ActivityInfo로 변환
            for (ProfileProjectVo project : projects) {
                if (project.projectStartDate() != null && project.projectEndDate() != null) {
                    // 실제 날짜 기반으로 진행 중인지 판단
                    boolean isOngoing = !now.isBefore(project.projectStartDate()) && now.isBefore(project.projectEndDate());
                    ActivityInfo activityInfo = new ActivityInfo(
                        project.projectStartDate(),
                        project.projectEndDate(),
                        isOngoing
                    );
                    activities.add(activityInfo);

                    log.debug("Added project activity: {} ({} to {}, ongoing: {}, status: {})",
                            project.title(), project.projectStartDate(), project.projectEndDate(), isOngoing, project.projectStatus());
                }
            }

            // 진행 중인 활동과 완료된 활동 분류
            long ongoingCount = activities.stream().filter(ActivityInfo::isOngoing).count();
            long completedCount = activities.stream().filter(a -> !a.isOngoing()).count();

            log.debug("Activity summary for member {}: {} ongoing, {} completed", memberId, ongoingCount, completedCount);

            // GracePeriodCalculator를 사용하여 유예기간 계산
            OffsetDateTime gracePeriod = GracePeriodCalculator.calculateGracePeriod(activities);

            if (gracePeriod != null) {
                if (ongoingCount > 0) {
                    log.info("Calculated grace period for member {} based on ONGOING activities: {} (from {} total activities)",
                            memberId, gracePeriod, activities.size());
                } else {
                    log.info("Calculated grace period for member {} based on COMPLETED activities: {} (from {} total activities)",
                            memberId, gracePeriod, activities.size());
                }
            } else {
                log.info("No grace period calculated for member {} (no valid activities)", memberId);
            }

            return gracePeriod;

        } catch (Exception e) {
            log.warn("Failed to calculate grace period for member {}: {}", memberId, e.getMessage());
            return null; // 계산 실패 시 null 반환
        }
    }

    // =================================================================
    // 목데이터 생성 메서드들
    // =================================================================

    /**
     * 스터디 목데이터 생성
     */
    private List<ProfileStudyVo> createMockStudies(Long memberId) {
        OffsetDateTime now = OffsetDateTime.now();

        return List.of(
            new ProfileStudyVo(
                1L,
                "Spring Boot 스터디",
                "Spring Boot 기초부터 심화까지 학습하는 스터디입니다.",
                StudyStatus.INPROGRESS, // 현재 진행 중
                now.minusWeeks(2), // 2주 전 시작 (현재 진행 중)
                now.plusWeeks(2),  // 2주 후 종료 예정 (4주 기간 → 1주 유예)
                new String[]{"Spring Boot", "Java", "JPA"},
                "TECH", // category
                "BACKEND" // subcategory
            ),
            new ProfileStudyVo(
                2L,
                "React 프론트엔드 스터디",
                "React를 활용한 모던 웹 개발 스터디입니다.",
                StudyStatus.COMPLETED, // 완료됨
                now.minusWeeks(8), // 8주 전 시작
                now.minusWeeks(2), // 2주 전 종료 (6주 기간 → 2주 유예)
                new String[]{"React", "JavaScript", "TypeScript"},
                "TECH", // category
                "FRONTEND" // subcategory
            ),
            new ProfileStudyVo(
                3L,
                "알고리즘 문제해결 스터디",
                "코딩테스트 대비 알고리즘 문제 해결 스터디입니다.",
                StudyStatus.READY, // 아직 시작 안함
                now.plusWeeks(1),   // 1주 후 시작 예정 (아직 진행 중 아님)
                now.plusWeeks(4),   // 4주 후 종료 예정 (3주 기간)
                new String[]{"Algorithm", "Problem Solving", "Python"},
                "TECH", // category
                "ALGORITHM" // subcategory
            )
        );
    }

    /**
     * 프로젝트 목데이터 생성
     */
    private List<ProfileProjectVo> createMockProjects(Long memberId) {
        OffsetDateTime now = OffsetDateTime.now();

        return List.of(
            new ProfileProjectVo(
                1L,
                "학습 플랫폼 개발 프로젝트",
                "온라인 학습 플랫폼을 개발하는 프로젝트입니다.",
                ProjectStatus.INPROGRESS, // 현재 진행 중
                now.minusWeeks(3), // 3주 전 시작 (현재 진행 중)
                now.plusWeeks(1),  // 1주 후 종료 예정 (4주 기간 → 1주 유예)
                new String[]{"Spring Boot", "React", "PostgreSQL"},
                "SECURITY", // category
                "WEB_PLATFORM" // subcategory
            ),
            new ProfileProjectVo(
                2L,
                "전자상거래 웹사이트",
                "전자상거래 웹사이트 구축 프로젝트입니다.",
                ProjectStatus.COMPLETED, // 완료됨
                now.minusWeeks(16), // 16주 전 시작
                now.minusWeeks(4),  // 4주 전 종료 (12주 기간 → 2주 유예)
                new String[]{"Node.js", "Vue.js", "MongoDB"},
                "SECURITY", // category
                "E_COMMERCE" // subcategory
            ),
            new ProfileProjectVo(
                3L,
                "모바일 앱 개발",
                "크로스 플랫폼 모바일 앱 개발 프로젝트입니다.",
                ProjectStatus.READY, // 아직 시작 안함
                now.plusWeeks(2),  // 2주 후 시작 예정 (아직 진행 중 아님)
                now.plusWeeks(6),  // 6주 후 종료 예정 (4주 기간)
                new String[]{"React Native", "Firebase", "TypeScript"},
                "SECURITY", // category
                "MOBILE" // subcategory
            )
        );
    }

    /**
     * Mock 스케줄 데이터 생성
     */
    private List<ScheduleInfoVo> createMockSchedules() {
        OffsetDateTime now = OffsetDateTime.now();
        
        return List.of(
            ScheduleInfoVo.of(
                1L,
                "오전 스터디",
                "강의실 A",
                "STUDY",
                now.withHour(9).withMinute(0),
                now.withHour(11).withMinute(0)
            ),
            ScheduleInfoVo.of(
                2L,
                "오후 프로젝트",
                "강의실 B",
                "PROJECT",
                now.withHour(14).withMinute(0),
                now.withHour(16).withMinute(0)
            )
        );
    }

    /**
     * 블로그 목데이터 생성
     */
    private List<ProfileBlogVo> createMockBlogs(Long memberId) {
        OffsetDateTime now = OffsetDateTime.now();

        return List.of(
            new ProfileBlogVo(
                1L,
                "Spring Boot 시작하기",
                "Spring Boot 프레임워크의 기본 개념과 설정 방법을 소개합니다.",
                ProjectStatus.COMPLETED, // 발행됨
                now.minusWeeks(4), // 4주 전 시작 (2주 작성 기간)
                now.minusWeeks(2), // 2주 전 완료
                new String[]{"Spring Boot", "Java", "Tutorial"},
                150,
                25,
                "TECH", // category
                ArticleReferenceType.PROJECT,
                "웹 개발 프로젝트"
            ),
            new ProfileBlogVo(
                2L,
                "React Hooks 완전 정복",
                "React Hooks의 모든 것을 다루는 포괄적인 가이드입니다.",
                ProjectStatus.INPROGRESS, // 작성 중
                now.minusWeeks(3), // 3주 전 시작
                now.plusWeeks(1),  // 1주 후 완료 예정 (4주 작성 기간)
                new String[]{"React", "JavaScript", "Hooks"},
                89,
                12,
                "TECH", // category
                ArticleReferenceType.STUDY,
                "프론트엔드 스터디"
            ),
            new ProfileBlogVo(
                3L,
                "알고리즘 문제 해결 전략",
                "효율적인 알고리즘 문제 해결 방법론을 제시합니다.",
                ProjectStatus.READY, // 준비 중
                now.plusWeeks(1),   // 1주 후 시작 예정
                now.plusWeeks(3),   // 3주 후 완료 예정 (2주 작성 기간)
                new String[]{"Algorithm", "Problem Solving", "Coding Test"},
                0,
                0,
                "TECH", // category
                null, // referenceType
                null  // referenceTitle
            )
        );
    }
}