package org.certis.studyplatform.study.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Study Attached Response DTO
 *
 * 스터디 첨부파일 정보를 반환하는 DTO
 * Presentation Layer의 응답 DTO
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StudyAttachedResponseDto {

    private Long id;

    private String name;

    private String type;

    private String size;

    private String attachedUrl;

    // toString for logging
    @Override
    public String toString() {
        return "StudyAttachedResponseDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", size='" + size + '\'' +
                '}';
    }
}
