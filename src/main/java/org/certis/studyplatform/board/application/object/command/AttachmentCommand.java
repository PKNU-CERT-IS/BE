package org.certis.studyplatform.board.application.object.command;

public record AttachmentCommand(
        Long id,  // 기존 파일 ID (수정 시에만 존재, 새 파일은 null)
        String name,
        String type,
        String size,
        String attachedUrl
) {
    // 기존에 존재하던 첨부파일은 db에 id 값을 가지고 있음
    public static AttachmentCommand of(Long id, String name, String type, String size, String attachedUrl) {
        return new AttachmentCommand(id, name, type, size, attachedUrl);
    }

    // 새롭게 추가하게된 첨부파일은 db에 id 값이 없음 ( db에서 시퀀스를 받아야함 )
    public static AttachmentCommand ofNew(String name, String type, String size, String attachedUrl) {
        return new AttachmentCommand(null, name, type, size, attachedUrl);
    }

    public boolean isNew() {
        return id == null;
    }

    public boolean isExisting() {
        return id != null;
    }
}
