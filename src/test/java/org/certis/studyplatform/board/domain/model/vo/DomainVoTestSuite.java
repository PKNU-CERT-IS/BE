package org.certis.studyplatform.board.domain.model.vo;

import org.junit.jupiter.api.Disabled;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@Disabled("Ignore suite in CI")
@SelectClasses({
        AttachmentVoTest.class,
        BoardContentVoTest.class,
        BoardCreationVoTest.class,
        BoardDescriptionVoTest.class,
        BoardIdVoTest.class,
        BoardSearchVoTest.class,
        BoardStatsVoTest.class,
        BoardTitleVoTest.class,
        BoardVoTest.class
})
public class DomainVoTestSuite {
}
