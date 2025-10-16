package org.certis.studyplatform.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 스터디 플랫폼 응답 상태 정의
 *
 * 네이밍 규칙: {DOMAIN}_{ACTION}_{STATUS}
 * 예: MEMBER_CREATE_SUCCESS, MEMBER_FIND_SUCCESS, PROJECT_CREATE_SUCCESS
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@RequiredArgsConstructor
@Getter
public enum ResponseStatus {


    // =================================================================
    // PROFILE DOMAIN RESPONSE CODES
    // =================================================================
    PROFILE_CREATE_SUCCESS(HttpStatus.CREATED, "회원이 성공적으로 생성되었습니다"),
    PROFILE_FIND_SUCCESS(HttpStatus.OK, "회원을 성공적으로 조회했습니다"),
    PROFILE_UPDATE_SUCCESS(HttpStatus.OK, "회원 정보가 성공적으로 갱신되었습니다"),
    PROFILE_DELETE_SUCCESS(HttpStatus.OK, "회원이 성공적으로 삭제되었습니다"),
    PROFILE_PROFILE_UPDATE_SUCCESS(HttpStatus.OK, "회원 프로필이 성공적으로 갱신되었습니다"),
    PROFILE_SKILLS_UPDATE_SUCCESS(HttpStatus.OK, "회원 기술 스택이 성공적으로 갱신되었습니다"),
    PROFILE_ROLE_UPDATE_SUCCESS(HttpStatus.OK, "회원 역할이 성공적으로 갱신되었습니다"),
    PROFILE_LIST_SUCCESS(HttpStatus.OK, "회원 목록을 성공적으로 조회했습니다"),
    PROFILE_SEARCH_SUCCESS(HttpStatus.OK, "회원 검색을 성공적으로 완료했습니다"),

    // =================================================================
    // MEMBER DOMAIN RESPONSE CODES
    // =================================================================
    MEMBER_CREATE_SUCCESS(HttpStatus.CREATED, "회원이 성공적으로 생성되었습니다"),
    MEMBER_FIND_SUCCESS(HttpStatus.OK, "회원을 성공적으로 조회했습니다"),
    MEMBER_UPDATE_SUCCESS(HttpStatus.OK, "회원 정보가 성공적으로 갱신되었습니다"),
    MEMBER_DELETE_SUCCESS(HttpStatus.OK, "회원이 성공적으로 삭제되었습니다"),
    MEMBER_PROFILE_UPDATE_SUCCESS(HttpStatus.OK, "회원 프로필이 성공적으로 갱신되었습니다"),
    MEMBER_SKILLS_UPDATE_SUCCESS(HttpStatus.OK, "회원 기술 스택이 성공적으로 갱신되었습니다"),
    MEMBER_ROLE_UPDATE_SUCCESS(HttpStatus.OK, "회원 역할이 성공적으로 갱신되었습니다"),
    MEMBER_LIST_SUCCESS(HttpStatus.OK, "회원 목록을 성공적으로 조회했습니다"),
    MEMBER_SEARCH_SUCCESS(HttpStatus.OK, "회원 검색을 성공적으로 완료했습니다"),
    MEMBER_ADMIN_PROFILE_UPDATE_SUCCESS(HttpStatus.OK, "관리자권한으로 회원 프로필이 성공적으로 갱신되었습니다"),
    MEMBER_ADMIN_SEARCH_SUCCESS(HttpStatus.OK, "관리자권한으로 회원 목록을 성공적으로 조회했습니다"),
    MEMBER_ADMIN_PENALTY_UPDATE_SUCCESS(HttpStatus.OK, "관리자권한으로 회원 벌점이 성공적으로 갱신되었습니다"),
    MEMBER_ADMIN_GRACE_PERIOD_UPDATE_SUCCESS(HttpStatus.OK, "관리자권한으로 회원 유예기간이 성공적으로 갱신되었습니다"),


    // =================================================================
    // PROJECT DOMAIN RESPONSE CODES
    // =================================================================
    PROJECT_CREATE_SUCCESS(HttpStatus.CREATED, "프로젝트가 성공적으로 생성되었습니다"),
    PROJECT_FIND_SUCCESS(HttpStatus.OK, "프로젝트를 성공적으로 조회했습니다"),
    PROJECT_UPDATE_SUCCESS(HttpStatus.OK, "프로젝트 정보가 성공적으로 갱신되었습니다"),
    PROJECT_DELETE_SUCCESS(HttpStatus.OK, "프로젝트가 성공적으로 삭제되었습니다"),
    PROJECT_END_SUCCESS(HttpStatus.OK, "프로젝트가 성공적으로 종료되었습니다"),
    PROJECT_END_REJECT_SUCCESS(HttpStatus.OK, "프로젝트 종료 제출이 성공적으로 거절되었습니다"),
    PROJECT_JOIN_SUCCESS(HttpStatus.OK, "프로젝트에 성공적으로 참여했습니다"),
    PROJECT_LEAVE_SUCCESS(HttpStatus.OK, "프로젝트에서 성공적으로 탈퇴했습니다"),
    PROJECT_LIST_SUCCESS(HttpStatus.OK, "프로젝트 목록을 성공적으로 조회했습니다"),
    PROJECT_SEARCH_SUCCESS(HttpStatus.OK, "프로젝트 검색을 성공적으로 완료했습니다"),
    PROJECT_STATUS_UPDATE_SUCCESS(HttpStatus.OK, "프로젝트 상태가 성공적으로 변경되었습니다"),
    
    // Project Meeting Response Codes
    PROJECT_MEETING_CREATE_SUCCESS(HttpStatus.CREATED, "프로젝트 회의록이 성공적으로 생성되었습니다"),
    PROJECT_MEETING_FIND_SUCCESS(HttpStatus.OK, "프로젝트 회의록을 성공적으로 조회했습니다"),
    PROJECT_MEETING_UPDATE_SUCCESS(HttpStatus.OK, "프로젝트 회의록이 성공적으로 수정되었습니다"),
    PROJECT_MEETING_DELETE_SUCCESS(HttpStatus.OK, "프로젝트 회의록이 성공적으로 삭제되었습니다"),

    PROJECT_PARTICIPANT_JOIN_REGISTERED(HttpStatus.CREATED, "프로젝트 참가 신청이 성공했습니다"),
    PROJECT_PARTICIPANT_JOIN_RESTORED(HttpStatus.CREATED, "프로젝트 참가 신청이 성공했습니다"),
    PROJECT_PARTICIPANT_JOIN_CANCELED(HttpStatus.OK, "프로젝트 참가 취소가 성공했습니다"),
    PROJECT_PARTICIPANT_JOIN_APPROVED(HttpStatus.OK, "프로젝트 참가가 승인되었습니다"),
    PROJECT_PARTICIPANT_JOIN_REJECTED(HttpStatus.OK, "프로젝트 참가가 거절되었습니다"),
    PROJECT_PARTICIPANT_APPROVE_SUCCESS(HttpStatus.OK, "프로젝트 참가 신청이 관리자에 의해 성공적으로 승인되었습니다"),
    PROJECT_PARTICIPANT_REJECT_SUCCESS(HttpStatus.OK, "프로젝트 참가 신청이 관리자에 의해 성공적으로 거절되었습니다"),
    PROJECT_PARTICIPANT_SEARCH_SUCCESS(HttpStatus.OK, "프로젝트 참가자 조회에 성공했습니다."),

    // =================================================================
    // STUDY DOMAIN RESPONSE CODES
    // =================================================================
    STUDY_CREATE_SUCCESS(HttpStatus.CREATED, "스터디가 성공적으로 생성되었습니다"),
    STUDY_FIND_SUCCESS(HttpStatus.OK, "스터디를 성공적으로 조회했습니다"),
    STUDY_UPDATE_SUCCESS(HttpStatus.OK, "스터디 정보가 성공적으로 갱신되었습니다"),
    STUDY_DELETE_SUCCESS(HttpStatus.OK, "스터디가 성공적으로 삭제되었습니다"),
    STUDY_END_SUCCESS(HttpStatus.OK, "스터디가 성공적으로 종료되었습니다"),
    STUDY_END_REJECT_SUCCESS(HttpStatus.OK, "스터디 종료 제출이 성공적으로 거절되었습니다"),
    STUDY_JOIN_SUCCESS(HttpStatus.OK, "스터디에 성공적으로 참여했습니다"),
    STUDY_LEAVE_SUCCESS(HttpStatus.OK, "스터디에서 성공적으로 탈퇴했습니다"),
    STUDY_SESSION_CREATE_SUCCESS(HttpStatus.CREATED, "스터디 세션이 성공적으로 생성되었습니다"),
    STUDY_SESSION_UPDATE_SUCCESS(HttpStatus.OK, "스터디 세션이 성공적으로 갱신되었습니다"),
    STUDY_MINUTES_UPLOAD_SUCCESS(HttpStatus.CREATED, "스터디 회의록이 성공적으로 업로드되었습니다"),
    STUDY_LIST_SUCCESS(HttpStatus.OK, "스터디 목록을 성공적으로 조회했습니다"),
    STUDY_SEARCH_SUCCESS(HttpStatus.OK, "스터디 검색을 성공적으로 완료했습니다"),
    STUDY_DETAIL_SUCCESS(HttpStatus.OK, "스터디 상세 정보를 성공적으로 조회했습니다"),
    STUDY_ATTACHMENTS_SUCCESS(HttpStatus.OK, "스터디 첨부파일을 성공적으로 조회했습니다"),
    STUDY_MEETINGS_SUCCESS(HttpStatus.OK, "스터디 회의록을 성공적으로 조회했습니다"),
    
    // Study Meeting Response Codes
    STUDY_MEETING_CREATE_SUCCESS(HttpStatus.CREATED, "스터디 회의록이 성공적으로 생성되었습니다"),
    STUDY_MEETING_FIND_SUCCESS(HttpStatus.OK, "스터디 회의록을 성공적으로 조회했습니다"),
    STUDY_MEETING_UPDATE_SUCCESS(HttpStatus.OK, "스터디 회의록이 성공적으로 수정되었습니다"),
    STUDY_MEETING_DELETE_SUCCESS(HttpStatus.OK, "스터디 회의록이 성공적으로 삭제되었습니다"),

    STUDY_PARTICIPANT_JOIN_REGISTERED(HttpStatus.CREATED, "스터디 참가 신청이 성공했습니다"),
    STUDY_PARTICIPANT_JOIN_CANCELED(HttpStatus.OK, "스터디 참가 취소가 성공했습니다"),
    STUDY_PARTICIPANT_JOIN_APPROVED(HttpStatus.OK, "스터디 참가가 승인되었습니다"),
    STUDY_PARTICIPANT_JOIN_REJECTED(HttpStatus.OK, "스터디 참가가 거절되었습니다"),
    STUDY_PARTICIPANT_APPROVE_SUCCESS(HttpStatus.OK, "스터디 참가 신청이 관리자에 의해 성공적으로 승인되었습니다"),
    STUDY_PARTICIPANT_REJECT_SUCCESS(HttpStatus.OK, "스터디 참가 신청이 관리자에 의해 성공적으로 거절되었습니다"),
    STUDY_PARTICIPANT_SEARCH_SUCCESS(HttpStatus.OK, "스터디 참가자 조회에 성공했습니다."),

    // =================================================================
    // BOARD DOMAIN RESPONSE CODES
    // =================================================================
    BOARD_CREATE_SUCCESS(HttpStatus.CREATED, "게시글이 성공적으로 생성되었습니다"),
    BOARD_FIND_SUCCESS(HttpStatus.OK, "게시글을 성공적으로 조회했습니다"),
    BOARD_UPDATE_SUCCESS(HttpStatus.OK, "게시글이 성공적으로 갱신되었습니다"),
    BOARD_DELETE_SUCCESS(HttpStatus.OK, "게시글이 성공적으로 삭제되었습니다"),
    BOARD_LIKE_SUCCESS(HttpStatus.OK, "게시글을 성공적으로 좋아요했습니다"),
    BOARD_UNLIKE_SUCCESS(HttpStatus.OK, "게시글 좋아요를 성공적으로 취소했습니다"),
    BOARD_LIST_SUCCESS(HttpStatus.OK, "게시글 목록을 성공적으로 조회했습니다"),
    BOARD_SEARCH_SUCCESS(HttpStatus.OK, "게시글 검색을 성공적으로 완료했습니다"),
    BOARD_VIEW_INCREMENT_SUCCESS(HttpStatus.OK, "게시글 조회수가 성공적으로 증가했습니다"),
    BOARD_SYNC_SUCCESS(HttpStatus.OK, "BOARD_SYNC_SUCCESS"),
    BOARD_STATS_FIND_SUCCESS(HttpStatus.OK, "BOARD_STATS_FIND_SUCCESS"),

    // =================================================================
    // COMMENT RESPONSE CODES
    // =================================================================
    COMMENT_CREATE_SUCCESS(HttpStatus.CREATED, "댓글이 성공적으로 생성되었습니다"),
    COMMENT_FIND_SUCCESS(HttpStatus.OK, "댓글을 성공적으로 조회했습니다"),
    COMMENT_UPDATE_SUCCESS(HttpStatus.OK, "댓글이 성공적으로 갱신되었습니다"),
    COMMENT_DELETE_SUCCESS(HttpStatus.OK, "댓글이 성공적으로 삭제되었습니다"),
    COMMENT_LIST_SUCCESS(HttpStatus.OK, "댓글 목록을 성공적으로 조회했습니다"),
    COMMENT_LIKE_SUCCESS(HttpStatus.OK, "댓글을 성공적으로 좋아요했습니다"),
    COMMENT_UNLIKE_SUCCESS(HttpStatus.OK, "댓글 좋아요를 성공적으로 취소했습니다"),

    // =================================================================
    // BLOG DOMAIN RESPONSE CODES
    // =================================================================
    BLOG_CREATE_SUCCESS(HttpStatus.CREATED, "블로그 글이 성공적으로 생성되었습니다"),
    BLOG_FIND_SUCCESS(HttpStatus.OK, "블로그 글을 성공적으로 조회했습니다"),
    BLOG_UPDATE_SUCCESS(HttpStatus.OK, "블로그 글이 성공적으로 갱신되었습니다"),
    BLOG_DELETE_SUCCESS(HttpStatus.OK, "블로그 글이 성공적으로 삭제되었습니다"),
    BLOG_LIST_SUCCESS(HttpStatus.OK, "블로그 글 목록을 성공적으로 조회했습니다"),
    BLOG_SEARCH_SUCCESS(HttpStatus.OK, "블로그 글 검색을 성공적으로 완료했습니다"),
    BLOG_PUBLISH_SUCCESS(HttpStatus.OK, "블로그 글이 성공적으로 발행되었습니다"),
    BLOG_DRAFT_SAVE_SUCCESS(HttpStatus.OK, "블로그 초안이 성공적으로 저장되었습니다"),

    // =================================================================
    // SCHEDULE DOMAIN RESPONSE CODES
    // =================================================================
    SCHEDULE_CREATE_SUCCESS(HttpStatus.CREATED, "일정이 성공적으로 생성되었습니다"),
    SCHEDULE_FIND_SUCCESS(HttpStatus.OK, "일정을 성공적으로 조회했습니다"),
    SCHEDULE_UPDATE_SUCCESS(HttpStatus.OK, "일정이 성공적으로 갱신되었습니다"),
    SCHEDULE_DELETE_SUCCESS(HttpStatus.OK, "일정이 성공적으로 삭제되었습니다"),
    SCHEDULE_APPROVE_SUCCESS(HttpStatus.OK, "스케줄이 성공적으로 승인되었습니다"),
    SCHEDULE_REJECT_SUCCESS(HttpStatus.OK, "스케줄이 성공적으로 거절되었습니다"),
    SCHEDULE_LIST_SUCCESS(HttpStatus.OK, "일정 목록을 성공적으로 조회했습니다"),
    SCHEDULE_SEARCH_SUCCESS(HttpStatus.OK, "일정 검색을 성공적으로 완료했습니다"),
    SCHEDULE_REMINDER_SET_SUCCESS(HttpStatus.OK, "일정 알림이 성공적으로 설정되었습니다"),

    // =================================================================
    // AUTHENTICATION & AUTHORIZATION RESPONSE CODES
    // =================================================================
    AUTH_LOGIN_SUCCESS(HttpStatus.OK, "성공적으로 로그인되었습니다"),
    AUTH_LOGOUT_SUCCESS(HttpStatus.OK, "성공적으로 로그아웃되었습니다"),
    AUTH_TOKEN_REFRESH_SUCCESS(HttpStatus.OK, "토큰이 성공적으로 갱신되었습니다"),
    AUTH_VERIFY_SUCCESS(HttpStatus.OK, "성공적으로 인증되었습니다"),
    AUTH_PASSWORD_CHANGE_SUCCESS(HttpStatus.OK, "비밀번호가 성공적으로 변경되었습니다"),
    AUTH_REGISTER_SUCCESS(HttpStatus.CREATED, "회원가입이 성공적으로 완료되었습니다"),
    AUTH_REGISTER_REQUEST_SUCCESS(HttpStatus.CREATED, "회원가입 요청이 성공적으로 완료되었습니다"),
    AUTH_EMAIL_VERIFY_SUCCESS(HttpStatus.OK, "이메일 인증이 성공적으로 완료되었습니다"),
    AUTH_PASSWORD_RESET_SUCCESS(HttpStatus.OK, "비밀번호 재설정이 성공적으로 완료되었습니다"),

    // =================================================================
    // FILE UPLOAD RESPONSE CODES
    // =================================================================
    FILE_UPLOAD_SUCCESS(HttpStatus.CREATED, "파일이 성공적으로 업로드되었습니다"),
    FILE_DOWNLOAD_SUCCESS(HttpStatus.OK, "파일이 성공적으로 다운로드되었습니다"),
    FILE_DELETE_SUCCESS(HttpStatus.OK, "파일이 성공적으로 삭제되었습니다"),
    FILE_LIST_SUCCESS(HttpStatus.OK, "파일 목록을 성공적으로 조회했습니다"),
    FILE_SEARCH_SUCCESS(HttpStatus.OK, "파일 검색을 성공적으로 완료했습니다"),
    FILE_METADATA_UPDATE_SUCCESS(HttpStatus.OK, "파일 메타데이터가 성공적으로 갱신되었습니다"),

    // =================================================================
    // NOTIFICATION RESPONSE CODES
    // =================================================================
    NOTIFICATION_CREATE_SUCCESS(HttpStatus.CREATED, "알림이 성공적으로 생성되었습니다"),
    NOTIFICATION_FIND_SUCCESS(HttpStatus.OK, "알림을 성공적으로 조회했습니다"),
    NOTIFICATION_READ_SUCCESS(HttpStatus.OK, "알림을 성공적으로 읽음 처리했습니다"),
    NOTIFICATION_DELETE_SUCCESS(HttpStatus.OK, "알림이 성공적으로 삭제되었습니다"),
    NOTIFICATION_LIST_SUCCESS(HttpStatus.OK, "알림 목록을 성공적으로 조회했습니다"),
    NOTIFICATION_SEND_SUCCESS(HttpStatus.OK, "알림이 성공적으로 전송되었습니다"),
    NOTIFICATION_SETTINGS_UPDATE_SUCCESS(HttpStatus.OK, "알림 설정이 성공적으로 갱신되었습니다"),

    // =================================================================
    // SYSTEM LEVEL RESPONSE CODES
    // =================================================================
    SYSTEM_HEALTH_CHECK_SUCCESS(HttpStatus.OK, "시스템 상태 확인이 성공적으로 완료되었습니다"),
    SYSTEM_BACKUP_SUCCESS(HttpStatus.OK, "시스템 백업이 성공적으로 완료되었습니다"),
    SYSTEM_CACHE_CLEAR_SUCCESS(HttpStatus.OK, "캐시가 성공적으로 삭제되었습니다"),
    SYSTEM_MAINTENANCE_START_SUCCESS(HttpStatus.OK, "시스템 점검이 성공적으로 시작되었습니다"),
    SYSTEM_MAINTENANCE_END_SUCCESS(HttpStatus.OK, "시스템 점검이 성공적으로 종료되었습니다");

    private final int statusCode;
    private final String message;

    ResponseStatus(HttpStatus status, String message) {
        this.statusCode = status.value();
        this.message = message;
    }
}

