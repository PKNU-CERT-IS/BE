package org.certis.studyplatform.study.application.object.command;

import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    private final MultipartFile attachment;
    
    public static EndStudyCommand of(Long studyId, Long requesterId, MultipartFile attachment) {
        return EndStudyCommand.builder()
                .studyId(studyId)
                .requesterId(requesterId)
                .attachment(attachment)
                .build();
    }
    
    public Long studyId() {
        return studyId;
    }
    
    public Long requesterId() {
        return requesterId;
    }
    
    public MultipartFile attachment() {
        return attachment;
    }
}
