package org.certis.studyplatform.board.infrastructure.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.board.domain.model.vo.AttachmentVo;
import org.certis.studyplatform.board.domain.model.vo.BoardCreationVo;
import org.certis.studyplatform.board.domain.model.vo.BoardSummaryVo;
import org.certis.studyplatform.board.domain.model.vo.BoardVo;
import org.certis.studyplatform.board.infrastructure.persistence.entity.BoardAttachedEntity;
import org.certis.studyplatform.board.infrastructure.persistence.entity.BoardEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BoardInfrastructureMapper {

    /**
     * BoardCreationVo → BoardEntity 변환
     */
    public BoardEntity toBoardEntity(BoardCreationVo creationVo) {
        if (creationVo == null) {
            log.warn("🔄 Mapper: BoardCreationVo is null, returning null");
            return null;
        }

        log.debug("🔄 Mapper: Converting BoardCreationVo to BoardEntity - title: {}", creationVo.title());

        return BoardEntity.builder()
                .memberId(creationVo.authorId())
                .title(creationVo.title())
                .content(creationVo.content())
                .description(creationVo.description())
                .category(creationVo.category())
                .build();
    }

    /**
     * BoardEntity → BoardVo 변환
     */
    public BoardVo toBoardVo(BoardEntity entity) {
        if (entity == null) {
            log.warn("🔄 Mapper: BoardEntity is null, returning null");
            return null;
        }

        log.debug("🔄 Mapper: Converting BoardEntity to BoardVo - ID: {}", entity.getId());

        return BoardVo.of(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getMemberId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                List.of() // 기본 변환에서는 첨부파일 없음
        );
    }

    /**
     * BoardEntity + Attachments → BoardVo 변환
     */
    public BoardVo toBoardVoWithAttachments(BoardEntity entity, List<BoardAttachedEntity> attachments) {
        if (entity == null) {
            log.warn("🔄 Mapper: BoardEntity is null, returning null");
            return null;
        }

        log.debug("🔄 Mapper: Converting BoardEntity with {} attachments to BoardVo",
                attachments != null ? attachments.size() : 0);

        List<AttachmentVo> attachmentVos = attachments != null ?
                attachments.stream().map(this::toAttachmentVo).toList() : List.of();

        return BoardVo.of(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getMemberId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                attachmentVos
        );
    }

    /**
     * BoardEntity → BoardSummaryVo 변환
     */
    public BoardSummaryVo toBoardSummaryVo(BoardEntity entity, String authorName) {
        if (entity == null) {
            log.warn("🔄 Mapper: BoardEntity is null, returning null");
            return null;
        }

        log.debug("🔄 Mapper: Converting BoardEntity to BoardSummaryVo - ID: {}", entity.getId());

        return BoardSummaryVo.of(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getMemberId(),
                authorName != null ? authorName : "Unknown",
                entity.getUpdatedAt(),
                0L, // 좋아요 수는 Redis에서 별도 조회
                0L  // 조회수는 Redis에서 별도 조회
        );
    }

    // ================================================================
    // ATTACHMENT VO ↔ ENTITY 변환
    // ================================================================

    /**
     * AttachmentVo → BoardAttachedEntity 변환
     */
    public BoardAttachedEntity toBoardAttachedEntity(AttachmentVo attachmentVo, Long boardId, Long memberId) {
        if (attachmentVo == null) {
            log.warn("🔄 Mapper: AttachmentVo is null, returning null");
            return null;
        }

        log.debug("🔄 Mapper: Converting AttachmentVo to BoardAttachedEntity - name: {}", attachmentVo.name());

        return BoardAttachedEntity.builder()
                .boardId(boardId)
                .memberId(memberId)
                .name(attachmentVo.name())
                .type(attachmentVo.type())
                .size(attachmentVo.size())
                .attachedUrl(attachmentVo.attachedUrl())
                .build();
    }

    /**
     * BoardAttachedEntity → AttachmentVo 변환
     */
    public AttachmentVo toAttachmentVo(BoardAttachedEntity entity) {
        if (entity == null) {
            log.warn("🔄 Mapper: BoardAttachedEntity is null, returning null");
            return null;
        }

        log.debug("🔄 Mapper: Converting BoardAttachedEntity to AttachmentVo - ID: {}", entity.getId());

        return AttachmentVo.of(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getSize(),
                entity.getAttachedUrl()
        );
    }

    // ================================================================
    // COLLECTION 변환 메서드들
    // ================================================================

    /**
     * AttachmentVo List → BoardAttachedEntity List 변환
     */
    public List<BoardAttachedEntity> toBoardAttachedEntityList(List<AttachmentVo> attachmentVos,
                                                               Long boardId, Long memberId) {
        if (attachmentVos == null || attachmentVos.isEmpty()) {
            log.debug("🔄 Mapper: AttachmentVo list is empty, returning empty list");
            return List.of();
        }

        log.debug("🔄 Mapper: Converting {} AttachmentVos to BoardAttachedEntities", attachmentVos.size());

        return attachmentVos.stream()
                .map(vo -> toBoardAttachedEntity(vo, boardId, memberId))
                .toList();
    }

    /**
     * BoardAttachedEntity List → AttachmentVo List 변환
     */
    public List<AttachmentVo> toAttachmentVoList(List<BoardAttachedEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            log.debug("🔄 Mapper: BoardAttachedEntity list is empty, returning empty list");
            return List.of();
        }

        log.debug("🔄 Mapper: Converting {} BoardAttachedEntities to AttachmentVos", entities.size());

        return entities.stream()
                .map(this::toAttachmentVo)
                .toList();
    }

    /**
     * BoardEntity List → BoardSummaryVo List 변환 (작성자명 Map 포함)
     */
    public List<BoardSummaryVo> toBoardSummaryVoList(List<BoardEntity> entities,
                                                     java.util.Map<Long, String> authorNames) {
        if (entities == null || entities.isEmpty()) {
            log.debug("🔄 Mapper: BoardEntity list is empty, returning empty list");
            return List.of();
        }

        log.debug("🔄 Mapper: Converting {} BoardEntities to BoardSummaryVos", entities.size());

        return entities.stream()
                .map(entity -> {
                    String authorName = authorNames != null ?
                            authorNames.getOrDefault(entity.getMemberId(), "Unknown") : "Unknown";
                    return toBoardSummaryVo(entity, authorName);
                })
                .toList();
    }
}
