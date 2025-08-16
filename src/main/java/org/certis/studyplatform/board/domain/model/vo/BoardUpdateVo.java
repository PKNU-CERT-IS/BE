package org.certis.studyplatform.board.domain.model.vo;

import java.util.List;

/**
 * Board Update Value Object
 * 게시글 수정용 VO
 */
public record BoardUpdateVo(
        Long id,
        String title,
        String content,
        String description,
        String category,
        List<AttachmentVo> attachments
) {

    public static BoardUpdateVo of(Long id, String title, String content, String description,
                                   String category, List<AttachmentVo> attachments) {
        // 개별 VO 생성으로 검증 수행
        BoardIdVo.of(id);
        BoardTitleVo.of(title);
        BoardContentVo.of(content);
        BoardDescriptionVo.of(description);
        BoardCategoryVo.of(category);

        return new BoardUpdateVo(id, title, content, description, category,
                attachments != null ? attachments : List.of());
    }
}