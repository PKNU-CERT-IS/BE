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
import org.springframework.security.test.context.support.WithMockUser;
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

    private static final Long TEST_MEMBER_ID = 1L;

    @Test
    @WithMockUser(username = "user1", roles = "UPSOLVER")
    @DisplayName("프로필 조회 - 200 OK")
    void getProfile_Success() throws Exception {
        mockMvc.perform(get("/api/v1/profile/me/{memberId}", TEST_MEMBER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()));
    }

    @Test
    @WithMockUser(username = "user1", roles = "UPSOLVER")
    @DisplayName("프로필 수정 - 200 OK")
    void updateProfile_Success() throws Exception {
        ProfileUpdateRequestDto request = ProfileUpdateRequestDto.builder()
                .description("통합테스트: 프로필 설명 수정")
                .build();

        mockMvc.perform(put("/api/v1/profile/me/{memberId}", TEST_MEMBER_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_UPDATE_SUCCESS.getMessage()));
    }

    @Test
    @WithMockUser(username = "user1", roles = "UPSOLVER")
    @DisplayName("스터디 목록 조회 - 200 OK")
    void getStudies_Success() throws Exception {
        mockMvc.perform(get("/api/v1/profile/{memberId}/study", TEST_MEMBER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "user1", roles = "UPSOLVER")
    @DisplayName("프로젝트 목록 조회 - 200 OK")
    void getProjects_Success() throws Exception {
        mockMvc.perform(get("/api/v1/profile/{memberId}/project", TEST_MEMBER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "user1", roles = "UPSOLVER")
    @DisplayName("블로그 목록 조회 - 200 OK")
    void getBlogs_Success() throws Exception {
        mockMvc.perform(get("/api/v1/profile/{memberId}/blog", TEST_MEMBER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray());
    }
}


