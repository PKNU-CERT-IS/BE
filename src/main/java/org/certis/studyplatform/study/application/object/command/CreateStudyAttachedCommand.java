package org.certis.studyplatform.study.application.object.command;

import org.certis.studyplatform.shared.type.AttachedType;


public record CreateStudyAttachedCommand(
        String name,
        AttachedType type,
        Long size,
        String url
) {

    public static CreateStudyAttachedCommand of(
            String name,
            AttachedType type,
            Long size,
            String url
    ) {
        return new CreateStudyAttachedCommand(
               name, type, size, url
        );
    }
}
