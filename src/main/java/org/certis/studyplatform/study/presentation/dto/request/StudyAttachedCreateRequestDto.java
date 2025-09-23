package org.certis.studyplatform.study.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import org.certis.studyplatform.shared.type.AttachedType;


@Getter
@Setter
public class StudyAttachedCreateRequestDto {
    @NotNull(message = "파일명은 필수입니다")
    private String name;

    @NotNull(message = "파일 타입은 필수입니다")
    private AttachedType type;

    private Long size;

    // 프론트엔드에서 FileReader로 변형된 파일 데이터 (Base64 또는 바이너리)
    private String fileData;
    
    // 기존 파일 URL (수정 시 기존 파일 유지용)
    private String attachedUrl;
}
