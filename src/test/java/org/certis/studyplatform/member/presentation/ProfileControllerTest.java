package org.certis.studyplatform.member.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.member.presentation.dto.request.ProfileUpdateRequestDto;
import org.certis.studyplatform.response.ResponseStatus;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.certis.studyplatform.schedule.infrastructure.persistence.entity.ScheduleEntity;
import org.certis.studyplatform.schedule.infrastructure.persistence.entity.ScheduleStatusEntity;
import org.certis.studyplatform.schedule.infrastructure.persistence.jpa.ScheduleJpaRepository;
import org.certis.studyplatform.schedule.infrastructure.persistence.jpa.ScheduleStatusJpaRepository;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberContactEntity;
import org.certis.studyplatform.member.infrastructure.persistence.jpa.MemberJpaRepository;
import org.certis.studyplatform.member.infrastructure.persistence.jpa.MemberContactJpaRepository;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("👤 Profile Controller 통합 테스트")
class ProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ScheduleJpaRepository scheduleJpaRepository;
    @Autowired private ScheduleStatusJpaRepository scheduleStatusJpaRepository;
    @Autowired private MemberJpaRepository memberJpaRepository;
    @Autowired private MemberContactJpaRepository memberContactJpaRepository;

    private Long testMemberId;

    @BeforeEach
    void setUp() {
        // 테스트용 멤버 데이터 생성
        testMemberId = createTestMemberData();
        // 테스트용 스케줄 데이터 생성
        createTestScheduleData();
    }

    private Long createTestMemberData() {
        // 기존 데이터 삭제
        memberContactJpaRepository.deleteAll();
        memberJpaRepository.deleteAll();
        
        // 테스트용 멤버 생성 (ID를 명시적으로 설정하지 않음)
        MemberEntity testMember = MemberEntity.builder()
                .name("테스트사용자")
                .studentNumber("20201234")
                .description("테스트용 사용자입니다")
                .skills(new String[]{"Java", "Spring Boot"})
                .major("컴퓨터공학과")
                .birthday(OffsetDateTime.now().minusYears(20))
                .gender("M")
                .role(MemberRole.UPSOLVER)
                .grade(MemberGrade.JUNIOR)
                .build();
        
        testMember = memberJpaRepository.save(testMember);
        
        // 테스트용 멤버 연락처 생성 (저장된 멤버의 실제 ID 사용)
        MemberContactEntity testMemberContact = MemberContactEntity.builder()
                .memberId(testMember.getId())
                .email("test@certis.org")
                .phoneNumber("010-0000-0000")
                .githubUrl("https://github.com/testuser")
                .linkedinUrl("https://www.linkedin.com/in/testuser")
                .build();
        
        memberContactJpaRepository.save(testMemberContact);
        
        return testMember.getId();
    }

    private void createTestScheduleData() {
        // 기존 데이터 삭제
        scheduleStatusJpaRepository.deleteAll();
        scheduleJpaRepository.deleteAll();

        // 오늘 날짜 범위에 해당하는 스케줄 생성
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startOfDay = now.toLocalDate().atStartOfDay().atOffset(now.getOffset());
        OffsetDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        // 오늘 오전 스케줄
        ScheduleEntity morningSchedule = ScheduleEntity.builder()
                .memberId(testMemberId) // 테스트용 멤버 ID
                .type("STUDY")
                .title("오전 스터디")
                .description("오전 스터디 설명")
                .place("강의실 A")
                .startedAt(startOfDay.plusHours(9))
                .endedAt(startOfDay.plusHours(11))
                .build();
        morningSchedule = scheduleJpaRepository.save(morningSchedule);

        // 오늘 오후 스케줄
        ScheduleEntity afternoonSchedule = ScheduleEntity.builder()
                .memberId(testMemberId)
                .type("PROJECT")
                .title("오후 프로젝트")
                .description("오후 프로젝트 설명")
                .place("강의실 B")
                .startedAt(startOfDay.plusHours(14))
                .endedAt(startOfDay.plusHours(16))
                .build();
        afternoonSchedule = scheduleJpaRepository.save(afternoonSchedule);

        // 스케줄 상태 생성 (APPROVED)
        ScheduleStatusEntity morningStatus = ScheduleStatusEntity.builder()
                .scheduleId(morningSchedule.getId())
                .status("APPROVED")
                .build();
        scheduleStatusJpaRepository.save(morningStatus);

        ScheduleStatusEntity afternoonStatus = ScheduleStatusEntity.builder()
                .scheduleId(afternoonSchedule.getId())
                .status("APPROVED")
                .build();
        scheduleStatusJpaRepository.save(afternoonStatus);
    }

    @Test
    @DisplayName("프로필 조회 - 200 OK")
    void getProfile_Success() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        mockMvc.perform(get("/api/v1/profile/me")
                .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()));
    }

    @Test
    @DisplayName("프로필 수정 - 200 OK")
    void updateProfile_Success() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        ProfileUpdateRequestDto request = ProfileUpdateRequestDto.builder()
                .name("MockStaff")
                .major("컴퓨터공학과")
                .description("통합테스트: 프로필 설명 수정")
                .studentNumber("20201234")
                .phoneNumber("010-0000-0000")
                .email("mock@certis.org")
                .skills(java.util.List.of("Java","Spring Boot"))
                .githubUrl("https://github.com/mock")
                .linkedinUrl("https://www.linkedin.com/in/mock")
                .build();

        mockMvc.perform(put("/api/v1/profile/me")
                        .with(user(mockUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_UPDATE_SUCCESS.getMessage()));
    }

    @Test
    @DisplayName("스터디 목록 조회 - 200 OK")
    void getStudies_Success() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        mockMvc.perform(get("/api/v1/profile/me/study")
                .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("프로젝트 목록 조회 - 200 OK")
    void getProjects_Success() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        mockMvc.perform(get("/api/v1/profile/me/project")
                .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("블로그 목록 조회 - 200 OK")
    void getBlogs_Success() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        mockMvc.perform(get("/api/v1/profile/me/blog")
                .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("블로그 목록 조회 - referenceType, referenceTitle 필드 포함")
    void getBlogs_WithReferenceFields() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        mockMvc.perform(get("/api/v1/profile/me/blog")
                .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].referenceType").exists())
                .andExpect(jsonPath("$.data[0].referenceTitle").exists());
    }

    @Test
    @DisplayName("프로필 수정 - URL 길이 제한 해제 테스트")
    void updateProfile_WithLongUrls() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        ProfileUpdateRequestDto request = ProfileUpdateRequestDto.builder()
                .name("MockStaff")
                .major("컴퓨터공학과")
                .description("통합테스트: 프로필 설명 수정")
                .studentNumber("20201234")
                .phoneNumber("010-0000-0000")
                .email("mock@certis.org")
                .skills(java.util.List.of("Java","Spring Boot"))
                .githubUrl("https://github.com/mock/very/long/url/that/exceeds/previous/limit/of/2000/characters/" + 
                          "a".repeat(3000)) // 3000자 이상의 긴 URL
                .linkedinUrl("https://www.linkedin.com/in/mock/very/long/url/that/exceeds/previous/limit/of/2000/characters/" + 
                            "b".repeat(3000)) // 3000자 이상의 긴 URL
                .build();

        mockMvc.perform(put("/api/v1/profile/me")
                        .with(user(mockUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_UPDATE_SUCCESS.getMessage()));
    }

    @Test
    @DisplayName("프로필 조회 - todaySchedules 필드 확인")
    void getProfile_WithScheduleFields() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        mockMvc.perform(get("/api/v1/profile/me")
                .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.todaySchedules").exists());
    }
}


