package org.certis.studyplatform.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 스터디 플랫폼 예외 상태 정의
 * 
 * 네이밍 규칙: {DOMAIN}_{LAYER}_{ERROR_TYPE}
 * 예: MEMBER_INFRASTRUCTURE_NOT_FOUND, PROJECT_DOMAIN_CAPACITY_EXCEEDED
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@RequiredArgsConstructor
@Getter
public enum ExceptionStatus {

    // =================================================================
    // GENERIC PRESENTATION LAYER EXCEPTIONS (4xx)
    // =================================================================
    PRESENTATION_VALIDATION_INVALID_REQUEST_DATA(HttpStatus.BAD_REQUEST, "요청 데이터가 유효하지 않습니다"),
    PRESENTATION_VALIDATION_MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "필수 파라미터가 누락되었습니다"),
    PRESENTATION_VALIDATION_INVALID_PATH_VARIABLE(HttpStatus.BAD_REQUEST, "경로 변수가 유효하지 않습니다"),
    PRESENTATION_HTTP_METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않는 HTTP 메서드입니다"),
    PRESENTATION_HTTP_UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다"),

    // AUTHENTICATION & AUTHORIZATION (401, 403)
    PRESENTATION_AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    PRESENTATION_AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),


    // Auth - Application Layer
    AUTH_APPLICATION_ACCOUNT_NOT_FOUND(HttpStatus.UNAUTHORIZED, "존재하지 않는 계정입니다"),
    AUTH_APPLICATION_PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다"),

    // Auth - Domain Layer
    AUTH_DOMAIN_INVALID_ACCOUNT_NUMBER_LENGTH(HttpStatus.BAD_REQUEST, "계정번호는 6자 이상 20자 이하여야 합니다"),
    AUTH_DOMAIN_DUPLICATE_ACCOUNT_NUMBER(HttpStatus.CONFLICT, "이미 존재하는 계정번호입니다"),
    AUTH_DOMAIN_WEAK_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호 정책을 만족하지 않습니다"),
    AUTH_DOMAIN_ACCOUNT_NOT_APPROVED(HttpStatus.FORBIDDEN, "승인되지 않은 계정입니다"),


    // Auth - Infrastructure (JWT token 관련)
    AUTH_INFRASTRUCTURE_INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 엑세스 토큰입니다"),
    AUTH_INFRASTRUCTURE_JWT_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "JWT 토큰이 만료되었습니다"),
    AUTH_INFRASTRUCTURE_JWT_TOKEN_INVALID_FORMAT(HttpStatus.UNAUTHORIZED, "JWT 토큰 형식이 올바르지 않습니다"),
    AUTH_INFRASTRUCTURE_JWT_TOKEN_UNSUPPORTED(HttpStatus.UNAUTHORIZED, "지원하지 않는 JWT 토큰입니다"),
    AUTH_INFRASTRUCTURE_JWT_TOKEN_INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "JWT 토큰 서명이 유효하지 않습니다"),
    AUTH_INFRASTRUCTURE_JWT_TOKEN_MISSING_CLAIMS(HttpStatus.UNAUTHORIZED, "JWT 토큰에 필수 정보가 없습니다"),
    AUTH_INFRASTRUCTURE_JWT_TOKEN_PARSE_ERROR(HttpStatus.UNAUTHORIZED, "JWT 토큰 파싱 중 오류가 발생했습니다"),
    AUTH_INFRASTRUCTURE_JWT_FILTER_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "JWT 필터 처리 중 예상치 못한 오류가 발생했습니다"),

    // Auth - Infrastructure Layer (Redis 관련)
    AUTH_INFRASTRUCTURE_REDIS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Redis 저장소 처리 중 오류가 발생했습니다"),

    // Auth - Infrastructure Layer
    AUTH_INFRASTRUCTURE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 인증정보입니다"),
    AUTH_INFRASTRUCTURE_DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "인증정보 데이터베이스 오류가 발생했습니다"),

    // Auth - Presentation Layer
    AUTH_PRESENTATION_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "인증 요청 데이터가 유효하지 않습니다"),

    // Auth - Domain Layer
    AUTH_DOMAIN_JWT_TOKEN_PARSE_ERROR(HttpStatus.UNAUTHORIZED, "JWT 토큰 파싱 중 오류가 발생했습니다"),
    AUTH_DOMAIN_JWT_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "JWT 토큰이 만료되었습니다"),
    AUTH_DOMAIN_ACCOUNT_NOT_FOUND(HttpStatus.UNAUTHORIZED, "존재하지 않는 계정입니다"),

    // =================================================================
    // MEMBER DOMAIN EXCEPTIONS
    // =================================================================
    
    // Member - Presentation Layer
    MEMBER_PRESENTATION_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "회원 요청 데이터가 유효하지 않습니다"),
    MEMBER_PRESENTATION_UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "회원 정보에 대한 접근 권한이 없습니다"),
    // Member - Application Layer  
    MEMBER_APPLICATION_BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "회원 비즈니스 규칙 위반입니다"),
    MEMBER_APPLICATION_COMMAND_EXECUTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "회원 명령 실행에 실패했습니다"),
    MEMBER_APPLICATION_QUERY_EXECUTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "회원 조회 실행에 실패했습니다"),
    MEMBER_APPLICATION_FACADE_OPERATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "회원 비즈니스 작업 실행에 실패했습니다"),
    MEMBER_APPLICATION_CANNOT_CHANGE_OWN_ADMIN_FIELDS(HttpStatus.FORBIDDEN,"자신의 권한을 바꿀 수 없습니다." ),
    // Member - Domain Layer
    MEMBER_DOMAIN_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "회원 도메인 규칙 위반입니다"),
    MEMBER_DOMAIN_AGGREGATE_CONSISTENCY_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "회원 애그리게이트 일관성 위반입니다"),
    MEMBER_DOMAIN_EVENT_PROCESSING_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "회원 도메인 이벤트 처리에 실패했습니다"),
    MEMBER_DOMAIN_DUPLICATE_STUDENT_NUMBER(HttpStatus.CONFLICT, "이미 존재하는 학번입니다"),
    MEMBER_DOMAIN_DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다"),
    MEMBER_DOMAIN_INVALID_STATUS(HttpStatus.UNPROCESSABLE_ENTITY, "유효하지 않은 회원 상태입니다"),
    MEMBER_DOMAIN_INSUFFICIENT_AUTHORITY(HttpStatus.FORBIDDEN, "유효하지 않은 권한 접근입니다"),
    MEMBER_DOMAIN_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다"),
    
    // Member - Domain VO Validation
    MEMBER_DOMAIN_INVALID_NAME(HttpStatus.BAD_REQUEST, "유효하지 않은 이름입니다"),
    MEMBER_DOMAIN_INVALID_STUDENT_NUMBER(HttpStatus.BAD_REQUEST, "유효하지 않은 학번입니다"),
    MEMBER_DOMAIN_INVALID_EMAIL(HttpStatus.BAD_REQUEST, "유효하지 않은 이메일입니다"),
    MEMBER_DOMAIN_INVALID_GRADE(HttpStatus.BAD_REQUEST, "유효하지 않은 학년입니다"),
    MEMBER_DOMAIN_INVALID_MAJOR(HttpStatus.BAD_REQUEST, "유효하지 않은 전공입니다"),
    MEMBER_DOMAIN_INVALID_ROLE(HttpStatus.BAD_REQUEST, "유효하지 않은 역할입니다"),
    MEMBER_DOMAIN_INVALID_SKILLS(HttpStatus.BAD_REQUEST, "유효하지 않은 기술 스택입니다"),
    
    // Member - Infrastructure Layer
    MEMBER_INFRASTRUCTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다"),
    MEMBER_INFRASTRUCTURE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 회원입니다"),
    MEMBER_INFRASTRUCTURE_DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "회원 데이터베이스 오류가 발생했습니다"),
    MEMBER_INFRASTRUCTURE_RESOURCE_CONFLICT(HttpStatus.CONFLICT, "회원 리소스 충돌이 발생했습니다"),
    MEMBER_INFRASTRUCTURE_EXTERNAL_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "회원 관련 외부 서비스 오류가 발생했습니다"),

    // =================================================================
    // PROFILE DOMAIN EXCEPTIONS
    // =================================================================

    // Profile - Presentation Layer
    PROFILE_PRESENTATION_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "프로필 요청 데이터가 유효하지 않습니다"),
    PROFILE_PRESENTATION_UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "프로필에 대한 접근 권한이 없습니다"),

    // Profile - Application Layer
    PROFILE_APPLICATION_BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "프로필 비즈니스 규칙 위반입니다"),
    PROFILE_APPLICATION_COMMAND_EXECUTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "프로필 명령 실행에 실패했습니다"),
    PROFILE_APPLICATION_QUERY_EXECUTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "프로필 조회 실행에 실패했습니다"),
    PROFILE_APPLICATION_FACADE_OPERATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "프로필 비즈니스 작업 실행에 실패했습니다"),

    // Profile - Domain Layer
    PROFILE_DOMAIN_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "프로필 도메인 규칙 위반입니다"),
    PROFILE_DOMAIN_AGGREGATE_CONSISTENCY_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "프로필 애그리게이트 일관성 위반입니다"),
    PROFILE_DOMAIN_EVENT_PROCESSING_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "프로필 도메인 이벤트 처리에 실패했습니다"),
    PROFILE_DOMAIN_UPDATE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "자신의 프로필만 수정할 수 있습니다"),
    PROFILE_DOMAIN_DUPLICATE_PROFILE(HttpStatus.CONFLICT, "이미 존재하는 프로필입니다"),
    PROFILE_DOMAIN_INVALID_STATUS(HttpStatus.UNPROCESSABLE_ENTITY, "유효하지 않은 프로필 상태입니다"),
    PROFILE_DOMAIN_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 프로필입니다."),

    // Profile - Domain VO Validation
    PROFILE_DOMAIN_INVALID_NAME(HttpStatus.BAD_REQUEST, "유효하지 않은 프로필 이름입니다"),
    PROFILE_DOMAIN_INVALID_DESCRIPTION(HttpStatus.BAD_REQUEST, "유효하지 않은 프로필 설명입니다"),
    PROFILE_DOMAIN_INVALID_IMAGE_URL(HttpStatus.BAD_REQUEST, "유효하지 않은 프로필 이미지 URL입니다"),
    PROFILE_DOMAIN_INVALID_CONTACT_INFO(HttpStatus.BAD_REQUEST, "유효하지 않은 연락처 정보입니다"),
    PROFILE_DOMAIN_INVALID_SOCIAL_LINKS(HttpStatus.BAD_REQUEST, "유효하지 않은 소셜 링크입니다"),
    PROFILE_DOMAIN_INAPPROPRIATE_CONTENT(HttpStatus.BAD_REQUEST, "부적절한 내용이 포함되어 있습니다"),
    PROFILE_DOMAIN_INVALID_PRIVACY_SETTING(HttpStatus.BAD_REQUEST, "유효하지 않은 프라이버시 설정입니다"),

    // Profile - Infrastructure Layer
    PROFILE_INFRASTRUCTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "프로필을 찾을 수 없습니다"),
    PROFILE_INFRASTRUCTURE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 프로필입니다"),
    PROFILE_INFRASTRUCTURE_DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "프로필 데이터베이스 오류가 발생했습니다"),
    PROFILE_INFRASTRUCTURE_RESOURCE_CONFLICT(HttpStatus.CONFLICT, "프로필 리소스 충돌이 발생했습니다"),
    PROFILE_INFRASTRUCTURE_EXTERNAL_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "프로필 관련 외부 서비스 오류가 발생했습니다"),
    PROFILE_INFRASTRUCTURE_IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "프로필 이미지 업로드에 실패했습니다"),
    PROFILE_INFRASTRUCTURE_IMAGE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "프로필 이미지 삭제에 실패했습니다"),

    // =================================================================
    // PROJECT DOMAIN EXCEPTIONS
    // =================================================================
    
    // Project - Presentation Layer
    PROJECT_PRESENTATION_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "프로젝트 요청 데이터가 유효하지 않습니다"),
    PROJECT_PRESENTATION_UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "프로젝트에 대한 접근 권한이 없습니다"),
    
    // Project - Application Layer
    PROJECT_APPLICATION_BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "프로젝트 비즈니스 규칙 위반입니다"),
    PROJECT_APPLICATION_COMMAND_EXECUTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "프로젝트 명령 실행에 실패했습니다"),
    PROJECT_APPLICATION_QUERY_EXECUTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "프로젝트 조회 실행에 실패했습니다"),
    
    // Project - Domain Layer
    PROJECT_DOMAIN_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "프로젝트 도메인 규칙 위반입니다"),
    PROJECT_DOMAIN_CAPACITY_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "프로젝트 정원을 초과했습니다"),
    PROJECT_DOMAIN_DEADLINE_PASSED(HttpStatus.UNPROCESSABLE_ENTITY, "프로젝트 마감일이 지났습니다"),
    PROJECT_DOMAIN_INVALID_STATUS(HttpStatus.UNPROCESSABLE_ENTITY, "유효하지 않은 프로젝트 상태입니다"),
    
    // Project - Domain VO Validation
    PROJECT_DOMAIN_INVALID_ID(HttpStatus.BAD_REQUEST, "유효하지 않은 프로젝트 ID입니다"),
    PROJECT_DOMAIN_INVALID_TITLE(HttpStatus.BAD_REQUEST, "유효하지 않은 프로젝트 제목입니다"),
    PROJECT_DOMAIN_INVALID_DESCRIPTION(HttpStatus.BAD_REQUEST, "유효하지 않은 프로젝트 설명입니다"),
    PROJECT_DOMAIN_INVALID_DIFFICULTY(HttpStatus.BAD_REQUEST, "유효하지 않은 프로젝트 난이도입니다"),
    PROJECT_DOMAIN_INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "유효하지 않은 프로젝트 카테고리입니다"),
    PROJECT_DOMAIN_INVALID_PARTICIPANT_LIMIT(HttpStatus.BAD_REQUEST, "유효하지 않은 참가자 제한 수입니다"),
    PROJECT_DOMAIN_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "유효하지 않은 프로젝트 기간입니다"),
    
    // Project - Infrastructure Layer
    PROJECT_INFRASTRUCTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다"),
    PROJECT_INFRASTRUCTURE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 프로젝트입니다"),
    PROJECT_INFRASTRUCTURE_DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "프로젝트 데이터베이스 오류가 발생했습니다"),
    PROJECT_INFRASTRUCTURE_RESOURCE_CONFLICT(HttpStatus.CONFLICT, "프로젝트 리소스 충돌이 발생했습니다"),

    // =================================================================
    // STUDY DOMAIN EXCEPTIONS
    // =================================================================
    
    // Study - Presentation Layer
    STUDY_PRESENTATION_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "스터디 요청 데이터가 유효하지 않습니다"),
    STUDY_PRESENTATION_UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "스터디에 대한 접근 권한이 없습니다"),
    
    // Study - Application Layer
    STUDY_APPLICATION_BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "스터디 비즈니스 규칙 위반입니다"),
    STUDY_APPLICATION_COMMAND_EXECUTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "스터디 명령 실행에 실패했습니다"),
    STUDY_APPLICATION_QUERY_EXECUTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "스터디 조회 실행에 실패했습니다"),
    
    // Study - Domain Layer
    STUDY_DOMAIN_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "스터디 도메인 규칙 위반입니다"),
    STUDY_DOMAIN_CAPACITY_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "스터디 정원을 초과했습니다"),
    STUDY_DOMAIN_SESSION_CONFLICT(HttpStatus.CONFLICT, "스터디 세션 시간이 중복됩니다"),
    STUDY_DOMAIN_INVALID_STATUS(HttpStatus.UNPROCESSABLE_ENTITY, "유효하지 않은 스터디 상태입니다"),
    
    // Study - Infrastructure Layer
    STUDY_INFRASTRUCTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "스터디를 찾을 수 없습니다"),
    STUDY_INFRASTRUCTURE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 스터디입니다"),
    STUDY_INFRASTRUCTURE_DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "스터디 데이터베이스 오류가 발생했습니다"),
    STUDY_INFRASTRUCTURE_RESOURCE_CONFLICT(HttpStatus.CONFLICT, "스터디 리소스 충돌이 발생했습니다"),

    // =================================================================
    // BOARD DOMAIN EXCEPTIONS
    // =================================================================
    
    // Board - Presentation Layer
    BOARD_PRESENTATION_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "게시글 요청 데이터가 유효하지 않습니다"),
    BOARD_PRESENTATION_UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "게시글에 대한 접근 권한이 없습니다"),
    
    // Board - Application Layer
    BOARD_APPLICATION_BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "게시글 비즈니스 규칙 위반입니다"),
    BOARD_APPLICATION_COMMAND_EXECUTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "게시글 명령 실행에 실패했습니다"),
    BOARD_APPLICATION_QUERY_EXECUTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "게시글 조회 실행에 실패했습니다"),
    
    // Board - Domain Layer
    BOARD_DOMAIN_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "게시글 도메인 규칙 위반입니다"),
    BOARD_DOMAIN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "게시글 접근 권한이 없습니다"),
    BOARD_DOMAIN_INVALID_STATUS(HttpStatus.UNPROCESSABLE_ENTITY, "유효하지 않은 게시글 상태입니다"),
    
    // Board - Infrastructure Layer
    BOARD_INFRASTRUCTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다"),
    BOARD_INFRASTRUCTURE_DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "게시글 데이터베이스 오류가 발생했습니다"),
    BOARD_INFRASTRUCTURE_FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "첨부파일 업로드에 실패했습니다"),
    BOARD_INFRASTRUCTURE_RESOURCE_CONFLICT(HttpStatus.CONFLICT, "게시글 리소스 충돌이 발생했습니다"),

    // Comment - Infrastructure Layer
    COMMENT_INFRASTRUCTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다"),
    COMMENT_INFRASTRUCTURE_DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "댓글 데이터베이스 오류가 발생했습니다"),

    // =================================================================
    // BLOG DOMAIN EXCEPTIONS
    // =================================================================
    
    // Blog - Infrastructure Layer
    BLOG_INFRASTRUCTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "블로그 글을 찾을 수 없습니다"),
    BLOG_INFRASTRUCTURE_DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "블로그 데이터베이스 오류가 발생했습니다"),

    // =================================================================
    // SCHEDULE DOMAIN EXCEPTIONS  
    // =================================================================

    // Schedule - Presentation Layer
    SCHEDULE_PRESENTATION_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "스케줄 요청 데이터가 유효하지 않습니다"),
    SCHEDULE_PRESENTATION_UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "스케줄에 대한 접근 권한이 없습니다"),

    // Schedule - Application Layer
    SCHEDULE_APPLICATION_BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "스케줄 비즈니스 규칙 위반입니다"),
    SCHEDULE_APPLICATION_COMMAND_EXECUTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "스케줄 명령 실행에 실패했습니다"),
    SCHEDULE_APPLICATION_QUERY_EXECUTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "스케줄 조회 실행에 실패했습니다"),
    SCHEDULE_APPLICATION_DELETE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "스케줄 삭제 권한이 없습니다"),

    // Schedule - Domain Layer
    SCHEDULE_DOMAIN_INVALID_ID(HttpStatus.UNPROCESSABLE_ENTITY, "유효하지 않은 스케줄 ID입니다"),
    SCHEDULE_DOMAIN_INVALID_TITLE(HttpStatus.UNPROCESSABLE_ENTITY, "스케줄 제목이 유효하지 않습니다"),
    SCHEDULE_DOMAIN_INVALID_DESCRIPTION(HttpStatus.UNPROCESSABLE_ENTITY, "스케줄 설명이 유효하지 않습니다"),
    SCHEDULE_DOMAIN_INVALID_TYPE(HttpStatus.UNPROCESSABLE_ENTITY, "스케줄 타입이 유효하지 않습니다"),
    SCHEDULE_DOMAIN_INVALID_PLACE(HttpStatus.UNPROCESSABLE_ENTITY, "스케줄 장소가 유효하지 않습니다"),
    SCHEDULE_DOMAIN_INVALID_TIME(HttpStatus.UNPROCESSABLE_ENTITY, "스케줄 시간이 유효하지 않습니다"),
    SCHEDULE_DOMAIN_INVALID_TIME_ORDER(HttpStatus.UNPROCESSABLE_ENTITY, "시작 시간이 종료 시간보다 늦을 수 없습니다"),
    SCHEDULE_DOMAIN_INVALID_PAST_TIME(HttpStatus.UNPROCESSABLE_ENTITY, "과거 시간으로 스케줄을 생성할 수 없습니다"),
    SCHEDULE_DOMAIN_INVALID_STATUS(HttpStatus.UNPROCESSABLE_ENTITY, "유효하지 않은 스케줄 상태입니다"),
    SCHEDULE_DOMAIN_TIME_CONFLICT(HttpStatus.CONFLICT, "스케줄 시간이 중복됩니다"),
    SCHEDULE_DOMAIN_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 스케줄입니다"),
    SCHEDULE_DOMAIN_NOT_PENDING(HttpStatus.UNPROCESSABLE_ENTITY, "대기 상태가 아닌 스케줄은 처리할 수 없습니다"),
    SCHEDULE_DOMAIN_NOT_APPROVED(HttpStatus.UNPROCESSABLE_ENTITY, "승인되지 않은 스케줄입니다"),

    // Schedule - Infrastructure Layer (새로 추가)
    SCHEDULE_INFRASTRUCTURE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 스케줄입니다"),
    SCHEDULE_INFRASTRUCTURE_RESOURCE_CONFLICT(HttpStatus.CONFLICT, "스케줄 리소스 충돌이 발생했습니다"),
    SCHEDULE_INFRASTRUCTURE_STATUS_NOT_FOUND(HttpStatus.NOT_FOUND, "스케줄 상태를 찾을 수 없습니다"),
    SCHEDULE_INFRASTRUCTURE_STATUS_DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "스케줄 상태 데이터베이스 오류가 발생했습니다"),

    // =================================================================
    // FILE & NOTIFICATION EXCEPTIONS
    // =================================================================
    
    // File - Infrastructure Layer
    FILE_INFRASTRUCTURE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다"),
    FILE_INFRASTRUCTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다"),
    FILE_INFRASTRUCTURE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다"),
    FILE_INFRASTRUCTURE_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장소 오류가 발생했습니다"),
    
    // Notification - Infrastructure Layer  
    NOTIFICATION_INFRASTRUCTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다"),
    NOTIFICATION_INFRASTRUCTURE_DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알림 데이터베이스 오류가 발생했습니다"),
    NOTIFICATION_INFRASTRUCTURE_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "알림 전송에 실패했습니다"),

    // =================================================================
    // SYSTEM LEVEL EXCEPTIONS (500)
    // =================================================================
    SYSTEM_INFRASTRUCTURE_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다"),
    SYSTEM_INFRASTRUCTURE_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서비스를 사용할 수 없습니다"),
    SYSTEM_INFRASTRUCTURE_ASYNC_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "비동기 처리 오류가 발생했습니다"),
    SYSTEM_INFRASTRUCTURE_CACHE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "캐시 처리 오류가 발생했습니다"),
    SYSTEM_INFRASTRUCTURE_MESSAGING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "메시징 처리 오류가 발생했습니다");

    private final int statusCode;
    private final String message;

    ExceptionStatus(HttpStatus status, String message) {
        this.statusCode = status.value();
        this.message = message;
    }
} 