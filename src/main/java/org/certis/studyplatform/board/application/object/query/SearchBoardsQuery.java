package org.certis.studyplatform.board.application.object.query;

public record SearchBoardsQuery(
        String search,     // 검색어 (Optional)
        String category,   // 카테고리 (Optional)
        int page,         // 페이지 번호
        int size          // 페이지 사이즈
) {
    public static SearchBoardsQuery of(String search, String category, int page, int size) {
        return new SearchBoardsQuery(search, category, page, size);
    }

    public boolean hasSearchKeyword() {
        return search != null && !search.trim().isEmpty();
    }

    public boolean hasCategoryFilter() {
        return category != null && !category.trim().isEmpty();
    }
}