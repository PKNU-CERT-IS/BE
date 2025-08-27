package org.certis.studyplatform.project.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Project Attached Response DTO
 *
 * 프로젝트 첨부파일 정보를 반환하는 DTO
 * Presentation Layer의 응답 DTO
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAttachedResponseDto {

    private Long id;

    private String name;

    private String type;

    private String size;

    private String attachedUrl;

    // toString for logging
    @Override
    public String toString() {
        return "ProjectAttachedResponseDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", size='" + size + '\'' +
                '}';
    }
}
