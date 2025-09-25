package org.certis.studyplatform.project.application.object.command;

import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * End Project Command
 *
 * 프로젝트 종료 요청을 위한 Command 객체
 * Clean Architecture Application Layer
 */
@Getter
@Builder
public class EndProjectCommand {
    
    private final Long projectId;
    private final Long requesterId;
    private final MultipartFile attachment;
    
    public static EndProjectCommand of(Long projectId, Long requesterId, MultipartFile attachment) {
        return EndProjectCommand.builder()
                .projectId(projectId)
                .requesterId(requesterId)
                .attachment(attachment)
                .build();
    }
    
    public Long projectId() {
        return projectId;
    }
    
    public Long requesterId() {
        return requesterId;
    }
    
    public MultipartFile attachment() {
        return attachment;
    }
}
