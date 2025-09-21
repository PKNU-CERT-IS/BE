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
    private final List<MultipartFile> files;
    
    public static EndStudyCommand of(Long studyId, Long requesterId, List<MultipartFile> files) {
        return EndStudyCommand.builder()
                .studyId(studyId)
                .requesterId(requesterId)
                .files(files)
                .build();
    }
    
    public Long studyId() {
        return studyId;
    }
    
    public Long requesterId() {
        return requesterId;
    }
    
    public List<MultipartFile> files() {
        return files;
    }
}
