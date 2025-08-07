package org.certis.studyplatform.member.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.ZonedDateTime;

@Getter
@Builder
public class MemberUpdatedResponseDto {

    private final Long id;

    private final String name;

    private final String profileImage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private final ZonedDateTime updatedAt;

    // toString for logging
    @Override
    public String toString() {
        return "MemberUpdatedResponseDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", profileImage='" + profileImage + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
