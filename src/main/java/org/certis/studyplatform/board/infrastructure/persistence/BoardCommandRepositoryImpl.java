package org.certis.studyplatform.board.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.board.domain.model.vo.*;
import org.certis.studyplatform.board.domain.repository.*;
import org.certis.studyplatform.board.infrastructure.mapper.BoardInfrastructureMapper;
import org.certis.studyplatform.board.infrastructure.persistence.entity.BoardAttachedEntity;
import org.certis.studyplatform.board.infrastructure.persistence.entity.BoardEntity;
import org.certis.studyplatform.board.infrastructure.persistence.entity.BoardLikeEntity;
import org.certis.studyplatform.board.infrastructure.persistence.entity.BoardViewEntity;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Repository
@RequiredArgsConstructor
@Slf4j
public class BoardCommandRepositoryImpl implements BoardCommandRepository {

    private final BoardJpaRepository boardJpaRepository;
    private final BoardAttachedJpaRepository boardAttachedJpaRepository;
    private final BoardInfrastructureMapper boardInfrastructureMapper;
    private final BoardQueryRepository boardQueryRepository;
    private final BoardLikeJpaRepository boardLikeJpaRepository;
    private final BoardViewJpaRepository boardViewJpaRepository;

    @Override
    @Transactional
    public BoardIdVo createBoard(BoardCreationVo creationVo) {
        log.info("🏗️ Infrastructure: Starting board creation - title: {}, author: {}",
                creationVo.title(), creationVo.authorId());

        try {
            log.debug("🔄 Converting BoardCreationVo to BoardEntity using mapper...");

            BoardEntity boardEntity = boardInfrastructureMapper.toBoardEntity(creationVo);

            BoardEntity savedBoard = boardJpaRepository.save(boardEntity);
            log.debug("💾 Board saved with ID: {}", savedBoard.getId());

            if (!creationVo.attachments().isEmpty()) {
                log.debug("📎 Saving {} attachments...", creationVo.attachments().size());

                List<BoardAttachedEntity> attachmentEntities =
                        boardInfrastructureMapper.toBoardAttachedEntityList(
                                creationVo.attachments(),
                                savedBoard.getId(),
                                creationVo.authorId()
                        );

                boardAttachedJpaRepository.saveAll(attachmentEntities);
                log.debug("💾 {} attachments saved successfully", attachmentEntities.size());
            }

            BoardIdVo result = BoardIdVo.of(savedBoard.getId());
            log.info("✅ Infrastructure: Board created successfully - ID: {}", result.value());
            return result;
        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to create board - title: {}", creationVo.title(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_CREATE_FAILED);
        }
    }

    @Override
    @Transactional
    public void updateBoard(BoardUpdateVo updateVo) {
            log.info("🔄 Infrastructure: Starting board update - ID: {}", updateVo.boardId());

            try {
                BoardIdVo boardIdVo = BoardIdVo.of(updateVo.boardId());
                BoardVo existingBoard = boardQueryRepository.findById(boardIdVo)
                        .orElseThrow(() -> new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_NOT_FOUND));

                BoardEntity boardEntity = BoardEntity.builder()
                        .id(updateVo.boardId()) // 기존 ID 유지
                        .memberId(existingBoard.authorId()) // 기존 작성자 유지
                        .title(updateVo.title())
                        .content(updateVo.content())
                        .description(updateVo.description())
                        .category(updateVo.category())
                        .createdAt(existingBoard.createdAt()) // 기존 생성일 유지
                        // updatedAt은 @UpdateTimestamp로 자동 설정됨
                        .build();

                boardJpaRepository.save(boardEntity);
                log.debug("💾 Board updated successfully - ID: {}", updateVo.boardId());

                updateAttachments(updateVo.boardId(), updateVo.attachments(), existingBoard.authorId());

                log.info("✅ Infrastructure: Board updated successfully - ID: {}", updateVo.boardId());

            } catch (Exception e) {
                log.error("❌ Infrastructure: Failed to update board - ID: {}", updateVo.boardId(), e);
                throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_UPDATE_FAILED);
            }
    }

    @Override
    @Transactional
    public void deleteBoard(BoardIdVo boardIdVo) {
        log.info("🗑️ Infrastructure: Starting board deletion - ID: {}", boardIdVo.value());

        try {
            BoardVo existingBoard = boardQueryRepository.findById(boardIdVo)
                    .orElseThrow(() -> new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_NOT_FOUND));
            boardJpaRepository.deleteById(boardIdVo.value());

            boardAttachedJpaRepository.softDeleteByBoardId(boardIdVo.value());
            log.debug("🗑️ Attachments deleted for board: {}", boardIdVo.value());
            log.info("✅ Infrastructure: Board deleted successfully - ID: {}", boardIdVo.value());

        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to delete board - ID: {}", boardIdVo.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_DELETE_FAILED,
                    "게시글 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    public void updateBoardStats(BoardIdVo boardIdVo, Long redisLikeCount, Long redisViewCount) {
        log.debug("📊 Infrastructure: Updating board stats - ID: {}, likes: {}, views: {}",
                boardIdVo.value(), redisLikeCount, redisViewCount);

        try {
            BoardVo existingBoard = boardQueryRepository.findById(boardIdVo)
                    .orElseThrow(() -> new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_NOT_FOUND));

            BoardLikeEntity likeEntity = boardLikeJpaRepository.findLatestByBoardId(boardIdVo.value());

            // 존재시 업데이트 존재하지 않을 시 새로운 통계 생성
            if (likeEntity != null) {
                // 기존 통계 업데이트
                BoardLikeEntity updatedLikeEntity = likeEntity.toBuilder()
                        .likeNumber(redisLikeCount.intValue())
                        .build();
                boardLikeJpaRepository.save(updatedLikeEntity);
            } else {
                BoardLikeEntity newLikeEntity = BoardLikeEntity.builder()
                        .boardId(boardIdVo.value())
                        .memberId(existingBoard.authorId())
                        .likeNumber(redisLikeCount.intValue())
                        .build();
                boardLikeJpaRepository.save(newLikeEntity);
            }

            BoardViewEntity viewEntity = boardViewJpaRepository.findLatestByBoardId(boardIdVo.value());

            if (viewEntity != null) {
                // 기존 통계 업데이트
                BoardViewEntity updatedViewEntity = viewEntity.toBuilder()
                        .viewNumber(redisViewCount.intValue())
                        .build();
                boardViewJpaRepository.save(updatedViewEntity);
            } else {
                // 새로운 통계 생성
                BoardViewEntity newViewEntity = BoardViewEntity.builder()
                        .boardId(boardIdVo.value())
                        .viewNumber(redisViewCount.intValue())
                        .build();
                boardViewJpaRepository.save(newViewEntity);
            }

            log.debug("✅ Board stats updated successfully - ID: {}, likes: {}, views: {}",
                    boardIdVo.value(), redisLikeCount, redisViewCount);

        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to update board stats - ID: {}", boardIdVo.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_UPDATE_FAILED)
        }
    }

    private void updateAttachments(Long boardId, List<AttachmentVo> newAttachments, Long memberId) {
        log.debug("📎 Starting attachment differential processing for board: {}", boardId);

        try {
            BoardIdVo boardIdVo = BoardIdVo.of(boardId);
            List<AttachmentVo> existingAttachments = boardQueryRepository.findAttachmentsByBoardId(boardIdVo);

            List<Long> newAttachmentIds = newAttachments.stream()
                    .map(AttachmentVo::id)
                    .filter(id -> id != null)
                    .toList();

            List<Long> attachmentIdsToDelete = existingAttachments.stream()
                    .map(AttachmentVo::id)
                    .filter(id -> !newAttachmentIds.contains(id))
                    .toList();

            if (!attachmentIdsToDelete.isEmpty()) {
                boardAttachedJpaRepository.deleteAllById(attachmentIdsToDelete);
                log.debug("🗑️ Deleted {} existing attachments", attachmentIdsToDelete.size());
            }

            List<AttachmentVo> newAttachmentsToSave = newAttachments.stream()
                    .filter(attachment -> attachment.id() == null) // ID가 없는 것 = 새로 추가된 것
                    .toList();

            if (!newAttachmentsToSave.isEmpty()) {
                List<BoardAttachedEntity> newAttachmentEntities =
                        boardInfrastructureMapper.toBoardAttachedEntityList(
                                newAttachmentsToSave,
                                boardId,
                                memberId
                        );

                boardAttachedJpaRepository.saveAll(newAttachmentEntities);
                log.debug("💾 Saved {} new attachments", newAttachmentEntities.size());
            }

            log.debug("✅ Attachment differential processing completed");

        } catch (Exception e) {
            log.error("❌ Failed to update attachments for board: {}", boardId, e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_UPDATE_FAILED,
                    "첨부파일 업데이트에 실패했습니다: " + e.getMessage());
        }
    }
}
