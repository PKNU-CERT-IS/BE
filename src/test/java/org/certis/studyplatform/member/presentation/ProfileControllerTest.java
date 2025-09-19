package org.certis.studyplatform.member.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.member.presentation.dto.request.ProfileUpdateRequestDto;
import org.certis.studyplatform.response.ResponseStatus;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("👤 Profile Controller 통합 테스트")
class ProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("프로필 조회 - 200 OK")
    void getProfile_Success() throws Exception {
        mockMvc.perform(get("/api/v1/profile/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()));
    }

    @Test
    @DisplayName("프로필 수정 - 200 OK")
    void updateProfile_Success() throws Exception {
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
        mockMvc.perform(get("/api/v1/profile/me/study"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("프로젝트 목록 조회 - 200 OK")
    void getProjects_Success() throws Exception {
        mockMvc.perform(get("/api/v1/profile/me/project"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("블로그 목록 조회 - 200 OK")
    void getBlogs_Success() throws Exception {
        mockMvc.perform(get("/api/v1/profile/me/blog"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray());
    }
}


