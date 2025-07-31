package org.certis.studyplatform.member.presentation.dto.response;

import org.certis.studyplatform.member.domain.model.Member;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;



@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponseDto {
    private Long id;
    private String name;
    private String studentNumber;
    private String profileImage;
    private String grade;
    private String role;
    private List<String> skills;
    private String major;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public static MemberResponseDto fromDomain(Member member) {
        return new MemberResponseDto(
                member.getId() != null ? member.getId().value() : null,
                member.getName(),
                member.getStudentNumber().value(),
                member.getProfileImage() != null ? member.getProfileImage().value() : null,
                member.getGrade(),
                member.getRole(),
                member.getSkills().values(),
                member.getMajor(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}