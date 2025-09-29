package org.certis.studyplatform.study.application.object.command;

import lombok.Builder;
import lombok.Getter;


/**
 * End Study Command
 *
 * 스터디 종료 요청을 위한 Command 객체
 * Clean Architecture Application Layer
 */
@Getter
@Builder
public class EndStudyCommand {
    
    private final Long studyId;
    private final Long requesterId;
    private final String attachmentUrl;
    
    public static EndStudyCommand of(Long studyId, Long requesterId, String attachmentUrl) {
        return EndStudyCommand.builder()
                .studyId(studyId)
                .requesterId(requesterId)
                .attachmentUrl(attachmentUrl)
                .build();
    }
    
    public Long studyId() {
        return studyId;
    }
    
    public Long requesterId() {
        return requesterId;
    }
    
    public String attachment() {
        return attachmentUrl;
    }
}
