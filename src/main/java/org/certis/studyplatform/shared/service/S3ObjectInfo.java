package org.certis.studyplatform.shared.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class S3ObjectInfo {
    private final String name;
    private final String contentType;
    private final Long size;
    private final String url;
}


