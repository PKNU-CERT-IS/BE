package org.certis.studyplatform.board.presentation.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BoardSearchRequestDto {

    private String keyword;    // 검색어 (Optional)
    private String category;   // 카테고리 (Optional)

    @Min(0)
    private int page = 0;      // 페이지 번호 (기본값 0)

    @Min(1)
    private int size = 10;     // 페이지 사이즈 (기본값 10)
}
