package org.certis.studyplatform.study.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * Study End Request DTO
 *
 * 스터디 종료 요청을 위한 DTO
 * Presentation Layer
 */
@Getter
@Setter
@NoArgsConstructor
public class StudyEndRequestDto {
    
    @NotNull(message = "스터디 ID는 필수입니다")
    private Long studyId;
    @NotNull(message = "종료 신청 보고서는 필수입니다")
    private MultipartFile attachment;
    
    public StudyEndRequestDto(Long studyId, MultipartFile attachment) {
        this.studyId = studyId;
        this.attachment = attachment;
    }
}