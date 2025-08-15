package org.certis.studyplatform.board.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardListResponseDto {

    private Long boardId;
    private String title;
    private String description;
    private OffsetDateTime updatedAt;
    private String category;
    private String authorName;    // member.name에서 가져옴
    private Long likeCount;       // Redis에서 조회
    private Long viewCount;       // Redis에서 조회
}
