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
import org.certis.studyplatform.shared.security.MockCurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardFacadeService boardFacadeService;
    private final MockCurrentUserProvider mockCurrentUserProvider;

    // 게시글 키워드 검색 조회
    @GetMapping("/keyword")
    public ResponseEntity<GlobalResponseHandler<Page<BoardListResponseDto>>> searchBoards(
            @ModelAttribute BoardSearchRequestDto request) {
        Page<BoardListResponseDto> boardPage = boardFacadeService.searchBoards(request);

        return GlobalResponseHandler.success(ResponseStatus.BOARD_SEARCH_SUCCESS, boardPage);
    }

   // 게시글 id 에 의한 상세 페이지 정보 조회
    @GetMapping("/detail/{id}")
    public ResponseEntity<GlobalResponseHandler<BoardDetailResponseDto>> getBoardDetail(
            @PathVariable Long id
//            , @AuthenticationPrincipal CurrentUser currentUser
    ) {
        CurrentUser currentUser = mockCurrentUserProvider.getMockCurrentUser();

        BoardDetailResponseDto boardDetail = boardFacadeService.getBoardDetail(id, currentUser.getId());

        return GlobalResponseHandler.success(ResponseStatus.BOARD_FIND_SUCCESS, boardDetail);
    }

    // 게시글 생성
    @PostMapping("/create")
    public ResponseEntity<GlobalResponseHandler<Void>> createBoard(
            @Valid @RequestBody BoardCreateRequestDto request
//            , @AuthenticationPrincipal CurrentUser currentUser
    ) {
        CurrentUser currentUser = mockCurrentUserProvider.getMockCurrentUser();

        boardFacadeService.createBoard(request, currentUser.getId());

        return GlobalResponseHandler.success(ResponseStatus.BOARD_CREATE_SUCCESS);
    }

    // 게시글 수정
    @PutMapping("/edit/{id}")
    public ResponseEntity<GlobalResponseHandler<Void>> updateBoard(
            @PathVariable Long id,
            @Valid @RequestBody BoardUpdateRequestDto request
//            , @AuthenticationPrincipal CurrentUser currentUser
    ) {
        CurrentUser currentUser = mockCurrentUserProvider.getMockCurrentUser();

        boardFacadeService.updateBoard(id, request, currentUser.getId());

        return GlobalResponseHandler.success(ResponseStatus.BOARD_UPDATE_SUCCESS);
    }

    // 게시글 삭제
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<GlobalResponseHandler<Void>> deleteBoard(
            @PathVariable Long id
//            , @AuthenticationPrincipal CurrentUser currentUser
    ) {

        CurrentUser currentUser = mockCurrentUserProvider.getMockCurrentUser();

        boardFacadeService.deleteBoard(id, currentUser.getId(),currentUser.getRole());

        return GlobalResponseHandler.success(ResponseStatus.BOARD_DELETE_SUCCESS);
    }

    // 게시글 좋아요 토글
    @PostMapping("/like/{id}")
    public ResponseEntity<GlobalResponseHandler<BoardLikeResponseDto>> toggleLike(
            @PathVariable Long id
//            , @AuthenticationPrincipal CurrentUser currentUser
    ) {
        CurrentUser currentUser = mockCurrentUserProvider.getMockCurrentUser();

        BoardLikeResponseDto likeResponse = boardFacadeService.toggleLike(id, currentUser.getId());

        return GlobalResponseHandler.success(ResponseStatus.BOARD_LIKE_SUCCESS, likeResponse);
    }
}
