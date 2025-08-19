package org.certis.studyplatform.board.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardLikeResponseDto {

    private boolean isLiked;      // 토글 후 좋아요 상태
    private Long likeCount;       // 총 좋아요 수
}

