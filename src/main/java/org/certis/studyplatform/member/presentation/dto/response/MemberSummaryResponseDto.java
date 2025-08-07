package org.certis.studyplatform.member.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberSummaryResponseDto {

    private final Long id;

    private final String name;

    private final String grade;

    private final String role;

    // toString for logging
    @Override
    public String toString() {
        return "MemberSummaryResponseDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", grade='" + grade + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
