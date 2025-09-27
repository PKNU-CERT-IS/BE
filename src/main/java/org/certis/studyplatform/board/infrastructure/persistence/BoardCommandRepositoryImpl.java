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
        if (statsUpdateVo.hasExistingViewEntity()) {
            // 기존 엔티티 업데이트 (ID만으로 바로 업데이트)
            BoardViewEntity updatedEntity = BoardViewEntity.builder()
                    .id(statsUpdateVo.viewEntityId())
                    .boardId(statsUpdateVo.boardId())
                    .viewNumber(statsUpdateVo.newViewCount().intValue())
                    .build();

            boardViewJpaRepository.save(updatedEntity);
            log.debug("📊 Updated view entity: id={}", statsUpdateVo.viewEntityId());
        } else {
            // 새 엔티티 생성
            BoardViewEntity newEntity = BoardViewEntity.builder()
                    .boardId(statsUpdateVo.boardId())
                    .viewNumber(statsUpdateVo.newViewCount().intValue())
                    .build();

            boardViewJpaRepository.save(newEntity);
            log.debug("📊 Created new view entity for board: {}", statsUpdateVo.boardId());
        }
    }

    private void updateLikeStatsWithJpa(BoardStatsUpdateVo statsUpdateVo) {
        if (statsUpdateVo.hasExistingLikeEntity()) {
            // 기존 엔티티 업데이트 (ID만으로 바로 업데이트)
            BoardLikeEntity updatedEntity = BoardLikeEntity.builder()
                    .id(statsUpdateVo.likeEntityId())  // 기존 ID 설정
                    .boardId(statsUpdateVo.boardId())
                    .memberId(statsUpdateVo.authorId())
                    .likeNumber(statsUpdateVo.newLikeCount().intValue())
                    .build();

            boardLikeJpaRepository.save(updatedEntity);  // JPA가 ID 있으면 UPDATE 실행
            log.debug("📊 Updated like entity: id={}", statsUpdateVo.likeEntityId());
        } else {
            // 새 엔티티 생성
            BoardLikeEntity newEntity = BoardLikeEntity.builder()
                    .boardId(statsUpdateVo.boardId())
                    .memberId(statsUpdateVo.authorId())
                    .likeNumber(statsUpdateVo.newLikeCount().intValue())
                    .build();

            boardLikeJpaRepository.save(newEntity);  // JPA가 ID 없으면 INSERT 실행
            log.debug("📊 Created new like entity for board: {}", statsUpdateVo.boardId());
        }
    }



    private void updateAttachments(Long boardId, List<AttachmentVo> newAttachments,
                                   Long memberId, List<AttachmentVo> existingAttachments) {
        log.debug("📎 Starting attachment processing for board: {}", boardId);

        try {
            // 기존 첨부파일 전체 삭제 (소프트 딜리트) + S3 원본 삭제
            List<BoardAttachedEntity> existingEntities = boardAttachedJpaRepository.findByBoardIdAndDeletedAtIsNull(boardId);

            if (!existingEntities.isEmpty()) {
                for (BoardAttachedEntity entity : existingEntities) {
                    try {
                        s3FileService.deleteFile(entity.getAttachedUrl());
                    } catch (Exception ex) {
                        log.warn("S3 delete failed for attachment url={} (boardId={})", entity.getAttachedUrl(), boardId, ex);
                    }
                }
                boardAttachedJpaRepository.deleteAll(existingEntities);
                boardAttachedJpaRepository.flush();
                log.debug("🗑️ Deleted {} existing attachments", existingEntities.size());
            }

            // 새로운 첨부파일 전체 저장
            if (!newAttachments.isEmpty()) {
                List<BoardAttachedEntity> newAttachmentEntities =
                        boardInfrastructureMapper.toBoardAttachedEntityList(
                                newAttachments,
                                boardId,
                                memberId
                        );

                boardAttachedJpaRepository.saveAll(newAttachmentEntities);
                log.debug("💾 Saved {} new attachments", newAttachmentEntities.size());
            }

            log.debug("✅ Attachment processing completed");

        } catch (Exception e) {
            log.error("❌ Failed to update attachments for board: {}", boardId, e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_UPDATE_FAILED,
                    "첨부파일 업데이트에 실패했습니다: " + e.getMessage());
        }
    }
}
