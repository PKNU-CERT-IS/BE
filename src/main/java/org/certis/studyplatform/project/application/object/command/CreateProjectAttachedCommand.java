package org.certis.studyplatform.project.application.object.command;

import org.certis.studyplatform.shared.type.AttachedType;


public record CreateProjectAttachedCommand(
        String name,
        AttachedType type,
        Long size,
        String url
) {

    public static CreateProjectAttachedCommand of(
            String name,
            AttachedType type,
            Long size,
            String url
    ) {
        return new CreateProjectAttachedCommand(
               name, type, size, url
        );
    }
}
