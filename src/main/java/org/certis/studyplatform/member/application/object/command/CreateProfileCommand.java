package org.certis.studyplatform.member.application.object.command;

public record CreateProfileCommand(
        Long memberId,
        String name,
        String description,
        String profileImageUrl
) {}