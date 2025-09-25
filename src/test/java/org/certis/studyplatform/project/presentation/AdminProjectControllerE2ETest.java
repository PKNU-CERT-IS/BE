package org.certis.studyplatform.project.presentation;

import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectEntity;
import org.certis.studyplatform.project.infrastructure.persistence.jpa.ProjectJpaRepository;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.certis.studyplatform.shared.service.S3FileService;
import org.certis.studyplatform.shared.service.S3ObjectInfo;
import org.certis.studyplatform.project.domain.service.ProjectDomainService;
import org.certis.studyplatform.project.domain.vo.ProjectEndSubmissionInfoVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminProjectControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectJpaRepository projectJpaRepository;

    @MockBean
    private ProjectDomainService projectDomainService;

    @MockBean
    private S3FileService s3FileService;

    @Test
    @DisplayName("GET /api/v1/admin/project/end/{id} returns attachment built from S3 metadata")
    @WithMockUser(username = "admin", roles = {"STAFF"})
    void getProjectEndSubmission_returnsAttachmentFromS3() throws Exception {
        // given
        Long projectId = 100L;
        String s3Url = "https://bucket.s3.ap-northeast-2.amazonaws.com/project-end-attachments/100/file.pdf";
        ProjectEntity entity = ProjectEntity.builder()
                .id(projectId)
                .resultSubmitStatus(ResultSubmitStatus.INPROGRESS)
                .resultSubmittedAt(OffsetDateTime.parse("2025-09-20T10:00:00Z"))
                .resultAttachmentUrl(s3Url)
                .build();
        when(projectJpaRepository.findById(anyLong())).thenReturn(Optional.of(entity));
        // mock domain service to provide status/submittedAt/url to facade
        ProjectEndSubmissionInfoVo infoVo = new ProjectEndSubmissionInfoVo(
                projectId,
                ResultSubmitStatus.INPROGRESS,
                OffsetDateTime.parse("2025-09-20T10:00:00Z"),
                s3Url,
                "CS",
                "BE",
                "Project Title",
                "Project Desc",
                999L,
                OffsetDateTime.parse("2025-09-01T00:00:00Z"),
                OffsetDateTime.parse("2025-10-01T00:00:00Z"),
                4,
                8
        );
        when(projectDomainService.getEndSubmissionInfo(anyLong())).thenReturn(infoVo);
        when(s3FileService.getObjectInfo(s3Url)).thenReturn(new S3ObjectInfo(
                "file.pdf", "application/pdf", 12345L, s3Url
        ));

        // when & then
        mockMvc.perform(get("/api/v1/admin/project/end/{projectId}", projectId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(projectId))
                .andExpect(jsonPath("$.data.status").value("INPROGRESS"))
                .andExpect(jsonPath("$.data.attachment.name").value("file.pdf"))
                .andExpect(jsonPath("$.data.attachment.type").value("application/pdf"))
                .andExpect(jsonPath("$.data.attachment.size").value("12345"))
                .andExpect(jsonPath("$.data.attachment.attachedUrl").value(s3Url))
                .andExpect(jsonPath("$.data.category").value("CS"))
                .andExpect(jsonPath("$.data.subCategory").value("BE"))
                .andExpect(jsonPath("$.data.title").value("Project Title"))
                .andExpect(jsonPath("$.data.description").value("Project Desc"))
                .andExpect(jsonPath("$.data.creatorId").value(999))
                .andExpect(jsonPath("$.data.currentParticipantNumber").value(4))
                .andExpect(jsonPath("$.data.maxParticipantNumber").value(8));
    }
}


