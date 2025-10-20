package org.certis.studyplatform.board.domain.model.vo;

public record BoardCategoryVo(String value) {

    public static BoardCategoryVo of(String value) {

        String upper = value.trim().toUpperCase();
        // 허용된 카테고리만 통과
        switch (upper) {
            case "NOTICE":
            case "ACTIVITY":
            case "SECURITY":
            case "TECH":
            case "QUESTION":
            case "PROJECT":
            case "ALL":  // Special case for fetching all posts
                return new BoardCategoryVo(upper);
            default:
                // 유효하지 않은 카테고리는 null로 처리 (예외 대신)
                return BoardCategoryVo.of("ALL");
        }
    }
}
