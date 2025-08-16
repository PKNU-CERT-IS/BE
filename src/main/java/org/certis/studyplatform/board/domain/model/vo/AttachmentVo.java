package org.certis.studyplatform.board.domain.model.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

/**
 * Attachment Value Object
 * 첨부파일 VO (기본 자료형 사용)
 */
public record AttachmentVo(
        Long id,
        String name,
        String type,
        String size,
        String attachedUrl
) {

    public static AttachmentVo of(Long id, String name, String type, String size, String attachedUrl) {
        // 필수 필드 검증
        if (name == null || name.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_ATTACHMENT_NAME);
        }
        if (type == null || type.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_ATTACHMENT_TYPE);
        }
        if (size == null || size.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_ATTACHMENT_SIZE);
        }
        if (attachedUrl == null || attachedUrl.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_ATTACHMENT_URL);
        }
        if (!attachedUrl.startsWith("http://") && !attachedUrl.startsWith("https://")) {
            throw new DomainException(ExceptionStatus.BOARD_DOMAIN_INVALID_ATTACHMENT_URL);
        }

        return new AttachmentVo(id, name.trim(), type.trim(), size.trim(), attachedUrl.trim());
    }

    public static AttachmentVo ofNew(String name, String type, String size, String attachedUrl) {
        return of(null, name, type, size, attachedUrl);
    }

    public boolean isNew() {
        return id == null;
    }

    public boolean isExisting() {
        return id != null;
    }
}