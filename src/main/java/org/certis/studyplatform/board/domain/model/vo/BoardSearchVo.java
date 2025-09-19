package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

public record BoardSearchVo(
        String search,
        String category,
        int page,
        int size
) {

    public static BoardSearchVo of(String search, String category, int page, int size) {
        // 검색어 검증
        if (search != null && search.trim().length() > 100) {
            // 검색어가 너무 긴 경우만 제한
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_PAGE);
        }

        // 카테고리 검증 (null이 아닌 경우에만)
        if (category != null && !category.trim().isEmpty()) {
            BoardCategoryVo.of(category); // 기존 카테고리 검증 재사용
        }

        // 페이징 검증
        if (page < 0) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_PAGE);
        }
        // 사이즈 상한을 제거하여 파라미터가 없을 때 전체 조회가 가능하도록 허용
        if (size < 1) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_PAGE_SIZE);
        }

        return new BoardSearchVo(
                search != null ? search.trim() : null,
                category != null ? category.trim() : null,
                page,
                size
        );
    }
}
