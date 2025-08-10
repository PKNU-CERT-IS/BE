package org.certis.studyplatform.member.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

@Getter
@Builder
public class MemberCreatedResponseDto {

    private final Long id;

    private final String name;

    private final String studentNumber;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private final OffsetDateTime createdAt;

    // toString for logging
    @Override
    public String toString() {
        return "MemberCreatedResponseDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", studentNumber='" + studentNumber + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}