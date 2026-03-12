package org.certis.studyplatform.board.infrastructure.persistence;

public final class BoardStatRedisKeys {

    private static final String LIKE_ACTIVE_PREFIX = "certis:board:like:active:";
    private static final String LIKE_FLUSH_PREFIX = "certis:board:like:flush:";
    private static final String LIKE_MEMBERS_PREFIX = "certis:board:like:members:";
    private static final String VIEW_ACTIVE_PREFIX = "certis:board:view:active:";
    private static final String VIEW_FLUSH_PREFIX = "certis:board:view:flush:";
    private static final String VIEW_MEMBERS_PREFIX = "certis:board:view:members:";

    private BoardStatRedisKeys() {
    }

    public static String likeActive(Long boardId) {
        return LIKE_ACTIVE_PREFIX + boardId;
    }

    public static String likeActivePrefix() {
        return LIKE_ACTIVE_PREFIX;
    }

    public static String likeFlush(Long boardId) {
        return LIKE_FLUSH_PREFIX + boardId;
    }

    public static String likeFlushPrefix() {
        return LIKE_FLUSH_PREFIX;
    }

    public static String likeMembers(Long boardId) {
        return LIKE_MEMBERS_PREFIX + boardId;
    }

    public static String viewActive(Long boardId) {
        return VIEW_ACTIVE_PREFIX + boardId;
    }

    public static String viewActivePrefix() {
        return VIEW_ACTIVE_PREFIX;
    }

    public static String viewFlush(Long boardId) {
        return VIEW_FLUSH_PREFIX + boardId;
    }

    public static String viewFlushPrefix() {
        return VIEW_FLUSH_PREFIX;
    }

    public static String viewMembers(Long boardId) {
        return VIEW_MEMBERS_PREFIX + boardId;
    }
}
