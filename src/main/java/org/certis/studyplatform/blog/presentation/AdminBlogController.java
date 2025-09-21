package org.certis.studyplatform.blog.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.blog.application.BlogFacadeService;
import org.certis.studyplatform.blog.presentation.dto.request.BlogTogglePublicRequestDto;
import org.certis.studyplatform.blog.presentation.dto.response.BlogSummaryResponseDto;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.certis.studyplatform.response.ResponseStatus;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Admin Blog REST Controller
 *
 * Clean Architecture Presentation Layer
 * Admin용 블로그 관련 엔드포인트 제공
 *
 * API 명세:
 * - PUT /api/v1/admin/blog/public - 블로그 공개 유무 토글
 */
@RestController
@RequestMapping("/api/v1/admin/blog")
@RequiredArgsConstructor
@Slf4j
public class AdminBlogController {

    private final BlogFacadeService blogFacadeService;

    /**
     * Admin용 블로그 공개 유무 토글 API
     * PUT /api/v1/admin/blog/public
     */
    @PutMapping("/public")
    public ResponseEntity<GlobalResponseHandler<Void>> toggleBlogPublicStatus(
            @Valid @RequestBody BlogTogglePublicRequestDto request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        log.info("REST: Toggling blog public status - ID: {} by admin: {}", request.getBlogId(), currentUser.getId());

        // Facade Service 호출
        blogFacadeService.toggleBlogPublicStatus(request, currentUser.getId());

        log.info("REST: Blog public status toggled successfully - ID: {}", request.getBlogId());

        return GlobalResponseHandler.success(ResponseStatus.BLOG_UPDATE_SUCCESS);
    }

}
