package org.certis.studyplatform.board.application.mapper;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.board.application.object.command.AttachmentCommand;
import org.certis.studyplatform.board.application.object.command.CreateBoardCommand;
import org.certis.studyplatform.board.application.object.command.UpdateBoardCommand;
import org.certis.studyplatform.board.application.object.query.SearchBoardsQuery;
import org.certis.studyplatform.board.domain.model.vo.AttachmentVo;
import org.certis.studyplatform.board.domain.model.vo.BoardLikeVo;
import org.certis.studyplatform.board.domain.model.vo.BoardSummaryVo;
import org.certis.studyplatform.board.domain.model.vo.BoardVo;
import org.certis.studyplatform.board.presentation.dto.request.AttachmentRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardCreateRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardSearchRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardUpdateRequestDto;
import org.certis.studyplatform.board.presentation.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BoardApplicationMapper {

    /**
     * BoardSearchRequestDto → SearchBoardsQuery 변환
     */
    public SearchBoardsQuery toSearchBoardsQuery(BoardSearchRequestDto request) {
        return SearchBoardsQuery.of(
                request.getSearch(),
                request.getCategory(),
                request.getPage(),
                request.getSize()
        );
    }

    /**
     * BoardCreateRequestDto → CreateBoardCommand 변환
     */
    public CreateBoardCommand toCreateBoardCommand(BoardCreateRequestDto request, Long authorId) {
        List<AttachmentCommand> attachments = request.getAttachments() != null
                ? request.getAttachments().stream()
                .map(this::toAttachmentCommand)
                .collect(Collectors.toList())
                : List.of();

        return CreateBoardCommand.of(
                request.getTitle(),
                request.getContent(),
                request.getDescription(),
                request.getCategory(),
                authorId,
                attachments
        );
    }

    /**
     * BoardUpdateRequestDto → UpdateBoardCommand 변환
     */
    public UpdateBoardCommand toUpdateBoardCommand(Long boardId, BoardUpdateRequestDto request, Long requesterId) {
        List<AttachmentCommand> attachments = request.getAttachments() != null
                ? request.getAttachments().stream()
                .map(this::toAttachmentCommand)
                .collect(Collectors.toList())
                : List.of();

        return UpdateBoardCommand.of(
                boardId,
                request.getTitle(),
                request.getContent(),
                request.getDescription(),
                request.getCategory(),
                requesterId,
                attachments
        );
    }

    /**
     * AttachmentRequestDto → AttachmentCommand 변환
     */
    private AttachmentCommand toAttachmentCommand(AttachmentRequestDto dto) {
        return AttachmentCommand.of(
                dto.getId(),
                dto.getName(),
                dto.getType(),
                dto.getSize(),
                dto.getAttachedUrl()
        );
    }

    // ================================================================
    // VO → RESPONSE DTO 변환
    // ================================================================

    /**
     * Page<BoardSummaryVo> → Page<BoardListResponseDto> 변환
     */
    public Page<BoardListResponseDto> toBoardListResponseDtoPage(Page<BoardSummaryVo> voPage) {
        List<BoardListResponseDto> dtoList = voPage.getContent().stream()
                .map(this::toBoardListResponseDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, voPage.getPageable(), voPage.getTotalElements());
    }

    /**
     * BoardSummaryVo → BoardListResponseDto 변환
     */
    private BoardListResponseDto toBoardListResponseDto(BoardSummaryVo vo) {
        return BoardListResponseDto.builder()
                .boardId(vo.id())
                .title(vo.title())
                .description(vo.description())
                .updatedAt(vo.updatedAt())
                .category(vo.category())
                .authorName(vo.authorName())
                .likeCount(vo.likeCount())
                .viewCount(vo.viewCount())
                .build();
    }

    /**
     * BoardVo → BoardDetailResponseDto 변환
     * Redis에서 좋아요 상태를 별도로 확인해야 함
     */
    public BoardDetailResponseDto toBoardDetailResponseDto(BoardVo boardVo, Long viewerId) {
        // TODO: Redis에서 좋아요 상태 확인 로직 필요
        // 현재는 임시로 false 설정
        boolean isLikedByCurrentUser = false;

        // TODO: Redis에서 통계 정보 조회 로직 필요
        // 현재는 임시로 0 설정
        Long likeCount = 0L;
        Long viewCount = 0L;

        return BoardDetailResponseDto.builder()
                .boardId(boardVo.id())
                .title(boardVo.title())
                .content(boardVo.content())
                .description(boardVo.description())
                .category(boardVo.category())
                .createdAt(boardVo.createdAt())
                .updatedAt(boardVo.updatedAt())
                .author(AuthorResponseDto.builder()
                        .memberId(boardVo.authorId())
                        .name("TODO: 작성자 이름 조회 필요") // TODO: 작성자 정보 조회
                        .build())
                .attachments(toAttachmentResponseDtoList(boardVo.attachments()))
                .likeCount(likeCount)
                .viewCount(viewCount)
                .isLikedByCurrentUser(isLikedByCurrentUser)
                .build();
    }

    /**
     * List<AttachmentVo> → List<AttachmentResponseDto> 변환
     */
    private List<AttachmentResponseDto> toAttachmentResponseDtoList(List<AttachmentVo> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }

        return attachments.stream()
                .map(this::toAttachmentResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * AttachmentVo → AttachmentResponseDto 변환
     */
    private AttachmentResponseDto toAttachmentResponseDto(AttachmentVo vo) {
        return AttachmentResponseDto.builder()
                .id(vo.id())
                .name(vo.name())
                .type(vo.type())
                .size(vo.size())
                .attachedUrl(vo.attachedUrl())
                .build();
    }

    /**
     * BoardLikeVo → BoardLikeResponseDto 변환
     */
    public BoardLikeResponseDto toBoardLikeResponseDto(BoardLikeVo vo) {
        return BoardLikeResponseDto.builder()
                .isLiked(vo.isLiked())
                .likeCount(vo.likeCount())
                .build();
    }
}
