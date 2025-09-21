package org.certis.studyplatform.board.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.board.application.service.BoardFacadeService;
import org.certis.studyplatform.board.presentation.dto.request.BoardCreateRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardSearchRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardUpdateRequestDto;
import org.certis.studyplatform.board.presentation.dto.response.BoardDetailResponseDto;
import org.certis.studyplatform.board.presentation.dto.response.BoardLikeResponseDto;
import org.certis.studyplatform.board.presentation.dto.response.BoardListResponseDto;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.certis.studyplatform.board.presentation.dto.response.BoardStatsResponseDto;

@RestController
@RequestMapping("/api/v1/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardFacadeService boardFacadeService;

    // 게시글 키워드 검색 조회
    @GetMapping("/search")
    public ResponseEntity<GlobalResponseHandler<Page<BoardListResponseDto>>> searchBoards(
            @ModelAttribute BoardSearchRequestDto request) {
        Page<BoardListResponseDto> boardPage = boardFacadeService.searchBoards(request);

        return GlobalResponseHandler.success(ResponseStatus.BOARD_SEARCH_SUCCESS, boardPage);
    }

   // 게시글 id 에 의한 상세 페이지 정보 조회
    @GetMapping("/detail/{id}")
    public ResponseEntity<GlobalResponseHandler<BoardDetailResponseDto>> getBoardDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        BoardDetailResponseDto boardDetail = boardFacadeService.getBoardDetail(id, currentUser.getId());

        return GlobalResponseHandler.success(ResponseStatus.BOARD_FIND_SUCCESS, boardDetail);
    }

    // 게시글 생성
    @PostMapping("/create")
    @PreAuthorize("hasRole('UPSOLVER') or hasRole('STAFF') or hasRole('VICECHAIRMAN') or hasRole('CHAIRMAN') or hasRole('ADMIN')") // 게시글 생성은 UPSOLVER 이상
    public ResponseEntity<GlobalResponseHandler<Void>> createBoard(
            @Valid @RequestBody BoardCreateRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        boardFacadeService.createBoard(request, currentUser.getId());

        return GlobalResponseHandler.success(ResponseStatus.BOARD_CREATE_SUCCESS);
    }

    // 게시글 수정
    @PutMapping("/edit/{id}")
    @PreAuthorize("hasRole('UPSOLVER') or hasRole('STAFF') or hasRole('VICECHAIRMAN') or hasRole('CHAIRMAN') or hasRole('ADMIN')") // 게시글 수정은 UPSOLVER 이상
    public ResponseEntity<GlobalResponseHandler<Void>> updateBoard(
            @PathVariable Long id,
            @Valid @RequestBody BoardUpdateRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        boardFacadeService.updateBoard(id, request, currentUser.getId());

        return GlobalResponseHandler.success(ResponseStatus.BOARD_UPDATE_SUCCESS);
    }

    // 게시글 삭제
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('STAFF') or hasRole('VICECHAIRMAN') or hasRole('CHAIRMAN') or hasRole('ADMIN')") // 게시글 삭제는 STAFF 이상 부터
    public ResponseEntity<GlobalResponseHandler<Void>> deleteBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {

        boardFacadeService.deleteBoard(id, currentUser.getId(),currentUser.getRole());

        return GlobalResponseHandler.success(ResponseStatus.BOARD_DELETE_SUCCESS);
    }

    // 게시글 좋아요 토글 ( Redis 활용 )
    @PostMapping("/like/{id}")
    public ResponseEntity<GlobalResponseHandler<BoardLikeResponseDto>> toggleLike(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        BoardLikeResponseDto likeResponse = boardFacadeService.toggleLike(id, currentUser.getId());

        if(likeResponse.isLiked()){
            return GlobalResponseHandler.success(ResponseStatus.BOARD_LIKE_SUCCESS, likeResponse);
        }
        else{
            return GlobalResponseHandler.success(ResponseStatus.BOARD_UNLIKE_SUCCESS, likeResponse);
        }
    }

    // 관리자 수동 동기화 트리거
    @PostMapping("/admin/sync")
    public ResponseEntity<GlobalResponseHandler<Void>> manualSync() {
        boardFacadeService.syncBoardStats();
        return GlobalResponseHandler.success(ResponseStatus.BOARD_SYNC_SUCCESS);
    }

    // 오늘 통계 조회
    @GetMapping("/stats/today")
    public ResponseEntity<GlobalResponseHandler<BoardStatsResponseDto>> getTodayStats() {
        BoardStatsResponseDto stats = boardFacadeService.getTodayStats();
        return GlobalResponseHandler.success(ResponseStatus.BOARD_STATS_FIND_SUCCESS, stats);
    }
}
