package org.certis.studyplatform.board.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.board.application.object.query.GetBoardDetailQuery;
import org.certis.studyplatform.board.application.object.query.SearchBoardsQuery;
import org.certis.studyplatform.board.domain.model.vo.BoardSummaryVo;
import org.certis.studyplatform.board.domain.model.vo.BoardVo;
import org.certis.studyplatform.board.domain.service.BoardDomainService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BoardQueryService {

    private final BoardDomainService boardDomainService;

    /**
     * 게시글 키워드 검색 조회
     *
     * @param query 검색 조건
     * @return 페이징된 게시글 요약 목록
     */
    public Page<BoardSummaryVo> searchBoards(SearchBoardsQuery query) {
        log.info("Query Service: Searching boards - search: {}, category: {}, page: {}, size: {}",
                query.search(), query.category(), query.page(), query.size());

        // Domain Service에 Query 전달 → VO 반환
        Page<BoardSummaryVo> result = boardDomainService.searchBoards(query);

        log.info("Query Service: Found {} boards", result.getTotalElements());
        return result;
    }

    /**
     * 게시글 상세 조회
     *
     * @param query 상세 조회 조건 (조회자 정보 포함)
     * @return 게시글 상세 정보
     */
    public BoardVo getBoardDetail(GetBoardDetailQuery query) {
        log.info("Query Service: Getting board detail - ID: {}, viewerId: {}",
                query.boardId(), query.viewerId());

        // Domain Service에 Query 전달 → VO 반환 (조회수 증가 포함)
        BoardVo result = boardDomainService.getBoardDetail(query);

        log.info("Query Service: Board detail retrieved - ID: {}, title: {}",
                result.id(), result.title());
        return result;
    }
}