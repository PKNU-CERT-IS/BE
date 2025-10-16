package org.certis.studyplatform.study.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.study.presentation.dto.request.AdminStudyUpdateRequestDto;
import org.certis.studyplatform.study.application.StudyFacadeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import static org.mockito.Mockito.mock;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.mockito.Mockito.atLeastOnce;
import org.springframework.test.annotation.DirtiesContext;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestEmbeddedPostgresConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminStudyControllerUpdateStatusTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudyFacadeService studyFacadeService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        StudyFacadeService studyFacadeService() {
            return mock(StudyFacadeService.class);
        }
    }

    @Test
    @DisplayName("Admin Study update: INPROGRESS → startDate moved future → handled by facade")
    @WithMockUser(username = "admin", roles = {"STAFF"})
    void adminUpdate_inprogressStartMovedFuture_invokesFacadeWithDates() throws Exception {
        // given
        AdminStudyUpdateRequestDto dto = new AdminStudyUpdateRequestDto();
        dto.setStudyId(1L);
        dto.setTitle("T");
        dto.setStartDate(OffsetDateTime.now().plusDays(14));
        dto.setEndDate(OffsetDateTime.now().plusDays(30));

        doNothing().when(studyFacadeService).updateStudyByAdmin(org.mockito.Mockito.any(AdminStudyUpdateRequestDto.class), anyLong());

        // when
        CurrentUser principal = new CurrentUser(999L, "admin", "admin@test.com", "관리자", "STAFF");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(put("/api/v1/admin/study/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk());

        // then - verify facade was called with provided dates
        ArgumentCaptor<AdminStudyUpdateRequestDto> captor = ArgumentCaptor.forClass(AdminStudyUpdateRequestDto.class);
        verify(studyFacadeService, atLeastOnce()).updateStudyByAdmin(captor.capture(), anyLong());
        AdminStudyUpdateRequestDto captured = captor.getValue();
        assertThat(captured.getStudyId()).isEqualTo(1L);
        assertThat(captured.getStartDate()).isEqualTo(dto.getStartDate());
        assertThat(captured.getEndDate()).isEqualTo(dto.getEndDate());
    }

    @Test
    @DisplayName("Admin Study update: READY → date change → handled by facade call")
    @WithMockUser(username = "admin", roles = {"STAFF"})
    void adminUpdate_readyDateChange_invokesFacade() throws Exception {
        AdminStudyUpdateRequestDto dto = new AdminStudyUpdateRequestDto();
        dto.setStudyId(2L);
        dto.setTitle("T2");
        dto.setStartDate(OffsetDateTime.now().plusDays(7));
        dto.setEndDate(OffsetDateTime.now().plusDays(21));

        doNothing().when(studyFacadeService).updateStudyByAdmin(org.mockito.Mockito.any(AdminStudyUpdateRequestDto.class), anyLong());

        CurrentUser principal2 = new CurrentUser(999L, "admin", "admin@test.com", "관리자", "STAFF");
        Authentication auth2 = new UsernamePasswordAuthenticationToken(principal2, null, principal2.getAuthorities());

        mockMvc.perform(put("/api/v1/admin/study/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth2)))
                .andExpect(status().isOk());

        ArgumentCaptor<AdminStudyUpdateRequestDto> captor = ArgumentCaptor.forClass(AdminStudyUpdateRequestDto.class);
        verify(studyFacadeService, atLeastOnce()).updateStudyByAdmin(captor.capture(), anyLong());
        assertThat(captor.getValue().getStudyId()).isEqualTo(2L);
    }
}


