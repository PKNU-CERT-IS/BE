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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminStudyControllerUpdateStatusTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StudyFacadeService studyFacadeService;

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
        verify(studyFacadeService).updateStudyByAdmin(captor.capture(), anyLong());
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
        verify(studyFacadeService).updateStudyByAdmin(captor.capture(), anyLong());
        assertThat(captor.getValue().getStudyId()).isEqualTo(2L);
    }
}


