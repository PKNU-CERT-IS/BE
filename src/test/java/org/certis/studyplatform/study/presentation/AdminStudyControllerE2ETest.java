package org.certis.studyplatform.study.presentation;

import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.certis.studyplatform.shared.service.S3FileService;
import org.certis.studyplatform.shared.service.S3ObjectInfo;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyEntity;
import org.certis.studyplatform.study.infrastructure.persistence.jpa.StudyJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
class AdminStudyControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudyJpaRepository studyJpaRepository;

    @MockBean
    private S3FileService s3FileService;

    @Test
    @DisplayName("GET /api/v1/admin/study/end/{id} returns single attachment built from S3 metadata")
    void getStudyEndSubmission_returnsAttachmentFromS3() throws Exception {
        // given
        Long studyId = 200L;
        String s3Url = "https://bucket.s3.ap-northeast-2.amazonaws.com/study-end-attachments/200/file2.pdf";
        StudyEntity entity = StudyEntity.builder()
                .id(studyId)
                .resultSubmitStatus(ResultSubmitStatus.INPROGRESS)
                .resultSubmittedAt(OffsetDateTime.parse("2025-09-20T10:00:00Z"))
                .resultAttachmentUrl(s3Url)
                .build();
        when(studyJpaRepository.findById(anyLong())).thenReturn(Optional.of(entity));
        when(s3FileService.getObjectInfo(s3Url)).thenReturn(new S3ObjectInfo(
                "file2.pdf", "application/pdf", 54321L, s3Url
        ));

        // when & then
        mockMvc.perform(get("/api/v1/admin/study/end/{studyId}", studyId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studyId").value(studyId))
                .andExpect(jsonPath("$.data.status").value("INPROGRESS"))
                .andExpect(jsonPath("$.data.attachment.name").value("file2.pdf"))
                .andExpect(jsonPath("$.data.attachment.type").value("application/pdf"))
                .andExpect(jsonPath("$.data.attachment.size").value("54321"))
                .andExpect(jsonPath("$.data.attachment.attachedUrl").value(s3Url));
    }
}


