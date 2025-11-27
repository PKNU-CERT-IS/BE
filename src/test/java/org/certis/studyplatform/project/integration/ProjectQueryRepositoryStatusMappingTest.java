package org.certis.studyplatform.project.integration;

import org.certis.studyplatform.project.domain.repository.ProjectQueryRepository;
import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestRedisMockConfig;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.certis.generated.jooq.Tables.MEMBER;
import static org.certis.generated.jooq.Tables.PROJECT;

@SpringBootTest
@Import({TestEmbeddedPostgresConfig.class, TestRedisMockConfig.class})
@ActiveProfiles("test")
@Transactional
class ProjectQueryRepositoryStatusMappingTest {

    @Autowired
    private DSLContext dsl;

    @Autowired
    private ProjectQueryRepository projectQueryRepository;

    @Test
    @DisplayName("findVoByIdForStatusCheck returns DB status as-is, ignoring endedAt (Project)")
    void findVoByIdForStatusCheck_mapsDbStatusIgnoringEndedAt_project() {
        OffsetDateTime now = OffsetDateTime.now();

        // Satisfy FK: insert minimal member
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, 404L)
                .set(MEMBER.NAME, "tester-404")
                .set(MEMBER.STUDENT_NUMBER, "20200004")
                .set(MEMBER.ROLE, "PLAYER")
                .set(MEMBER.GRADE, "JUNIOR")
                .set(MEMBER.BIRTHDAY, now.minusYears(22))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.MAJOR, "CS")
                .set(MEMBER.CREATED_AT, now)
                .set(MEMBER.UPDATED_AT, now)
                .onConflict(MEMBER.ID).doNothing()
                .execute();

        Long projectId = dsl.insertInto(PROJECT)
                .set(PROJECT.MEMBER_ID, 404L)
                .set(PROJECT.TITLE, "status-check-proj")
                .set(PROJECT.DESCRIPTION, "desc")
                .set(PROJECT.CONTENT, "content")
                .set(PROJECT.CATEGORY, "CS")
                .set(PROJECT.SUBCATEGORY, "BE")
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.STARTED_AT, now.minusDays(10))
                .set(PROJECT.ENDED_AT, now.minusDays(1))
                .set(PROJECT.STATUS, "APPROVED")
                .set(PROJECT.RESULT_SUBMIT_STATUS, "READY")
                .set(PROJECT.CREATED_AT, now.minusDays(11))
                .set(PROJECT.UPDATED_AT, now.minusDays(1))
                .returning(PROJECT.ID)
                .fetchOne()
                .get(PROJECT.ID);

        Optional<ProjectVo> voOpt = projectQueryRepository.findVoByIdForStatusCheck(projectId);

        assertThat(voOpt).isPresent();
        assertThat(voOpt.get().status()).isEqualTo("APPROVED");
    }
}



