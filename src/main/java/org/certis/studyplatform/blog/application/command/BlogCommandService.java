package org.certis.studyplatform.blog.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.blog.application.object.command.CreateBlogCommand;
import org.certis.studyplatform.blog.application.object.command.DeleteBlogCommand;
import org.certis.studyplatform.blog.application.object.command.UpdateBlogCommand;
import org.certis.studyplatform.blog.domain.service.BlogDomainService;
import org.certis.studyplatform.blog.domain.vo.BlogVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Blog Command Service
 *
 * Clean Architecture Application Layer
 * 블로그 쓰기 작업 처리 (CQRS Command Side)
 *
 * Command 객체를 Domain Service로 전달하여 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlogCommandService {

    private final BlogDomainService blogDomainService;

    /**
     * 블로그 생성
     */
    @Transactional
    public BlogVo createBlog(CreateBlogCommand command) {
        log.info("Command: Creating blog - {}", command.title());

        // Command 객체를 Domain Service로 전달
        BlogVo createdVo = blogDomainService.createBlog(command);

        log.info("Command: Blog created successfully - ID: {}", createdVo.id());
        return createdVo;
    }

    /**
     * 블로그 수정
     */
    @Transactional
    public BlogVo updateBlog(UpdateBlogCommand command) {
        log.info("Command: Updating blog - ID: {}", command.id());

        // Command 객체를 Domain Service로 전달
        BlogVo updatedVo = blogDomainService.updateBlog(command);

        log.info("Command: Blog updated successfully - ID: {}", updatedVo.id());
        return updatedVo;
    }

    /**
     * 블로그 삭제
     */
    @Transactional
    public void deleteBlog(DeleteBlogCommand command) {
        log.info("Command: Deleting blog - ID: {}", command.id());

        // Command 객체를 Domain Service로 전달
        blogDomainService.deleteBlog(command);

        log.info("Command: Blog deleted successfully - ID: {}", command.id());
    }

    /**
     * Admin용 블로그 공개 유무 토글
     */
    @Transactional
    public void toggleBlogPublicStatus(Long blogId, Boolean isPublic, Long adminId) {
        log.info("Command: Toggling blog public status - ID: {} to {} by admin: {}", blogId, isPublic, adminId);

        // Domain Service로 전달
        blogDomainService.toggleBlogPublicStatus(blogId, isPublic, adminId);

        log.info("Command: Blog public status toggled successfully - ID: {}", blogId);
    }
}