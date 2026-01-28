package org.certis.studyplatform.file.domain.vo;

import lombok.Builder;

@Builder
public record FileVo(
        String url,
        String fileName
) {
    public static FileVo of(String url, String fileName) {
        return FileVo.builder()
                .url(url)
                .fileName(fileName)
                .build();
    }
}
