package org.certis.studyplatform.member.presentation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.certis.studyplatform.member.domain.MemberRole;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SearchMembersRequestDto {

    private String name;
    private String grade;
    private MemberRole role;
    private String major;
    private List<String> skills;
}