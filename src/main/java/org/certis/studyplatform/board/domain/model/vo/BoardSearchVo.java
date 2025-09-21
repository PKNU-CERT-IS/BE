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

        // 카테고리 검증 (null이 아닌 경우에만, "ALL"은 특별 처리)
        if (category != null && !category.trim().isEmpty() && !"ALL".equals(category.trim().toUpperCase())) {
            BoardCategoryVo.of(category); // 기존 카테고리 검증 재사용
        }

        // 페이징 검증
        if (page < 0) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_PAGE);
        }
        // 사이즈 검증 (1 이상 1000 이하로 제한)
        if (size < 1) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_PAGE_SIZE);
        }
        if (size > 1000) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_PAGE_SIZE, "페이지 크기는 1000을 초과할 수 없습니다.");
        }

        return new BoardSearchVo(
                search != null ? search.trim() : null,
                category != null ? category.trim() : null,
                page,
                size
        );
    }
}
