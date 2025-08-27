package org.certis.studyplatform.board.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardDetailResponseDto {

    private Long boardId;
    private String title;
    private String content;
    private String description;
    private String category;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private AuthorResponseDto author;                    // 작성자 정보
    private List<AttachmentResponseDto> attachments;     // 첨부파일 목록

    private Long likeCount;                              // 좋아요 수
    private Long viewCount;                              // 조회수
    private boolean isLikedByCurrentUser;                // 현재 사용자 좋아요 여부
}
