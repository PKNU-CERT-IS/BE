package org.certis.studyplatform.board.presentation;

import org.certis.studyplatform.board.application.mapper.BoardApplicationMapper;
import org.certis.studyplatform.board.application.object.query.SearchBoardsQuery;
import org.certis.studyplatform.board.presentation.dto.request.BoardSearchRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Board Search 기능 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Board Search 테스트")
class BoardSearchTest {

    private BoardApplicationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new BoardApplicationMapper();
    }

    @Test
    @DisplayName("keyword와 category가 모두 없을 때 전체 조회가 가능해야 함")
    void searchBoards_WithNoFilters_ShouldReturnAllBoards() {
        // Given
        BoardSearchRequestDto request = new BoardSearchRequestDto();
        request.setKeyword(null);
        request.setCategory(null);
        request.setPage(0);
        request.setSize(10);

        // When
        SearchBoardsQuery query = mapper.toSearchBoardsQuery(request);

        // Then
        assertThat(query.search()).isNull();
        assertThat(query.category()).isNull();
        assertThat(query.page()).isEqualTo(0);
        assertThat(query.size()).isEqualTo(1000); // noFilter일 때 1000으로 설정
    }

    @Test
    @DisplayName("keyword가 비어있을 때 전체 조회가 가능해야 함")
    void searchBoards_WithEmptyKeyword_ShouldReturnAllBoards() {
        // Given
        BoardSearchRequestDto request = new BoardSearchRequestDto();
        request.setKeyword("");
        request.setCategory(null);
        request.setPage(0);
        request.setSize(10);

        // When
        SearchBoardsQuery query = mapper.toSearchBoardsQuery(request);

        // Then
        assertThat(query.search()).isEqualTo("");
        assertThat(query.category()).isNull();
        assertThat(query.page()).isEqualTo(0);
        assertThat(query.size()).isEqualTo(1000); // noFilter일 때 1000으로 설정
    }

    @Test
    @DisplayName("keyword가 있을 때 정상적으로 검색해야 함")
    void searchBoards_WithKeyword_ShouldSearchNormally() {
        // Given
        BoardSearchRequestDto request = new BoardSearchRequestDto();
        request.setKeyword("테스트");
        request.setCategory("TECH");
        request.setPage(1);
        request.setSize(20);

        // When
        SearchBoardsQuery query = mapper.toSearchBoardsQuery(request);

        // Then
        assertThat(query.search()).isEqualTo("테스트");
        assertThat(query.category()).isEqualTo("TECH");
        assertThat(query.page()).isEqualTo(1);
        assertThat(query.size()).isEqualTo(20);
    }

    @Test
    @DisplayName("category만 있을 때 정상적으로 검색해야 함")
    void searchBoards_WithCategoryOnly_ShouldSearchNormally() {
        // Given
        BoardSearchRequestDto request = new BoardSearchRequestDto();
        request.setKeyword(null);
        request.setCategory("NOTICE");
        request.setPage(0);
        request.setSize(15);

        // When
        SearchBoardsQuery query = mapper.toSearchBoardsQuery(request);

        // Then
        assertThat(query.search()).isNull();
        assertThat(query.category()).isEqualTo("NOTICE");
        assertThat(query.page()).isEqualTo(0);
        assertThat(query.size()).isEqualTo(15);
    }
}
