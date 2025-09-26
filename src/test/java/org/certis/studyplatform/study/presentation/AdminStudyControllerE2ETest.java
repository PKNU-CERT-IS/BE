package org.certis.studyplatform.study.presentation;

import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.study.application.StudyFacadeService;
import org.certis.studyplatform.study.application.StudyParticipantFacadeService;
import org.certis.studyplatform.study.presentation.dto.response.AdminStudyEndSubmissionResponseDto;
import org.certis.studyplatform.study.presentation.dto.response.StudyAttachedResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminStudyController.class)
class AdminStudyControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudyFacadeService studyFacadeService;

    @MockBean
    private StudyParticipantFacadeService studyParticipantFacadeService;

    @MockBean
    private org.certis.studyplatform.shared.security.JwtTokenProvider jwtTokenProvider;

    // No S3 in WebMvcTest slice; facade returns fully built DTO

    @Test
    @WithMockUser(username = "admin", roles = {"STAFF"})
    @DisplayName("GET /api/v1/admin/study/end/{id} returns single attachment built from S3 metadata")
    void getStudyEndSubmission_returnsAttachmentFromS3() throws Exception {
        // given
        Long studyId = 200L;
        String s3Url = "https://bucket.s3.ap-northeast-2.amazonaws.com/study-end-attachments/200/file2.pdf";
        
        AdminStudyEndSubmissionResponseDto dto = AdminStudyEndSubmissionResponseDto.builder()
                .studyId(studyId)
                .status(ResultSubmitStatus.INPROGRESS)
                .submittedAt(OffsetDateTime.parse("2025-09-20T10:00:00Z"))
                .attachment(StudyAttachedResponseDto.builder()
                        .name("file2.pdf")
                        .type("application/pdf")
                        .size("54321")
                        .attachedUrl(s3Url)
                        .build())
                .category("CS")
                .subCategory("BE")
                .title("file2.pdf")
                .description("Study Desc")
                .creatorId(999L)
                .studyCreatorName("홍길동")
                .studyCreatorGrade(MemberGrade.SENIOR)
                .startedAt(OffsetDateTime.parse("2025-09-01T00:00:00Z"))
                .endedAt(OffsetDateTime.parse("2025-10-01T00:00:00Z"))
                .currentParticipantNumber(3)
                .maxParticipantNumber(10)
                .build();

        when(studyFacadeService.getAdminStudyEndSubmission(anyLong())).thenReturn(dto);

        // when & then
        mockMvc.perform(get("/api/v1/admin/study/end/{studyId}", studyId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.study_id").value(studyId))
                .andExpect(jsonPath("$.data.status").value("INPROGRESS"))
                .andExpect(jsonPath("$.data.attachment.name").value("file2.pdf"))
                .andExpect(jsonPath("$.data.attachment.type").value("application/pdf"))
                .andExpect(jsonPath("$.data.attachment.size").value("54321"))
                .andExpect(jsonPath("$.data.attachment.attached_url").value(s3Url))
                .andExpect(jsonPath("$.data.category").value("CS"))
                .andExpect(jsonPath("$.data.sub_category").value("BE"))
                .andExpect(jsonPath("$.data.title").value("file2.pdf"))
                .andExpect(jsonPath("$.data.description").value("Study Desc"))
                .andExpect(jsonPath("$.data.creator_id").value(999))
                .andExpect(jsonPath("$.data.study_creator_name").value("홍길동"))
                .andExpect(jsonPath("$.data.study_creator_grade").value("SENIOR"))
                .andExpect(jsonPath("$.data.current_participant_number").value(3))
                .andExpect(jsonPath("$.data.max_participant_number").value(10));
    }
}


