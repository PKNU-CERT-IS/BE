package org.certis.studyplatform.project.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.project.presentation.dto.request.AdminProjectUpdateRequestDto;
import org.certis.studyplatform.project.application.ProjectFacadeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import static org.mockito.Mockito.mock;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.mockito.Mockito;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestEmbeddedPostgresConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminProjectControllerUpdateStatusTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectFacadeService projectFacadeService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        ProjectFacadeService projectFacadeService() {
            return mock(ProjectFacadeService.class);
        }
    }

    @Test
    @DisplayName("Admin Project update: INPROGRESS → startDate moved future → keep handled by facade")
    @WithMockUser(username = "admin", roles = {"STAFF"})
    void adminUpdate_inprogressStartMovedFuture_invokesFacadeWithDates() throws Exception {
        // given
        AdminProjectUpdateRequestDto dto = new AdminProjectUpdateRequestDto();
        dto.setProjectId(1L);
        dto.setTitle("T");
        dto.setStartDate(OffsetDateTime.now().plusDays(14));
        dto.setEndDate(OffsetDateTime.now().plusDays(30));

        doNothing().when(projectFacadeService).updateProjectByAdmin(Mockito.any(AdminProjectUpdateRequestDto.class), anyLong());

        // when
        CurrentUser principal = new CurrentUser(999L, "admin", "admin@test.com", "관리자", "STAFF");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(put("/api/v1/admin/project/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk());

        // then - verify facade was called with provided dates
        ArgumentCaptor<AdminProjectUpdateRequestDto> captor = ArgumentCaptor.forClass(AdminProjectUpdateRequestDto.class);
        verify(projectFacadeService, atLeastOnce()).updateProjectByAdmin(captor.capture(), anyLong());
        AdminProjectUpdateRequestDto captured = captor.getValue();
        assertThat(captured.getProjectId()).isEqualTo(1L);
        assertThat(captured.getStartDate()).isEqualTo(dto.getStartDate());
        assertThat(captured.getEndDate()).isEqualTo(dto.getEndDate());
    }

    @Test
    @DisplayName("Admin Project update: READY → date change → stays handled by facade")
    @WithMockUser(username = "admin", roles = {"STAFF"})
    void adminUpdate_readyDateChange_invokesFacade() throws Exception {
        AdminProjectUpdateRequestDto dto = new AdminProjectUpdateRequestDto();
        dto.setProjectId(2L);
        dto.setTitle("T2");
        dto.setStartDate(OffsetDateTime.now().plusDays(7));
        dto.setEndDate(OffsetDateTime.now().plusDays(21));

        doNothing().when(projectFacadeService).updateProjectByAdmin(Mockito.any(AdminProjectUpdateRequestDto.class), anyLong());

        CurrentUser principal2 = new CurrentUser(999L, "admin", "admin@test.com", "관리자", "STAFF");
        Authentication auth2 = new UsernamePasswordAuthenticationToken(principal2, null, principal2.getAuthorities());

        mockMvc.perform(put("/api/v1/admin/project/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth2)))
                .andExpect(status().isOk());

        ArgumentCaptor<AdminProjectUpdateRequestDto> captor = ArgumentCaptor.forClass(AdminProjectUpdateRequestDto.class);
        verify(projectFacadeService, atLeastOnce()).updateProjectByAdmin(captor.capture(), anyLong());
        assertThat(captor.getValue().getProjectId()).isEqualTo(2L);
    }
}


