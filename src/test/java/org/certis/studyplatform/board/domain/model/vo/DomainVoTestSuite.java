package org.certis.studyplatform.board.domain.model.vo;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
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
