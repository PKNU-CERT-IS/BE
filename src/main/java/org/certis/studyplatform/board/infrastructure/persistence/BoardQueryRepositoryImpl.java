package org.certis.studyplatform.board.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.board.domain.model.vo.*;
import org.certis.studyplatform.board.domain.repository.BoardQueryRepository;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.MemberRole;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;
import org.jooq.Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BoardQueryRepositoryImpl implements BoardQueryRepository {

    private final DSLContext dsl;

    @Override
    public Optional<BoardVo> findById(BoardIdVo boardIdVo) {
        log.debug("🔍 Infrastructure: Finding board by ID: {}", boardIdVo.value());

        try {
            Record record = dsl.select(
                            field("b.id").as("id"),
                            field("b.member_id").as("member_id"),
                            field("b.title").as("title"),
                            field("b.content").as("content"),
                            field("b.description").as("description"),
                            field("b.category").as("category"),
                            field("b.created_at").as("created_at"),
                            field("b.updated_at").as("updated_at")
                    )
                    .from(table("board").as("b"))
                    .where(field("b.id").eq(boardIdVo.value()))
                    .and(field("b.deleted_at").isNull())
                    .fetchOne();

            if (record == null) {
                return Optional.empty();
            }

            BoardVo boardVo = recordToBoardVo(record);
            return Optional.of(boardVo);

        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to find board by ID: {}", boardIdVo.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_QUERY_FAILED);
        }
    }

    @Override
    public Page<BoardSummaryVo> searchBoards(BoardSearchVo searchVo) {
        log.info("🔍 Infrastructure: Searching boards - search: '{}', category: '{}'",
                searchVo.search(), searchVo.category());

        try {
            SelectConditionStep<?> baseQuery = dsl.select(
                            field("b.id").as("id"),
                            field("b.title").as("title"),
                            field("b.description").as("description"),
                            field("b.category").as("category"),
                            field("b.member_id").as("author_id"),
                            field("m.name").as("author_name"),
                            field("b.updated_at").as("updated_at")
                    )
                    .from(table("board").as("b"))
                    .leftJoin(table("member").as("m"))
                    .on(field("b.member_id").eq(field("m.id")))
                    .where(field("b.deleted_at").isNull())
                    .and(field("m.deleted_at").isNull());

            if (searchVo.search() != null && !searchVo.search().trim().isEmpty()) { // 비어있는지 판별
                String searchKeyword = "%" + searchVo.search().trim() + "%"; // LIKE 와 같은 설정
                baseQuery = baseQuery.and(
                        field("b.title").likeIgnoreCase(searchKeyword) // 대소문자 구분없음
                                .or(field("b.description").likeIgnoreCase(searchKeyword))
                );
            }

            if (searchVo.category() != null && !searchVo.category().trim().isEmpty() && !"ALL".equals(searchVo.category().trim().toUpperCase())) {
                baseQuery = baseQuery.and(field("b.category").eq(searchVo.category()));
            }

            // Count 쿼리 - 별도로 구성
            SelectConditionStep<?> countQuery = dsl.selectCount()
                    .from(table("board").as("b"))
                    .leftJoin(table("member").as("m"))
                    .on(field("b.member_id").eq(field("m.id")))
                    .where(field("b.deleted_at").isNull())
                    .and(field("m.deleted_at").isNull());

            // 검색 조건을 count 쿼리에 적용
            if (searchVo.search() != null && !searchVo.search().trim().isEmpty()) {
                String searchKeyword = "%" + searchVo.search().trim() + "%";
                countQuery = countQuery.and(
                        field("b.title").likeIgnoreCase(searchKeyword)
                                .or(field("b.description").likeIgnoreCase(searchKeyword))
                );
            }

            if (searchVo.category() != null && !searchVo.category().trim().isEmpty() && !"ALL".equals(searchVo.category().trim().toUpperCase())) {
                countQuery = countQuery.and(field("b.category").eq(searchVo.category()));
            }

            long totalCount = countQuery.fetchOne(0, Long.class);

            // 데이터 조회 쿼리
            var records = baseQuery
                    .orderBy(field("b.updated_at").desc())
                    .limit(searchVo.size())
                    .offset(searchVo.page() * searchVo.size())
                    .fetch();

            List<BoardSummaryVo> boards = records.stream()
                    .map(this::recordToBoardSummaryVo)
                    .toList();

            Pageable pageable = PageRequest.of(searchVo.page(), searchVo.size());
            return new PageImpl<>(boards, pageable, totalCount);

        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to search boards - search: '{}', category: '{}', page: {}, size: {}", 
                    searchVo.search(), searchVo.category(), searchVo.page(), searchVo.size(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_QUERY_FAILED);
        }
    }

    @Override
    public Optional<BoardVo> findByIdWithAttachments(BoardIdVo boardIdVo) {
        Optional<BoardVo> boardOpt = findById(boardIdVo);
        if (boardOpt.isEmpty()) {
            return Optional.empty();
        }

        BoardVo board = boardOpt.get();
        List<AttachmentVo> attachments = findAttachmentsByBoardId(boardIdVo);

        BoardVo boardWithAttachments = BoardVo.of(
                board.id(),
                board.title(),
                board.content(),
                board.description(),
                board.category(),
                board.authorId(),
                board.createdAt(),
                board.updatedAt(),
                attachments
        );


        return Optional.of(boardWithAttachments);
    }

    @Override
    public String getAuthorName(BoardIdVo boardIdVo) {
        try {
            String authorName = dsl.select(field("m.name"))
                    .from(table("board").as("b"))
                    .leftJoin(table("member").as("m"))
                    .on(field("b.member_id").eq(field("m.id")))
                    .where(field("b.id").eq(boardIdVo.value()))
                    .and(field("b.deleted_at").isNull())
                    .and(field("m.deleted_at").isNull())
                    .fetchOne(field("m.name"), String.class);

            return authorName != null ? authorName : "Unknown";
        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to get author name for board: {}", boardIdVo.value(), e);
            return "Unknown";
        }
    }

    @Override
    public BoardAuthorInfoVo getAuthorInfo(BoardIdVo boardIdVo) {
        try {
            Record record = dsl.select(field("m.name"), field("m.role"), field("m.profile_image"))
                    .from(table("board").as("b"))
                    .leftJoin(table("member").as("m"))
                    .on(field("b.member_id").eq(field("m.id")))
                    .where(field("b.id").eq(boardIdVo.value()))
                    .and(field("b.deleted_at").isNull())
                    .and(field("m.deleted_at").isNull())
                    .fetchOne();

            if (record != null) {
                String name = record.get(field("m.name"), String.class);
                String roleString = record.get(field("m.role"), String.class);
                String profileImageUrl = record.get(field("m.profile_image"), String.class);
                MemberRole role = roleString != null ? MemberRole.valueOf(roleString) : MemberRole.NONE;
                
                return new BoardAuthorInfoVo(
                    name != null ? name : "Unknown",
                    role,
                    profileImageUrl
                );
            }
            
            return new BoardAuthorInfoVo("Unknown", MemberRole.NONE, null);
        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to get author info for board: {}", boardIdVo.value(), e);
            return new BoardAuthorInfoVo("Unknown", MemberRole.NONE, null);
        }
    }

    @Override
    public Long getAuthorId(BoardIdVo boardIdVo) {
        try {
            return dsl.select(field("b.member_id", Long.class))
                    .from(table("board").as("b"))
                    .where(field("b.id").eq(boardIdVo.value()))
                    .and(field("b.deleted_at").isNull())
                    .fetchOne(field("b.member_id", Long.class));
        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to get author id for board: {}", boardIdVo.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_QUERY_FAILED);
        }
    }

    @Override
    public List<Long> findAllActiveBoardIds() {
        try {
            return dsl.select(field("b.id"))
                    .from(table("board").as("b"))
                    .where(field("b.deleted_at").isNull())
                    .orderBy(field("b.id").asc())
                    .fetch(field("b.id"), Long.class);
        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to find active board IDs", e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_QUERY_FAILED,
                    "활성 게시글 ID 조회에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public Long getLikeCountFromDB(BoardIdVo boardIdVo) {
        try {
            Long likeCount = dsl.select(sum(field("bl.like_number", Integer.class)))
                    .from(table("board_like").as("bl"))
                    .where(field("bl.board_id").eq(boardIdVo.value()))
                    .fetchOne(0, Long.class);

            return likeCount != null ? likeCount : 0L;
        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to get like count from DB - board: {}", boardIdVo.value(), e);
            return 0L;
        }
    }

    @Override
    public Map<Long, Long> getLikeCountsFromDB(List<BoardIdVo> boardIds) {
        if (boardIds.isEmpty()) {
            return Map.of();
        }
        try {
            List<Long> ids = boardIds.stream().map(BoardIdVo::value).toList();
            Map<Long, Long> counts = new java.util.LinkedHashMap<>();
            Field<Long> boardIdField = field("bl.board_id", Long.class).as("board_id");
            Field<Long> countField = sum(field("bl.like_number", Integer.class)).cast(Long.class).as("count");
            dsl.select(boardIdField, countField)
                    .from(table("board_like").as("bl"))
                    .where(field("bl.board_id").in(ids))
                    .groupBy(field("bl.board_id"))
                    .fetch()
                    .forEach(record -> counts.put(
                            record.get(boardIdField),
                            record.get(countField) != null ? record.get(countField) : 0L
                    ));
            return counts;
        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to get like counts from DB for boards: {}", boardIds.size(), e);
            return Map.of();
        }
    }

    @Override
    public Long getViewCountFromDB(BoardIdVo boardIdVo) {
        try {
            Long viewCount = dsl.select(sum(field("bv.view_number", Integer.class)))
                    .from(table("board_view").as("bv"))
                    .where(field("bv.board_id").eq(boardIdVo.value()))
                    .fetchOne(0, Long.class);

            return viewCount != null ? viewCount : 0L;
        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to get view count from DB - board: {}", boardIdVo.value(), e);
            return 0L;
        }
    }

    @Override
    public Map<Long, Long> getViewCountsFromDB(List<BoardIdVo> boardIds) {
        if (boardIds.isEmpty()) {
            return Map.of();
        }
        try {
            List<Long> ids = boardIds.stream().map(BoardIdVo::value).toList();
            Map<Long, Long> counts = new java.util.LinkedHashMap<>();
            Field<Long> boardIdField = field("bv.board_id", Long.class).as("board_id");
            Field<Long> countField = sum(field("bv.view_number", Integer.class)).cast(Long.class).as("count");
            dsl.select(boardIdField, countField)
                    .from(table("board_view").as("bv"))
                    .where(field("bv.board_id").in(ids))
                    .groupBy(field("bv.board_id"))
                    .fetch()
                    .forEach(record -> counts.put(
                            record.get(boardIdField),
                            record.get(countField) != null ? record.get(countField) : 0L
                    ));
            return counts;
        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to get view counts from DB for boards: {}", boardIds.size(), e);
            return Map.of();
        }
    }

    @Override
    public boolean hasMemberLiked(BoardIdVo boardIdVo, Long memberId) {
        try {
            Integer count = dsl.selectCount()
                    .from(table("board_like").as("bl"))
                    .where(field("bl.board_id").eq(boardIdVo.value()))
                    .and(field("bl.member_id").eq(memberId))
                    .fetchOne(0, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to check hasMemberLiked - board: {}, member: {}", boardIdVo.value(), memberId, e);
            return false;
        }
    }
    @Override
    public List<AttachmentVo> findAttachmentsByBoardId(BoardIdVo boardIdVo) {
        try {
            @NotNull Result<Record5<Object, Object, Object, Object, Object>> records = dsl.select(
                            field("a.id").as("attachment_id"),
                            field("a.name").as("attachment_name"),
                            field("a.type").as("attachment_type"),
                            field("a.size").as("attachment_size"),
                            field("a.attached_url").as("attachment_url")
                    )
                    .from(table("board_attached").as("a"))
                    .where(field("a.board_id").eq(boardIdVo.value()))
                    .and(field("a.deleted_at").isNull())
                    .orderBy(field("a.created_at").asc())
                    .fetch();

            return records.stream()
                    .map(this::recordToAttachmentVo)
                    .toList();
        } catch (Exception e) {
            log.error("❌ Infrastructure: Failed to find attachments for board: {}", boardIdVo.value(), e);
            throw new DomainException(ExceptionStatus.BOARD_INFRASTRUCTURE_QUERY_FAILED,
                    "첨부파일 조회에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public BoardStatsVo getBoardStats(BoardIdVo boardIdVo) {
        log.debug("🔍 Infrastructure: Getting board stats - ID: {}", boardIdVo.value());

        try {
            // Like 통계 조회
            Record likeRecord = dsl.select(
                            field("id", Long.class),
                            field("like_number", Long.class)
                    )
                    .from(table("board_like"))
                    .where(field("board_id").eq(boardIdVo.value()))
                    .orderBy(field("updated_at").desc())
                    .limit(1)
                    .fetchOne();

            // View 통계 조회
            Record viewRecord = dsl.select(
                            field("id", Long.class),
                            field("view_number", Long.class)
                    )
                    .from(table("board_view"))
                    .where(field("board_id").eq(boardIdVo.value()))
                    .orderBy(field("updated_at").desc())
                    .limit(1)
                    .fetchOne();

            Long likeCount = likeRecord != null ? likeRecord.get("like_number", Long.class) : 0L;
            Long viewCount = viewRecord != null ? viewRecord.get("view_number", Long.class) : 0L;
            Long likeId = likeRecord != null ? likeRecord.get("id", Long.class) : null;
            Long viewId = viewRecord != null ? viewRecord.get("id", Long.class) : null;

            BoardStatsVo result = BoardStatsVo.of(likeCount, viewCount, likeId, viewId);

            log.debug("🔍 Found board stats: likes={}, views={}, likeId={}, viewId={}",
                    likeCount, viewCount, likeId, viewId);

            return result;

        } catch (Exception e) {
            log.error("❌ Failed to get board stats: {}", boardIdVo.value(), e);
            return BoardStatsVo.empty();
        }
    }

    private BoardVo recordToBoardVo(Record record) {
        return BoardVo.of(
                record.get("id", Long.class),
                record.get("title", String.class),
                record.get("content", String.class),
                record.get("description", String.class),
                record.get("category", String.class),
                record.get("member_id", Long.class),
                record.get("created_at", OffsetDateTime.class),
                record.get("updated_at", OffsetDateTime.class),
                List.of()
        );

    }

    private BoardSummaryVo recordToBoardSummaryVo(Record record) {
        return BoardSummaryVo.of(
                record.get("id", Long.class),
                record.get("title", String.class),
                record.get("description", String.class),
                record.get("category", String.class),
                record.get("author_id", Long.class),
                record.get("author_name", String.class),
                record.get("updated_at", OffsetDateTime.class),
                0L,
                0L
        );
    }

    private AttachmentVo recordToAttachmentVo(Record record) {
        return AttachmentVo.of(
                record.get("attachment_id", Long.class),
                record.get("attachment_name", String.class),
                record.get("attachment_type", String.class),
                record.get("attachment_size", String.class),
                record.get("attachment_url", String.class)
        );
    }
}
