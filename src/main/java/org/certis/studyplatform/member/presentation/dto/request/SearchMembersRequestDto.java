package org.certis.studyplatform.member.presentation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SearchMembersRequestDto {

    private String name;
    private String grade;
    private String role;
    private String major;
    private List<String> skills;
}