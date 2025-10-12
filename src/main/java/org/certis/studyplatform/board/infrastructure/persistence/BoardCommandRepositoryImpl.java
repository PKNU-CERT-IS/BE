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
import org.certis.studyplatform.board.infrastructure.persistence.jpa.BoardAttachedJpaRepository;
import org.certis.studyplatform.board.infrastructure.persistence.jpa.BoardJpaRepository;
import org.certis.studyplatform.board.infrastructure.persistence.jpa.BoardLikeJpaRepository;
import org.certis.studyplatform.board.infrastructure.persistence.jpa.BoardViewJpaRepository;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.shared.service.S3FileService;
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
    private final BoardLikeJpaRepository boardLikeJpaRepository;
    private final BoardViewJpaRepository boardViewJpaRepository;

    private final S3FileService s3FileService;

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
    public void updateBoard(BoardUpdateVo updateVo, BoardVo existingBoard) { // 기존 데이터를 파라미터로 받음
            log.info("🔄 Infrastructure: Starting board update - ID: {}", updateVo.boardId());

            try {
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

                updateAttachments(updateVo.boardId(), updateVo.attachments(),
                        existingBoard.authorId(), existingBoard.attachments());

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
            // 1. 첨부파일 S3 삭제
            List<BoardAttachedEntity> attachments = boardAttachedJpaRepository.findByBoardIdAndDeletedAtIsNull(boardIdVo.value());
            if (!attachments.isEmpty()) {
                for (BoardAttachedEntity attachment : attachments) {
                    try {
                        s3FileService.deleteFile(attachment.getAttachedUrl());
                        log.debug("🗑️ S3 file deleted: {}", attachment.getAttachedUrl());
                    } catch (Exception ex) {
                        log.warn("S3 delete failed for attachment url={} (boardId={})", attachment.getAttachedUrl(), boardIdVo.value(), ex);
                    }
                }
            }

            // 2. DB 소프트 삭제
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

    @Override
    @Transactional
    public void updateBoardStats(BoardStatsUpdateVo statsUpdateVo) {
        log.debug("📊 Infrastructure: Updating board stats - ID: {}", statsUpdateVo.boardId());

        try {
            // Like 통계 업데이트 (JPA 쓰기 전용)
            if (statsUpdateVo.isLikeCountChanged()) {
                updateLikeStatsWithJpa(statsUpdateVo);
            }

            // View 통계 업데이트 (JPA 쓰기 전용)
            if (statsUpdateVo.isViewCountChanged()) {
                updateViewStatsWithJpa(statsUpdateVo);
            }

            log.debug("✅ Board stats updated successfully - ID: {}", statsUpdateVo.boardId());

        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to update board stats - ID: {}", statsUpdateVo.boardId(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_UPDATE_FAILED);
        }
    }

    private void updateViewStatsWithJpa(BoardStatsUpdateVo statsUpdateVo) {
        try {
            if (statsUpdateVo.hasExistingViewEntity()) {
                // Update existing entity directly with SQL
                boardViewJpaRepository.updateViewNumber(
                        statsUpdateVo.viewEntityId(), 
                        statsUpdateVo.newViewCount().intValue());
                log.debug("📊 Updated view entity: id={}, viewNumber={}", 
                        statsUpdateVo.viewEntityId(), statsUpdateVo.newViewCount());
            } else {
                // Create new entity
                BoardViewEntity newEntity = BoardViewEntity.builder()
                        .boardId(statsUpdateVo.boardId())
                        .viewNumber(statsUpdateVo.newViewCount().intValue())
                        .build();

                boardViewJpaRepository.save(newEntity);
                log.debug("📊 Created new view entity for board: {}, viewNumber={}", 
                        statsUpdateVo.boardId(), statsUpdateVo.newViewCount());
            }
        } catch (Exception e) {
            log.error("❌ Failed to update view stats: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void updateLikeStatsWithJpa(BoardStatsUpdateVo statsUpdateVo) {
        try {
            if (statsUpdateVo.hasExistingLikeEntity()) {
                // Update existing entity directly with SQL
                boardLikeJpaRepository.updateLikeNumber(
                        statsUpdateVo.likeEntityId(), 
                        statsUpdateVo.newLikeCount().intValue());
                log.debug("📊 Updated like entity: id={}, likeNumber={}", 
                        statsUpdateVo.likeEntityId(), statsUpdateVo.newLikeCount());
            } else {
                // Create new entity
                BoardLikeEntity newEntity = BoardLikeEntity.builder()
                        .boardId(statsUpdateVo.boardId())
                        .memberId(statsUpdateVo.authorId())
                        .likeNumber(statsUpdateVo.newLikeCount().intValue())
                        .build();

                boardLikeJpaRepository.save(newEntity);
                log.debug("📊 Created new like entity for board: {}, likeNumber={}", 
                        statsUpdateVo.boardId(), statsUpdateVo.newLikeCount());
            }
        } catch (Exception e) {
            log.error("❌ Failed to update like stats: {}", e.getMessage(), e);
            throw e;
        }
    }



    private void updateAttachments(Long boardId, List<AttachmentVo> newAttachments,
                                   Long memberId, List<AttachmentVo> existingAttachments) {
        log.debug("📎 Starting attachment processing for board: {}", boardId);

        try {
            // 현재 DB에 저장된 첨부 목록
            List<BoardAttachedEntity> existingEntities = boardAttachedJpaRepository.findByBoardIdAndDeletedAtIsNull(boardId);

            // attachments == null 또는 빈 배열이면 DB만 비웁니다(S3 삭제 없음)
            if (newAttachments == null || newAttachments.isEmpty()) {
                if (!existingEntities.isEmpty()) {
                    boardAttachedJpaRepository.deleteAll(existingEntities);
                    boardAttachedJpaRepository.flush();
                }
                log.debug("🗑️ Cleared all board attachments from DB only (no S3 deletes)");
                log.debug("✅ Attachment processing completed");
                return;
            }

            // 정규화된 URL 기준으로 diff 계산
            java.util.Set<String> desired = new java.util.LinkedHashSet<>();
            for (AttachmentVo vo : newAttachments) {
                if (vo.attachedUrl() != null) desired.add(s3FileService.normalizeUrl(vo.attachedUrl()));
            }
            java.util.Set<String> existing = new java.util.LinkedHashSet<>();
            for (BoardAttachedEntity e : existingEntities) {
                existing.add(s3FileService.normalizeUrl(e.getAttachedUrl()));
            }

            // DB에서 제거할 것(존재하지만 요청에 없음)
            java.util.Set<String> toRemove = new java.util.LinkedHashSet<>(existing);
            toRemove.removeAll(desired);
            if (!toRemove.isEmpty()) {
                List<BoardAttachedEntity> removeEntities = existingEntities.stream()
                        .filter(e -> toRemove.contains(s3FileService.normalizeUrl(e.getAttachedUrl())))
                        .toList();
                if (!removeEntities.isEmpty()) {
                    boardAttachedJpaRepository.deleteAll(removeEntities);
                }
            }

            // DB에 추가할 것(요청엔 있으나 기존엔 없음)
            java.util.Set<String> toAdd = new java.util.LinkedHashSet<>(desired);
            toAdd.removeAll(existing);
            if (!toAdd.isEmpty()) {
                List<AttachmentVo> addVos = new java.util.ArrayList<>();
                for (AttachmentVo vo : newAttachments) {
                    String canon = s3FileService.normalizeUrl(vo.attachedUrl());
                    if (toAdd.contains(canon)) addVos.add(vo);
                }
                if (!addVos.isEmpty()) {
                    List<BoardAttachedEntity> newAttachmentEntities =
                            boardInfrastructureMapper.toBoardAttachedEntityList(
                                    addVos, boardId, memberId);
                    boardAttachedJpaRepository.saveAll(newAttachmentEntities);
                }
            }

            log.debug("✅ Attachment processing completed");

        } catch (Exception e) {
            log.error("❌ Failed to update attachments for board: {}", boardId, e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_UPDATE_FAILED,
                    "첨부파일 업데이트에 실패했습니다: " + e.getMessage());
        }
    }
}
