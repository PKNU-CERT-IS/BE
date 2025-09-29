package org.certis.studyplatform.board.application.mapper;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.board.application.object.command.AttachmentCommand;
import org.certis.studyplatform.board.application.object.command.CreateBoardCommand;
import org.certis.studyplatform.board.application.object.command.UpdateBoardCommand;
import org.certis.studyplatform.board.application.object.query.SearchBoardsQuery;
import org.certis.studyplatform.board.domain.model.vo.*;
import org.certis.studyplatform.board.presentation.dto.request.AttachmentRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardCreateRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardSearchRequestDto;
import org.certis.studyplatform.board.presentation.dto.request.BoardUpdateRequestDto;
import org.certis.studyplatform.board.presentation.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import org.certis.studyplatform.shared.service.S3FileService;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BoardApplicationMapper {

    private final S3FileService s3FileService;

    /**
     * BoardSearchRequestDto → SearchBoardsQuery 변환
     */
    public SearchBoardsQuery toSearchBoardsQuery(BoardSearchRequestDto request) {
        // 전달된 page/size를 그대로 사용 (검증은 Domain Vo에서 수행)
        int page = request.getPage();
        int size = request.getSize();

        return SearchBoardsQuery.of(
                request.getKeyword(),
                request.getCategory(),
                page,
                size
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
                .category(vo.category() != null ? vo.category().value() : null)
                .authorName(vo.authorName())
                .likeCount(vo.likeCount())
                .viewCount(vo.viewCount())
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
                // S3 URL을 presigned URL로 변환하여 프론트엔드에서 직접 접근 가능하도록 함
                .attachedUrl(normalizeUrl(vo.attachedUrl()))
                .build();
    }

    /**
     * URL 정규화 - S3 URL을 presigned URL로 변환
     */
    private String normalizeUrl(String url) {
        return s3FileService.toPresignedUrl(url);
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

    public BoardDetailResponseDto toBoardDetailResponseDto(BoardDetailVo boardDetailVo) {
        return BoardDetailResponseDto.builder()
                .boardId(boardDetailVo.id())
                .title(boardDetailVo.title())
                .content(boardDetailVo.content())
                .description(boardDetailVo.description())
                .category(boardDetailVo.category())
                .createdAt(boardDetailVo.createdAt())
                .updatedAt(boardDetailVo.updatedAt())
                .author(AuthorResponseDto.builder()
                        .memberId(boardDetailVo.authorId())
                        .name(boardDetailVo.authorName())
                        .role(boardDetailVo.authorRole())
                        .build())
                .attachments(toAttachmentResponseDtoList(boardDetailVo.attachments()))
                .likeCount(boardDetailVo.likeCount())
                .viewCount(boardDetailVo.viewCount())
                .isLikedByCurrentUser(boardDetailVo.isLikedByCurrentUser())
                .build();
    }
}
