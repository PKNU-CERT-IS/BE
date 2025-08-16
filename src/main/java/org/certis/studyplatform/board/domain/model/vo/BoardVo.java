package org.certis.studyplatform.board.domain.model.vo;

import java.time.OffsetDateTime;
import java.util.List;

public record BoardVo(
        Long id,
        String title,
        String content,
        String description,
        String category,
        Long authorId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<AttachmentVo> attachments
) {

    public static BoardVo of(Long id, String title, String content, String description,
                             String category, Long authorId, OffsetDateTime createdAt,
                             OffsetDateTime updatedAt, List<AttachmentVo> attachments) {
        // 개별 VO 생성으로 검증 수행
        BoardIdVo.of(id);
        BoardTitleVo.of(title);
        BoardContentVo.of(content);
        BoardDescriptionVo.of(description);
        BoardCategoryVo.of(category);

        return new BoardVo(id, title, content, description, category, authorId,
                createdAt, updatedAt, attachments != null ? attachments : List.of());
    }

    // 작성자 확인
    public boolean isAuthor(Long memberId) {
        return authorId.equals(memberId);
    }

    // 첨부파일 여부 확인
    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }

    // 첨부파일 개수
    public int getAttachmentCount() {
        return attachments != null ? attachments.size() : 0;
    }
}
