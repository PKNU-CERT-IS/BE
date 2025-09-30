package org.certis.studyplatform.member.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.member.domain.repository.query.ProfileQueryRepository;
import org.certis.studyplatform.member.domain.repository.query.MemberQueryRepository;
import org.certis.studyplatform.member.domain.vo.*;
import org.certis.studyplatform.member.infrastructure.mapper.MemberInfrastructureMapper;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberPenaltyEntity;
import org.certis.studyplatform.member.infrastructure.persistence.jpa.MemberJpaRepository;
import org.certis.studyplatform.member.infrastructure.persistence.jpa.MemberPenaltyJpaRepository;
import org.certis.studyplatform.study.domain.repository.StudyParticipantQueryRepository;
import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantSummaryVo;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository;
import org.certis.studyplatform.project.domain.repository.ProjectQueryRepository;
import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.certis.studyplatform.project.domain.vo.ProjectParticipantSummaryVo;
import org.certis.studyplatform.blog.domain.repository.BlogQueryRepository;
import org.certis.studyplatform.blog.domain.vo.BlogSummaryVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final MemberPenaltyJpaRepository memberPenaltyJpaRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final MemberInfrastructureMapper memberInfrastructureMapper;
    private final ScheduleQueryRepository scheduleQueryRepository;
    private final StudyParticipantQueryRepository studyParticipantQueryRepository;
    private final StudyQueryRepository studyQueryRepository;
    private final ProjectParticipantQueryRepository projectParticipantQueryRepository;
    private final ProjectQueryRepository projectQueryRepository;
    private final BlogQueryRepository blogQueryRepository;

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

            // 벌점 정보 조회
            Optional<MemberPenaltyEntity> penaltyOpt = memberPenaltyJpaRepository.findByMemberId(memberIdVo.toLong());
            Integer penaltyCount = penaltyOpt.map(MemberPenaltyEntity::getPenaltyPoint).orElse(0);
            log.debug("Penalty count for member {}: {}", memberIdVo.toLong(), penaltyCount);

            // gracePeriod, todaySchedules, contact 정보, 벌점 정보가 포함된 새로운 ProfileVo 생성
            ProfileVo profileWithEnhancements = new ProfileVo(
                baseProfile.memberId(),
                baseProfile.name(),
                baseProfile.description(),
                baseProfile.profileImage(),
                todaySchedules, // 실제 스케줄 데이터
                penaltyCount, // 실제 벌점 데이터
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

        try {
            // StudyParticipantQueryRepository를 통해 해당 회원이 참여한 스터디 참가자 정보 조회
            Page<StudyParticipantSummaryVo> participants = studyParticipantQueryRepository.findByMemberId(
                    memberIdVo.toLong(), 
                    Pageable.unpaged()
            );

            if (participants.isEmpty()) {
                log.debug("No studies found for member ID: {}", memberIdVo.toLong());
                return List.of();
            }

            // 스터디 단위로 그룹핑 후, 각 스터디에 대해 최신 상태를 조회하여 포함 여부 결정
            java.util.Set<Long> studyIdSet = new java.util.LinkedHashSet<>();

            // 우선 멤버가 연관된 스터디 ID만 집합으로 수집 (중복 제거)
            java.util.Set<Long> allRelatedStudyIds = participants.getContent().stream()
                    .map(StudyParticipantSummaryVo::studyId)
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

            // 각 스터디에 대해 최신 참가 상태 조회 (UPDATED_AT desc) 후 APPROVED + not-deleted 인 경우만 포함
            for (Long studyId : allRelatedStudyIds) {
                var latestOpt = studyParticipantQueryRepository.findByStudyIdAndMemberId(studyId, memberIdVo.toLong());
                if (latestOpt.isPresent()) {
                    var latest = latestOpt.get();
                    if (latest.status() == org.certis.studyplatform.study.domain.StudyParticipantStatus.APPROVED) {
                        studyIdSet.add(studyId);
                    }
                }
            }

            if (studyIdSet.isEmpty()) {
                log.debug("No APPROVED studies for member ID: {}", memberIdVo.toLong());
                return List.of();
            }

            // 개별 스터디 조회
            List<StudyVo> studies = studyIdSet.stream()
                    .map(studyId -> studyQueryRepository.findByIdAndDeletedAtIsNull(studyId))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            // ProfileStudyVo로 변환
            return studies.stream()
                    .map(this::toProfileStudyVo)
                    .toList();

        } catch (Exception e) {
            log.error("Error finding studies for member ID {}: {}", memberIdVo.toLong(), e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public List<ProfileProjectVo> findProjectsByMemberId(MemberIdVo memberIdVo) {
        log.debug("Query Infrastructure: Finding projects by member ID: {}", memberIdVo.toLong());

        try {
            // ProjectParticipantQueryRepository를 통해 해당 회원이 참여한 프로젝트 참가자 정보 조회
            Page<ProjectParticipantSummaryVo> participants = projectParticipantQueryRepository.findByMemberId(
                    memberIdVo.toLong(), 
                    Pageable.unpaged()
            );

            if (participants.isEmpty()) {
                log.debug("No projects found for member ID: {}", memberIdVo.toLong());
                return List.of();
            }

            // 프로젝트 단위 중복 제거 후, 각 프로젝트의 최신 참가 상태를 확인하여 APPROVED만 포함
            java.util.Set<Long> allRelatedProjectIds = participants.getContent().stream()
                    .map(ProjectParticipantSummaryVo::projectId)
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

            java.util.Set<Long> approvedProjectIds = new java.util.LinkedHashSet<>();
            for (Long projectId : allRelatedProjectIds) {
                var latestOpt = projectParticipantQueryRepository.findByProjectIdAndMemberId(projectId, memberIdVo.toLong());
                if (latestOpt.isPresent()) {
                    var latest = latestOpt.get();
                    if (latest.status() == org.certis.studyplatform.project.domain.ProjectParticipantStatus.APPROVED) {
                        approvedProjectIds.add(projectId);
                    }
                }
            }

            if (approvedProjectIds.isEmpty()) {
                log.debug("No APPROVED projects for member ID: {}", memberIdVo.toLong());
                return List.of();
            }

            // 개별 프로젝트 조회 (승인된 프로젝트만)
            List<ProjectVo> projects = approvedProjectIds.stream()
                    .map(projectId -> projectQueryRepository.findByIdAndDeletedAtIsNull(projectId))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            // ProfileProjectVo로 변환
            return projects.stream()
                    .map(this::toProfileProjectVo)
                    .toList();

        } catch (Exception e) {
            log.error("Error finding projects for member ID {}: {}", memberIdVo.toLong(), e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public List<ProfileBlogVo> findBlogsByMemberId(MemberIdVo memberIdVo) {
        log.debug("Query Infrastructure: Finding blogs by member ID: {}", memberIdVo.toLong());

        try {
            // BlogQueryRepository를 통해 해당 회원이 작성한 블로그 목록 조회
            Page<BlogSummaryVo> blogs = blogQueryRepository.findByMemberId(
                    memberIdVo.toLong(), 
                    Pageable.unpaged()
            );

            if (blogs.isEmpty()) {
                log.debug("No blogs found for member ID: {}", memberIdVo.toLong());
                return List.of();
            }

            // ProfileBlogVo로 변환
            return blogs.getContent().stream()
                    .map(this::toProfileBlogVo)
                    .toList();

        } catch (Exception e) {
            log.error("Error finding blogs for member ID {}: {}", memberIdVo.toLong(), e.getMessage(), e);
            return List.of();
        }
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

        try {
            Page<StudyParticipantSummaryVo> participants = studyParticipantQueryRepository.findByMemberId(
                    memberIdVo.toLong(), 
                    Pageable.unpaged()
            );
            return participants.getTotalElements();
        } catch (Exception e) {
            log.error("Error counting studies for member ID {}: {}", memberIdVo.toLong(), e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countProjectsByMemberId(MemberIdVo memberIdVo) {
        log.debug("Query Infrastructure: Counting projects for member ID: {}", memberIdVo.toLong());

        try {
            Page<ProjectParticipantSummaryVo> participants = projectParticipantQueryRepository.findByMemberId(
                    memberIdVo.toLong(), 
                    Pageable.unpaged()
            );
            return participants.getTotalElements();
        } catch (Exception e) {
            log.error("Error counting projects for member ID {}: {}", memberIdVo.toLong(), e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countBlogsByMemberId(MemberIdVo memberIdVo) {
        log.debug("Query Infrastructure: Counting blogs for member ID: {}", memberIdVo.toLong());

        try {
            return blogQueryRepository.countByMemberId(memberIdVo.toLong());
        } catch (Exception e) {
            log.error("Error counting blogs for member ID {}: {}", memberIdVo.toLong(), e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public ActivitySummaryVo getRecentActivitySummary(MemberIdVo memberIdVo) {
        log.debug("Query Infrastructure: Getting activity summary for member ID: {}", memberIdVo.toLong());

        try {
            // 실제 데이터 기반으로 활동 요약 반환
            long studyCount = countStudiesByMemberId(memberIdVo);
            long projectCount = countProjectsByMemberId(memberIdVo);
            long blogCount = countBlogsByMemberId(memberIdVo);

            log.debug("Activity summary for member {}: {} studies, {} projects, {} blogs", 
                    memberIdVo.toLong(), studyCount, projectCount, blogCount);

            return new ActivitySummaryVo(studyCount, projectCount, blogCount);
        } catch (Exception e) {
            log.error("Error getting activity summary for member ID {}: {}", memberIdVo.toLong(), e.getMessage(), e);
            return new ActivitySummaryVo(0, 0, 0);
        }
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


    // =================================================================
    // Entity to Profile VO 변환 메서드들
    // =================================================================

    /**
     * StudyVo → ProfileStudyVo 변환
     */
    private ProfileStudyVo toProfileStudyVo(StudyVo study) {
        // Study 상태를 날짜 기준으로 계산
        OffsetDateTime now = OffsetDateTime.now();
        StudyStatus status;
        if (study.endDate() != null && study.endDate().isBefore(now)) {
            status = StudyStatus.COMPLETED;
        } else if (study.startDate() != null && study.startDate().isAfter(now)) {
            status = StudyStatus.READY;
        } else {
            status = StudyStatus.INPROGRESS;
        }
        
        return new ProfileStudyVo(
                study.id(),
                study.title(),
                study.description(),
                status,
                study.startDate(),
                study.endDate(),
                new String[0], // tags는 별도 테이블에서 조회 필요
                study.category(),
                study.subCategory()
        );
    }

    /**
     * ProjectVo → ProfileProjectVo 변환
     */
    private ProfileProjectVo toProfileProjectVo(ProjectVo project) {
        // Project 상태를 날짜 기준으로 계산
        OffsetDateTime now = OffsetDateTime.now();
        ProjectStatus status;
        if (project.endDate() != null && project.endDate().isBefore(now)) {
            status = ProjectStatus.COMPLETED;
        } else if (project.startDate() != null && project.startDate().isAfter(now)) {
            status = ProjectStatus.READY;
        } else {
            status = ProjectStatus.INPROGRESS;
        }
        
        return new ProfileProjectVo(
                project.id(),
                project.title(),
                project.description(),
                status,
                project.startDate(),
                project.endDate(),
                new String[0], // tags는 별도 테이블에서 조회 필요
                project.category(),
                project.subCategory()
        );
    }

    /**
     * BlogSummaryVo → ProfileBlogVo 변환
     */
    private ProfileBlogVo toProfileBlogVo(BlogSummaryVo blog) {
        // ArticleReferenceType 결정
        ArticleReferenceType referenceType = null;
        String referenceTitle = null;
        
        if (blog.studyId() != null) {
            referenceType = ArticleReferenceType.STUDY;
            referenceTitle = blog.studyTitle();
        } else if (blog.projectId() != null) {
            referenceType = ArticleReferenceType.PROJECT;
            referenceTitle = blog.projectTitle();
        }

        return new ProfileBlogVo(
                blog.id().value(), // BlogIdVo를 Long으로 변환
                blog.title(),
                blog.description(),
                ProjectStatus.COMPLETED, // 블로그는 발행된 상태로 간주
                blog.createdAt(),
                blog.updatedAt(),
                new String[0], // 태그는 별도 테이블에서 조회 필요
                blog.views() != null ? blog.views() : 0, // 조회수
                0, // 좋아요수는 별도 테이블에서 조회 필요
                blog.category(),
                referenceType,
                referenceTitle
        );
    }
}