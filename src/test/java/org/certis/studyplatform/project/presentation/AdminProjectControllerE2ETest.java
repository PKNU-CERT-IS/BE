package org.certis.studyplatform.project.presentation;

import org.certis.studyplatform.project.domain.ProjectStatus;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.certis.studyplatform.shared.service.S3FileService;
import org.certis.studyplatform.shared.service.S3ObjectInfo;
import org.certis.studyplatform.project.domain.service.ProjectDomainService;
import org.certis.studyplatform.project.domain.vo.ProjectEndSubmissionInfoVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.springframework.test.annotation.DirtiesContext;
import org.jooq.DSLContext;

import java.time.OffsetDateTime;

import static org.certis.generated.jooq.Tables.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestEmbeddedPostgresConfig.class, AdminProjectControllerE2ETest.TestConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminProjectControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private ProjectDomainService projectDomainService;

    @Autowired
    private S3FileService s3FileService;

    // TestConfiguration을 static inner class로 이동
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ProjectDomainService projectDomainService() {
            return Mockito.mock(ProjectDomainService.class);
        }

        @Bean
        @Primary
        public S3FileService s3FileService() {
            return Mockito.mock(S3FileService.class);
        }
    }

    @BeforeEach
    void setUp() {
        // Mock 초기화
        Mockito.reset(projectDomainService, s3FileService);
        
        // DB 정리
        dsl.execute("TRUNCATE TABLE project_participant RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE project RESTART IDENTITY CASCADE");
        dsl.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");
        
        OffsetDateTime now = OffsetDateTime.now();
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, 999L)
                .set(MEMBER.NAME, "관리자")
                .set(MEMBER.STUDENT_NUMBER, "20240999")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.GRADE, "SENIOR")
                .set(MEMBER.ROLE, "STAFF")
                .set(MEMBER.BIRTHDAY, now.minusYears(25))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, now)
                .set(MEMBER.UPDATED_AT, now)
                .execute();
    }

    @Test
    @DisplayName("GET /api/v1/admin/project/end/{id} returns attachment built from S3 metadata")
    @WithMockUser(username = "admin", roles = {"STAFF"})
    void getProjectEndSubmission_returnsAttachmentFromS3() throws Exception {
        // Given
        Long projectId = 100L;
        String s3Url = "https://bucket.s3.ap-northeast-2.amazonaws.com/project-end-attachments/100/file.pdf";
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime submittedAt = now.minusDays(1);
        OffsetDateTime projectStartAt = now.minusDays(20);
        OffsetDateTime projectEndAt = now.plusDays(10);

        dsl.insertInto(PROJECT)
                .set(PROJECT.ID, projectId)
                .set(PROJECT.TITLE, "Project Title")
                .set(PROJECT.DESCRIPTION, "Project Desc")
                .set(PROJECT.CONTENT, "Content")
                .set(PROJECT.CATEGORY, "CS")
                .set(PROJECT.SUBCATEGORY, "BE")
                .set(PROJECT.STATUS, "INPROGRESS")
                .set(PROJECT.RESULT_SUBMIT_STATUS, "INPROGRESS")
                .set(PROJECT.RESULT_SUBMITTED_AT, submittedAt)
                .set(PROJECT.RESULT_ATTACHED_URL, s3Url)
                .set(PROJECT.MEMBER_ID, 999L)
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 8)
                .set(PROJECT.STARTED_AT, projectStartAt)
                .set(PROJECT.ENDED_AT, projectEndAt)
                .set(PROJECT.CREATED_AT, now)
                .set(PROJECT.UPDATED_AT, now)
                .execute();

        ProjectEndSubmissionInfoVo infoVo = new ProjectEndSubmissionInfoVo(
                projectId,
                ProjectStatus.INPROGRESS,
                ResultSubmitStatus.INPROGRESS,
                submittedAt,
                s3Url,
                "CS",
                "BE",
                "Project Title",
                "Project Desc",
                999L,
                "관리자",
                MemberGrade.SENIOR,
                projectStartAt,
                projectEndAt,
                4,
                8
        );
        when(projectDomainService.getEndSubmissionInfo(projectId)).thenReturn(infoVo);
        when(s3FileService.getObjectInfo(s3Url)).thenReturn(new S3ObjectInfo(
                "file.pdf", "application/pdf", 12345L, s3Url
        ));
        when(s3FileService.toPresignedUrl(s3Url)).thenReturn(s3Url);

        // When & Then
        mockMvc.perform(get("/api/v1/admin/project/end/{projectId}", projectId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
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
                .andExpect(jsonPath("$.data.projectCreatorName").value("관리자"))
                .andExpect(jsonPath("$.data.projectCreatorGrade").value("SENIOR"))
                .andExpect(jsonPath("$.data.currentParticipantNumber").value(4))
                .andExpect(jsonPath("$.data.maxParticipantNumber").value(8));
    }
}


