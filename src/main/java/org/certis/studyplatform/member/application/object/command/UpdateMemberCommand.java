package org.certis.studyplatform.member.application.object.command;

import java.util.List;

public record UpdateMemberCommand(
        Long id,
        String name,
        String studentNumber,
        String grade,
        String role,
        String major,
        String description,
        List<String> skills
) {}