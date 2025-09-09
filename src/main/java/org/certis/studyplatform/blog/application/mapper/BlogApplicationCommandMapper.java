package org.certis.studyplatform.blog.application.mapper;

import org.certis.studyplatform.blog.application.object.command.CreateBlogCommand;
import org.certis.studyplatform.blog.application.object.command.DeleteBlogCommand;
import org.certis.studyplatform.blog.application.object.command.UpdateBlogCommand;
import org.certis.studyplatform.blog.presentation.dto.request.*;
import org.springframework.stereotype.Component;

/**
 * Blog Application Command Mapper
 *
 * Presentation Layer DTO를 Application Layer Command 객체로 변환
 */
@Component
public class BlogApplicationCommandMapper {

    /**
     * BlogCreateRequestDto를 CreateBlogCommand로 변환
     */
    public CreateBlogCommand toCreateBlogCommand(BlogCreateRequestDto dto, Long creatorId) {
        return CreateBlogCommand.of(
                dto.getTitle(),
                dto.getDescription(),
                dto.getContent(),
                dto.getCategory(),
                dto.getReferenceType(),
                dto.getReferenceId(),
                creatorId
        );
    }

    /**
     * BlogUpdateRequestDto를 UpdateBlogCommand로 변환
     */
    public UpdateBlogCommand toUpdateBlogCommand(BlogUpdateRequestDto dto, Long requesterId) {
        return UpdateBlogCommand.of(
                dto.getBlogId(),
                dto.getTitle(),
                dto.getDescription(),
                dto.getContent(),
                dto.getCategory(),
                dto.getReferenceType(),
                dto.getReferenceId(),
                requesterId
        );
    }

    /**
     * DeleteBlogCommand 생성
     */
    public DeleteBlogCommand toDeleteBlogCommand(Long blogId, Long requesterId) {
        return DeleteBlogCommand.of(blogId, requesterId);
    }
}