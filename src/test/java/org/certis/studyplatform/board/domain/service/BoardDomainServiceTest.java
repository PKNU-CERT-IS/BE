package org.certis.studyplatform.board.domain.service;

import org.certis.studyplatform.board.application.object.command.*;
import org.certis.studyplatform.board.domain.model.vo.*;
import org.certis.studyplatform.board.domain.repository.BoardCommandRepository;
import org.certis.studyplatform.board.domain.repository.BoardQueryRepository;
import org.certis.studyplatform.board.domain.repository.BoardRedisRepository;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("Board Domain Service 단위 테스트")
public class BoardDomainServiceTest {
    @Mock
    private BoardCommandRepository boardCommandRepository;

    @Mock
    private BoardQueryRepository boardQueryRepository;

    @Mock
    private BoardRedisRepository boardRedisRepository;

    private BoardDomainService boardDomainService;

    @BeforeEach
    void setUp() {
        boardDomainService = new BoardDomainService(
                boardCommandRepository,
                boardQueryRepository,
                boardRedisRepository
        );
    }

    @Nested
    @DisplayName("게시글 생성 테스트")
    class CreateBoardTest {

        @Test
        @DisplayName("유효한 명령으로 게시글 생성 성공")
        void createBoard_WithValidCommand_Success() {
            // Given
            CreateBoardCommand command = CreateBoardCommand.of(
                    "테스트 게시글 제목",     // title
                    "게시글 내용입니다",      // content
                    "게시글 설명입니다",      // description
                    "STUDY",               // category
                    1L,
                    List.of()              // attachments
            );

            BoardIdVo expectedBoardId = BoardIdVo.of(100L);
            given(boardCommandRepository.createBoard(any(BoardCreationVo.class)))
                    .willReturn(expectedBoardId);

            // When & Then
            assertThatNoException().isThrownBy(() ->
                    boardDomainService.createBoard(command)
            );

            // Verify
            then(boardCommandRepository).should(times(1)).createBoard(any(BoardCreationVo.class));
            then(boardRedisRepository).should(times(1)).initializeStats(expectedBoardId);
        }

        @Test
        @DisplayName("새 첨부파일로 게시글 생성 성공")
        void createBoard_WithNewAttachments_Success() {
            // Given
            List<AttachmentCommand> attachments = List.of(
                    AttachmentCommand.ofNew("새파일1.pdf", "application/pdf", "1MB", "http://example.com/new1.pdf"),
                    AttachmentCommand.ofNew("새파일2.jpg", "image/jpeg", "500KB", "http://example.com/new2.jpg")
            );

            CreateBoardCommand command = CreateBoardCommand.of(
                    "제목", "내용", "설명", "STUDY", 1L, attachments
            );

            BoardIdVo expectedBoardId = BoardIdVo.of(100L);
            given(boardCommandRepository.createBoard(any())).willReturn(expectedBoardId);

            // When
            boardDomainService.createBoard(command);

            // Then
            then(boardCommandRepository).should().createBoard(argThat(creationVo -> {
                assertThat(creationVo.attachments()).hasSize(2);
                // AttachmentCommand -> AttachmentVo 변환 시 새 파일은 null ID를 가짐
                assertThat(creationVo.attachments().get(0).id()).isNull();
                assertThat(creationVo.attachments().get(0).name()).isEqualTo("새파일1.pdf");
                assertThat(creationVo.attachments().get(1).id()).isNull();
                assertThat(creationVo.attachments().get(1).name()).isEqualTo("새파일2.jpg");
                return true;
            }));
            then(boardRedisRepository).should().initializeStats(expectedBoardId);
        }


        @Nested
        @DisplayName("게시글 수정 테스트")
        class UpdateBoardTest {

            @Test
            @DisplayName("작성자가 수정 시 성공")
            void updateBoard_ByAuthor_Success() {
                // Given
                Long boardId = 1L;
                Long authorId = 100L;

                var command = UpdateBoardCommand.of(
                        boardId,  "수정된 제목", "수정된 내용",
                        "수정된 설명", "PROJECT", authorId,List.of()
                );

                BoardVo existingBoard = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existingBoard));

                // When
                boardDomainService.updateBoard(command);

                // Then
                then(boardCommandRepository).should().updateBoard(any(BoardUpdateVo.class), eq(existingBoard));
            }

            @Test
            @DisplayName("작성자가 아닌 사용자가 수정 시 예외")
            void updateBoard_ByNonAuthor_ThrowsException() {
                // Given
                Long boardId = 1L;
                Long authorId = 100L;
                Long otherUserId = 200L;

                var command = UpdateBoardCommand.of(
                        boardId,  "수정된 제목", "수정된 내용",
                        "수정된 설명", "PROJECT", otherUserId,List.of()
                );

                BoardVo existingBoard = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existingBoard));

                // When & Then
                assertThatThrownBy(() -> boardDomainService.updateBoard(command))
                        .isInstanceOf(DomainException.class)
                        .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_AUTHOR_MISMATCH);
            }
        }

        @Nested
        @DisplayName("게시글 삭제 테스트")
        class DeleteBoardTest {

            @Test
            @DisplayName("작성자가 삭제 시 성공")
            void deleteBoard_ByAuthor_Success() {
                // Given
                Long boardId = 1L;
                Long authorId = 100L;
                var command = DeleteBoardCommand.of(boardId, authorId, "PLAYER");

                BoardVo existingBoard = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existingBoard));

                // When
                boardDomainService.deleteBoard(command);

                // Then
                then(boardCommandRepository).should().deleteBoard(any(BoardIdVo.class));
                then(boardRedisRepository).should().deleteStats(any(BoardIdVo.class));
            }

            @Test
            @DisplayName("STAFF 권한으로 타인 게시글 삭제 성공")
            void deleteBoard_ByStaff_Success() {
                // Given
                Long boardId = 1L;
                Long authorId = 100L;
                Long staffId = 200L;
                var command = DeleteBoardCommand.of(boardId, staffId, "STAFF");

                BoardVo existingBoard = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existingBoard));

                // When
                boardDomainService.deleteBoard(command);

                // Then
                then(boardCommandRepository).should().deleteBoard(any(BoardIdVo.class));
                then(boardRedisRepository).should().deleteStats(any(BoardIdVo.class));
            }

            @Test
            @DisplayName("권한 없는 사용자가 삭제 시 예외")
            void deleteBoard_ByUnauthorized_ThrowsException() {
                // Given
                Long boardId = 1L;
                Long authorId = 100L;
                Long otherUserId = 200L;
                var command = DeleteBoardCommand.of(boardId, otherUserId, "PLAYER");

                BoardVo existingBoard = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existingBoard));

                // When & Then
                assertThatThrownBy(() -> boardDomainService.deleteBoard(command))
                        .isInstanceOf(DomainException.class)
                        .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_DELETE_PERMISSION_DENIED);
            }
        }

        @Nested
        @DisplayName("좋아요 토글 테스트")
        class ToggleLikeTest {

            @Test
            @DisplayName("타인 게시글 좋아요 토글 성공")
            void toggleLike_OnOthersBoard_Success() {
                // Given
                Long boardId = 1L;
                Long authorId = 100L;
                Long likerId = 200L;
                var command = ToggleLikeCommand.of(boardId, likerId);

                BoardVo existingBoard = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existingBoard));
                given(boardRedisRepository.isLikedByMember(any(), any())).willReturn(false); // 아직 안 눌렀음
                given(boardRedisRepository.getLikeCount(any())).willReturn(5L);

                // When
                BoardLikeVo result = boardDomainService.toggleLike(command);

                // Then
                assertAll(
                        () -> assertThat(result.boardId()).isEqualTo(boardId),
                        () -> assertThat(result.memberId()).isEqualTo(likerId),
                        () -> assertThat(result.isLiked()).isTrue(),
                        () -> assertThat(result.likeCount()).isEqualTo(5L)
                );
            }

            @Test
            @DisplayName("본인 게시글 좋아요 시도 시 예외")
            void toggleLike_OnOwnBoard_ThrowsException() {
                // Given
                Long boardId = 1L;
                Long authorId = 100L;
                var command = ToggleLikeCommand.of(boardId, authorId);

                BoardVo existingBoard = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existingBoard));

                // When & Then
                assertThatThrownBy(() -> boardDomainService.toggleLike(command))
                        .isInstanceOf(DomainException.class)
                        .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_LIKE_SELF_NOT_ALLOWED);
            }
        }

    }

    private BoardVo createMockBoard(Long boardId, Long authorId) {
        return BoardVo.of(
                boardId,
                "제목",
                "내용",
                "설명",
                "STUDY",
                authorId,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                List.of()
        );
    }


}
