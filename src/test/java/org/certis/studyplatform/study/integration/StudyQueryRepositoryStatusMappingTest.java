package org.certis.studyplatform.study.integration;

import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
import org.certis.studyplatform.study.domain.vo.StudyVo;
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
import static org.certis.generated.jooq.Tables.STUDY;

@SpringBootTest
@Import({TestEmbeddedPostgresConfig.class, TestRedisMockConfig.class})
@ActiveProfiles("test")
@Transactional
class StudyQueryRepositoryStatusMappingTest {

    @Autowired
    private DSLContext dsl;

    @Autowired
    private StudyQueryRepository studyQueryRepository;

    @Test
    @DisplayName("findVoByIdForStatusCheck returns DB status as-is, ignoring endedAt")
    void findVoByIdForStatusCheck_mapsDbStatusIgnoringEndedAt() {
        // Given: Insert a row with ended_at in the past but status APPROVED
        OffsetDateTime now = OffsetDateTime.now();

        // Satisfy FK: insert minimal member
        dsl.insertInto(org.certis.generated.jooq.tables.Member.MEMBER)
                .set(org.certis.generated.jooq.tables.Member.MEMBER.ID, 202L)
                .set(org.certis.generated.jooq.tables.Member.MEMBER.NAME, "tester-202")
                .set(org.certis.generated.jooq.tables.Member.MEMBER.STUDENT_NUMBER, "20200001")
                .set(org.certis.generated.jooq.tables.Member.MEMBER.ROLE, "PLAYER")
                .set(org.certis.generated.jooq.tables.Member.MEMBER.GRADE, "JUNIOR")
                .set(org.certis.generated.jooq.tables.Member.MEMBER.BIRTHDAY, now.minusYears(20))
                .set(org.certis.generated.jooq.tables.Member.MEMBER.GENDER, "MALE")
                .set(org.certis.generated.jooq.tables.Member.MEMBER.MAJOR, "CS")
                .set(org.certis.generated.jooq.tables.Member.MEMBER.CREATED_AT, now)
                .set(org.certis.generated.jooq.tables.Member.MEMBER.UPDATED_AT, now)
                .onConflict(org.certis.generated.jooq.tables.Member.MEMBER.ID).doNothing()
                .execute();

        Long studyId = dsl.insertInto(STUDY)
                .set(STUDY.MEMBER_ID, 202L)
                .set(STUDY.TITLE, "status-check")
                .set(STUDY.DESCRIPTION, "desc")
                .set(STUDY.CONTENT, "content")
                .set(STUDY.CATEGORY, "CS")
                .set(STUDY.SUBCATEGORY, "BE")
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 5)
                .set(STUDY.STARTED_AT, now.minusDays(10))
                .set(STUDY.ENDED_AT, now.minusDays(1)) // would imply completed by time
                .set(STUDY.STATUS, "APPROVED") // but DB says APPROVED
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.CREATED_AT, now.minusDays(11))
                .set(STUDY.UPDATED_AT, now.minusDays(1))
                .returning(STUDY.ID)
                .fetchOne()
                .get(STUDY.ID);

        // When
        Optional<StudyVo> voOpt = studyQueryRepository.findVoByIdForStatusCheck(studyId);

        // Then
        assertThat(voOpt).isPresent();
        assertThat(voOpt.get().status()).isEqualTo("APPROVED");
    }
}


