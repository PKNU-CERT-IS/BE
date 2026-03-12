package org.certis.studyplatform.board.domain.service;

import org.certis.studyplatform.board.application.object.command.*;
import org.certis.studyplatform.board.application.object.query.GetBoardDetailQuery;
import org.certis.studyplatform.board.domain.model.vo.*;
import org.certis.studyplatform.board.domain.repository.BoardCommandRepository;
import org.certis.studyplatform.board.domain.repository.BoardQueryRepository;
import org.certis.studyplatform.board.domain.repository.BoardRedisRepository;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.MemberRole;
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
    class BoardDomainServiceTest {

        @Mock private BoardCommandRepository boardCommandRepository;
        @Mock private BoardQueryRepository boardQueryRepository;
        @Mock private BoardRedisRepository boardRedisRepository;

        private BoardDomainService boardDomainService;

        @BeforeEach
        void setUp() {
            boardDomainService = new BoardDomainService(
                    boardCommandRepository,
                    boardQueryRepository,
                    boardRedisRepository
            );
        }

        // =========================
        @Nested
        @DisplayName("게시글 생성 테스트")
        class CreateBoardTest {

            @Test
            @DisplayName("유효한 명령으로 게시글 생성 성공")
            void createBoard_WithValidCommand_Success() {
                CreateBoardCommand command = CreateBoardCommand.of(
                        "제목", "내용", "설명", "TECH", 1L, List.of()
                );
                BoardIdVo expectedBoardId = BoardIdVo.of(100L);
                given(boardCommandRepository.createBoard(any())).willReturn(expectedBoardId);

                assertThatNoException().isThrownBy(() -> boardDomainService.createBoard(command));

                then(boardCommandRepository).should(times(1)).createBoard(any());
                then(boardRedisRepository).should(times(1)).initializeStats(expectedBoardId);
            }

            @Test
            @DisplayName("새 첨부파일로 게시글 생성 성공")
            void createBoard_WithNewAttachments_Success() {
                List<AttachmentCommand> attachments = List.of(
                        AttachmentCommand.ofNew("새파일1.pdf", "application/pdf", "1MB", "http://example.com/new1.pdf"),
                        AttachmentCommand.ofNew("새파일2.jpg", "image/jpeg", "500KB", "http://example.com/new2.jpg")
                );
                CreateBoardCommand command = CreateBoardCommand.of("제목", "내용", "설명", "TECH", 1L, attachments);
                BoardIdVo expectedBoardId = BoardIdVo.of(100L);
                given(boardCommandRepository.createBoard(any())).willReturn(expectedBoardId);

                boardDomainService.createBoard(command);

                then(boardCommandRepository).should().createBoard(argThat(creationVo -> {
                    assertThat(creationVo.attachments()).hasSize(2);
                    assertThat(creationVo.attachments().get(0).id()).isNull();
                    assertThat(creationVo.attachments().get(1).id()).isNull();
                    return true;
                }));
                then(boardRedisRepository).should().initializeStats(expectedBoardId);
            }

            @Test
            @DisplayName("잘못된 인자(빈 제목/잘못된 카테고리)")
            void createBoard_InvalidArgument_Throws() {
                var cmd = CreateBoardCommand.of("", "내용", "설명", "INVALID", 1L, List.of());
                assertThatThrownBy(() -> boardDomainService.createBoard(cmd))
                        .isInstanceOf(DomainException.class)
                        .extracting("status")
                        .isEqualTo(ExceptionStatus.BOARD_DOMAIN_INVALID_TITLE);
            }
        }

        // =========================
        @Nested
        @DisplayName("게시글 수정 테스트")
        class UpdateBoardTest {

            @Test
            @DisplayName("작성자가 수정 시 성공")
            void updateBoard_ByAuthor_Success() {
                Long boardId = 1L; Long authorId = 100L;
                var command = UpdateBoardCommand.of(boardId,"수정제목","수정내용","수정설명","PROJECT",authorId,List.of());
                BoardVo existingBoard = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existingBoard));

                boardDomainService.updateBoard(command);

                then(boardCommandRepository).should().updateBoard(any(BoardUpdateVo.class), eq(existingBoard));
            }

            @Test
            @DisplayName("작성자가 아닌 사용자가 수정 시 예외")
            void updateBoard_ByNonAuthor_Throws() {
                Long boardId = 1L; Long authorId = 100L; Long otherUserId = 200L;
                var command = UpdateBoardCommand.of(boardId,"수정제목","수정내용","수정설명","PROJECT",otherUserId,List.of());
                BoardVo existingBoard = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existingBoard));

                assertThatThrownBy(() -> boardDomainService.updateBoard(command))
                        .isInstanceOf(DomainException.class)
                        .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_AUTHOR_MISMATCH);
            }

            @Test
            @DisplayName("존재하지 않는 게시글 수정 시 예외")
            void updateBoard_NotFound_Throws() {
                var cmd = UpdateBoardCommand.of(999L, "t","c","d","TECH", 1L, List.of());
                given(boardQueryRepository.findById(any())).willReturn(Optional.empty());

                assertThatThrownBy(() -> boardDomainService.updateBoard(cmd))
                        .isInstanceOf(DomainException.class)
                        .extracting("status")
                        .isEqualTo(ExceptionStatus.BOARD_DOMAIN_INVALID_ID);
            }

            @Test
            @DisplayName("기존+신규 첨부 혼합 수정")
            void updateBoard_MixExistingAndNewAttachments() {
                Long boardId = 1L; Long authorId = 100L;
                List<AttachmentCommand> attachments = List.of(
                        AttachmentCommand.of(10L, "old.pdf", "application/pdf","1MB","http://.../old.pdf"),
                        AttachmentCommand.ofNew("new.jpg", "image/jpeg","500KB","http://.../new.jpg")
                );
                var cmd = UpdateBoardCommand.of(boardId,"수정제목","수정내용","수정설명","TECH",authorId,attachments);
                var existing = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existing));

                boardDomainService.updateBoard(cmd);

                then(boardCommandRepository).should().updateBoard(argThat(updateVo -> {
                    assertThat(updateVo.attachments()).hasSize(2);
                    return true;
                }), eq(existing));
            }
        }

        // =========================
        @Nested
        @DisplayName("게시글 삭제 테스트")
        class DeleteBoardTest {

            @Test
            @DisplayName("작성자가 삭제 성공")
            void deleteBoard_ByAuthor_Success() {
                Long boardId=1L; Long authorId=100L;
                var cmd = DeleteBoardCommand.of(boardId,authorId,"PLAYER");
                var existing = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existing));

                boardDomainService.deleteBoard(cmd);

                then(boardCommandRepository).should().deleteBoard(any(BoardIdVo.class));
                then(boardRedisRepository).should().deleteStats(any(BoardIdVo.class));
            }

            @Test
            @DisplayName("STAFF 권한으로 삭제 성공")
            void deleteBoard_ByStaff_Success() {
                Long boardId=1L; Long authorId=100L; Long staffId=200L;
                var cmd = DeleteBoardCommand.of(boardId,staffId,"STAFF");
                var existing = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existing));

                boardDomainService.deleteBoard(cmd);

                then(boardCommandRepository).should().deleteBoard(any(BoardIdVo.class));
                then(boardRedisRepository).should().deleteStats(any(BoardIdVo.class));
            }

            @Test
            @DisplayName("ADMIN 권한으로 삭제 성공")
            void deleteBoard_ByAdmin_Success() {
                Long boardId=1L; Long authorId=100L; Long adminId=300L;
                var cmd = DeleteBoardCommand.of(boardId,adminId,"ADMIN");
                var existing = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existing));

                boardDomainService.deleteBoard(cmd);

                then(boardCommandRepository).should().deleteBoard(any(BoardIdVo.class));
                then(boardRedisRepository).should().deleteStats(any(BoardIdVo.class));
            }

            @Test
            @DisplayName("권한 없는 사용자 삭제 시 예외")
            void deleteBoard_ByUnauthorized_Throws() {
                Long boardId=1L; Long authorId=100L; Long otherId=200L;
                var cmd = DeleteBoardCommand.of(boardId,otherId,"PLAYER");
                var existing = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existing));

                assertThatThrownBy(() -> boardDomainService.deleteBoard(cmd))
                        .isInstanceOf(DomainException.class)
                        .extracting("status")
                        .isEqualTo(ExceptionStatus.BOARD_DOMAIN_DELETE_PERMISSION_DENIED);
            }

            @Test
            @DisplayName("존재하지 않는 게시글 삭제 시 예외")
            void deleteBoard_NotFound_Throws() {
                var cmd = DeleteBoardCommand.of(999L,1L,"PLAYER");
                given(boardQueryRepository.findById(any())).willReturn(Optional.empty());

                assertThatThrownBy(() -> boardDomainService.deleteBoard(cmd))
                        .isInstanceOf(DomainException.class)
                        .extracting("status")
                        .isEqualTo(ExceptionStatus.BOARD_DOMAIN_INVALID_ID);
            }

        }

        // =========================
        @Nested
        @DisplayName("좋아요 토글 테스트")
        class ToggleLikeTest {

            @Test
            @DisplayName("타인 게시글 좋아요 성공")
            void toggleLike_OnOthersBoard_Success() {
                Long boardId=1L; Long authorId=100L; Long likerId=200L;
                var cmd = ToggleLikeCommand.of(boardId,likerId);
                var existing = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existing));
                given(boardRedisRepository.isLikedByMember(any(),any())).willReturn(false);
                given(boardRedisRepository.getLikeCount(any())).willReturn(5L);

                BoardLikeVo result = boardDomainService.toggleLike(cmd);

                assertAll(
                        () -> assertThat(result.isLiked()).isTrue(),
                        () -> assertThat(result.likeCount()).isEqualTo(5L)
                );
            }

            @Test
            @DisplayName("본인 게시글 좋아요 시 예외")
            void toggleLike_OnOwnBoard_Throws() {
                Long boardId=1L; Long authorId=100L;
                var cmd = ToggleLikeCommand.of(boardId,authorId);
                var existing = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existing));

                assertThatThrownBy(() -> boardDomainService.toggleLike(cmd))
                        .isInstanceOf(DomainException.class)
                        .hasFieldOrPropertyWithValue("status", ExceptionStatus.BOARD_DOMAIN_LIKE_SELF_NOT_ALLOWED);
            }

            @Test
            @DisplayName("이미 좋아요 상태면 UNLIKE 처리")
            void toggleLike_WhenAlreadyLiked_ThenUnlike() {
                Long boardId=1L; Long authorId=100L; Long likerId=200L;
                var cmd = ToggleLikeCommand.of(boardId,likerId);
                var existing = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existing));
                given(boardRedisRepository.isLikedByMember(any(),any())).willReturn(true);
                given(boardRedisRepository.getLikeCount(any())).willReturn(4L);

                var result = boardDomainService.toggleLike(cmd);

                assertAll(
                        () -> assertThat(result.isLiked()).isFalse(),
                        () -> assertThat(result.likeCount()).isEqualTo(4L)
                );
            }

            @Test
            @DisplayName("존재하지 않는 게시글 좋아요 시 예외")
            void toggleLike_NotFound_Throws() {
                var cmd = ToggleLikeCommand.of(999L,1L);
                given(boardQueryRepository.findById(any())).willReturn(Optional.empty());

                assertThatThrownBy(() -> boardDomainService.toggleLike(cmd))
                        .isInstanceOf(DomainException.class)
                        .extracting("status")
                        .isEqualTo(ExceptionStatus.BOARD_DOMAIN_INVALID_ID);
            }
        }

        // =========================
        @Nested
        @DisplayName("게시글 상세 조회 테스트 - 새로운 기능")
        class GetBoardDetailTest {

            @Test
            @DisplayName("게시글 상세 조회 성공 - 작성자 역할 포함")
            void getBoardDetail_Success_WithAuthorRole() {
                // Given
                Long boardId = 1L;
                Long memberId = 100L;
                GetBoardDetailQuery query = new GetBoardDetailQuery(boardId, memberId);
                
                BoardVo boardVo = createMockBoard(boardId, 200L);
                BoardAuthorInfoVo authorInfo = new BoardAuthorInfoVo("작성자", MemberRole.UPSOLVER, null);
                
                given(boardQueryRepository.findByIdWithAttachments(any(BoardIdVo.class))).willReturn(Optional.of(boardVo));
                given(boardQueryRepository.getAuthorInfo(any(BoardIdVo.class))).willReturn(authorInfo);
                given(boardRedisRepository.getLikeCount(any(BoardIdVo.class))).willReturn(5L);
                given(boardRedisRepository.getViewCount(any(BoardIdVo.class))).willReturn(100L);
                given(boardRedisRepository.isLikedByMember(any(BoardIdVo.class), any())).willReturn(false);

                // When
                BoardDetailVo result = boardDomainService.getBoardDetail(query);

                // Then
                assertAll(
                        () -> assertThat(result.authorName()).isEqualTo("작성자"),
                        () -> assertThat(result.authorRole()).isEqualTo(MemberRole.UPSOLVER),
                        () -> assertThat(result.likeCount()).isEqualTo(5L),
                        () -> assertThat(result.viewCount()).isEqualTo(100L),
                        () -> assertThat(result.isLikedByCurrentUser()).isFalse()
                );

                then(boardRedisRepository).should().addView(any(BoardIdVo.class), eq(memberId));
            }

            @Test
            @DisplayName("새로운 카테고리로 게시글 생성 성공")
            void createBoard_WithNewCategories_Success() {
                // Given - 새로운 카테고리들 테스트
                List<String> newCategories = List.of("NOTICE", "ACTIVITY", "SECURITY", "TECH", "QUESTION");
                
                for (String category : newCategories) {
                    CreateBoardCommand command = CreateBoardCommand.of(
                            "제목", "내용", "설명", category, 1L, List.of()
                    );
                    BoardIdVo expectedBoardId = BoardIdVo.of(100L);
                    given(boardCommandRepository.createBoard(any())).willReturn(expectedBoardId);

                    // When & Then
                    assertThatNoException().isThrownBy(() -> boardDomainService.createBoard(command));
                }
            }

            @Test
            @DisplayName("긴 콘텐츠로 게시글 생성 성공 - 100,000자 제한")
            void createBoard_WithLongContent_Success() {
                // Given
                String longContent = "A".repeat(99999); // 99,999자 (제한 내)
                CreateBoardCommand command = CreateBoardCommand.of(
                        "제목", longContent, "설명", "TECH", 1L, List.of()
                );
                BoardIdVo expectedBoardId = BoardIdVo.of(100L);
                given(boardCommandRepository.createBoard(any())).willReturn(expectedBoardId);

                // When & Then
                assertThatNoException().isThrownBy(() -> boardDomainService.createBoard(command));
            }

            @Test
            @DisplayName("동일 사용자의 중복 조회는 증가하지 않음")
            void getBoardDetail_DuplicateView_NoIncrement() {
                // Given
                Long boardId = 1L;
                Long memberId = 100L;
                GetBoardDetailQuery query = new GetBoardDetailQuery(boardId, memberId);

                BoardVo boardVo = createMockBoard(boardId, 200L);
                BoardAuthorInfoVo authorInfo = new BoardAuthorInfoVo("작성자", MemberRole.UPSOLVER, null);

                given(boardQueryRepository.findByIdWithAttachments(any(BoardIdVo.class))).willReturn(Optional.of(boardVo));
                given(boardQueryRepository.getAuthorInfo(any(BoardIdVo.class))).willReturn(authorInfo);
                // 이미 본 사용자라면 addView 호출 없이 통계만 읽음
                given(boardRedisRepository.isViewedByMember(any(BoardIdVo.class), eq(memberId))).willReturn(true);
                given(boardRedisRepository.getLikeCount(any(BoardIdVo.class))).willReturn(5L);
                given(boardRedisRepository.getViewCount(any(BoardIdVo.class))).willReturn(100L);
                given(boardRedisRepository.isLikedByMember(any(BoardIdVo.class), any())).willReturn(false);

                // When
                BoardDetailVo result = boardDomainService.getBoardDetail(query);

                // Then
                assertAll(
                        () -> assertThat(result.viewCount()).isEqualTo(100L),
                        () -> assertThat(result.likeCount()).isEqualTo(5L)
                );
                // addView 호출되지 않음
                then(boardRedisRepository).should(times(0)).addView(any(BoardIdVo.class), any());
                then(boardRedisRepository).should(times(1)).isViewedByMember(any(BoardIdVo.class), eq(memberId));
            }
        }

        @Nested
        @DisplayName("좋아요 선동기화(DB→Redis) 테스트")
        class LikePreSyncTest {

            @Test
            @DisplayName("Redis에 없고 DB에 있으면 pre-sync로 addLike 호출")
            void toggleLike_PreSync_AddsLikeFromDb() {
                Long boardId = 1L; Long authorId = 100L; Long likerId = 200L;
                var cmd = ToggleLikeCommand.of(boardId, likerId);

                var existing = createMockBoard(boardId, authorId);
                given(boardQueryRepository.findById(any())).willReturn(Optional.of(existing));

                // Redis에는 아직 없는 상태
                given(boardRedisRepository.isLikedByMember(any(BoardIdVo.class), eq(likerId))).willReturn(false);
                // DB에는 과거 like 기록 존재
                given(boardQueryRepository.hasMemberLiked(any(BoardIdVo.class), eq(likerId))).willReturn(true);
                // 토글 이후 count 조회
                given(boardRedisRepository.getLikeCount(any(BoardIdVo.class))).willReturn(6L);

                // When
                BoardLikeVo result = boardDomainService.toggleLike(cmd);

                // Then
                assertThat(result.isLiked()).isTrue();
                then(boardRedisRepository).should(times(2)).addLike(any(BoardIdVo.class), eq(likerId)); // pre-sync 1회 + 토글 1회
            }
        }

        // =========================
        private BoardVo createMockBoard(Long boardId, Long authorId) {
            return BoardVo.of(boardId,"제목","내용","설명","TECH",
                    authorId, OffsetDateTime.now(), OffsetDateTime.now(), List.of());
        }
    }
