package org.certis.studyplatform.board.domain.model.vo;

/**
 * Board Created Value Object
 * 게시글 생성 결과 VO
 */
public record BoardCreatedVo(
        Long id,
        String title
) {
    public static BoardCreatedVo of(Long id, String title) {
        BoardIdVo.of(id);
        BoardTitleVo.of(title);

        return new BoardCreatedVo(id, title);
    }
}
