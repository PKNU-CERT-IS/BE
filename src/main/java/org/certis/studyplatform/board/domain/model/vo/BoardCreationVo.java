package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

import java.util.List;

/**
 * Board Creation Value Object
 * 게시글 생성용 VO
 */
public record BoardCreationVo(
        String title,
        String content,
        String description,
        String category,
        Long authorId,
        List<AttachmentVo> attachments
) {

    public static BoardCreationVo of(String title, String content, String description,
                                     String category, Long authorId, List<AttachmentVo> attachments) {
        // 개별 VO 생성으로 검증 수행
        BoardTitleVo.of(title);
        BoardContentVo.of(content);
        BoardDescriptionVo.of(description);
        BoardCategoryVo.of(category);

        if (authorId == null || authorId <= 0) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_ID);
        }

        return new BoardCreationVo(title, content, description, category, authorId,
                attachments != null ? attachments : List.of());
    }
}
