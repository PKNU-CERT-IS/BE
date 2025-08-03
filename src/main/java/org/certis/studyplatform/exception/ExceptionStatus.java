package org.certis.studyplatform.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@RequiredArgsConstructor
@Getter
public enum ExceptionStatus {

    // AUTH ERROR CODE
    AUTH_INVALID_PARAMS(HttpStatus.BAD_REQUEST, "잘못된 인자로 요청했습니다"),
    AUTH_DUPLICATE_STUDENT_NUMBER(HttpStatus.BAD_REQUEST,"이미 존재하는 유저입니다."),
    AUTH_DELETED_USER(HttpStatus.BAD_REQUEST,"탈퇴된 유저입니다."),
    AUTH_BAD_SESSION_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 세션으로 인한 요청 방법입니다."),
    AUTH_COOKIE_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "잘못된 쿠키로 접근했습니다"),
    AUTH_SESSION_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "잘못된 세션으로 접근했습니다"),
    AUTH_MISMATCH_PASSWORD(HttpStatus.UNAUTHORIZED,"비밀번호 불일치"),
    AUTH_MISMATCH_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED,"리프레시 토큰 불일치."),

    // CABINET ERROR CODE
    CABINET_INVALID_ID(HttpStatus.BAD_REQUEST, "잘못된 사물함 ID입니다"),
    CABINET_INVALID_STUDENT_NUMBER(HttpStatus.BAD_REQUEST, "잘못된 학번입니다"),
    CABINET_HISTORY_CREATION_FAILED(HttpStatus.BAD_REQUEST, "사물함 대여 기록 생성에 실패했습니다"),
    CABINET_HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "사물함 대여 기록을 찾을 수 없습니다"),
    CABINET_HISTORY_SEARCH_FAILED(HttpStatus.BAD_REQUEST, "사물함 대여 기록 조회에 실패했습니다"),

    CABINET_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사물함의 정보를 찾을 수 없습니다"),

    CABINET_NOT_USING(HttpStatus.CONFLICT, "사용중이지 않은 사물함입니다"),
    CABINET_ALREADY_USING(HttpStatus.CONFLICT, "이미 대여중인 사물함입니다"),

    CABINET_RENT_FAILED(HttpStatus.CONFLICT, "사물함 대여에 실패했습니다"),
    CABINET_RETURN_FAILED(HttpStatus.CONFLICT, "사물함 반납에 실패했습니다"),
    CABINET_HISTORY_UPDATE_FAILED(HttpStatus.CONFLICT, "사물함 대여 기록 업데이트에 실패했습니다"),

    CABINET_NOT_ALLOW_RENT(HttpStatus.BAD_REQUEST, "아직 대여 가능 시간이 아닙니다. 반납일 다음날 13시 이후에 대여할 수 있습니다."),

    CABINET_STATUS_MULTI_UPDATE_FAILED(HttpStatus.BAD_REQUEST, "AVAILABLE을 제외한 상태 변경은 한 개의 사물함만 가능합니다."),
    CABINET_STATUS_BROKEN_REASON_UPDATE_FAILED(HttpStatus.BAD_REQUEST, "BROKEN 상태 변경시에는 사유가 필요합니다."),

    //USER
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 유저를 찾을 수 없습니다"),
    USER_INVALID_STUDENT_NUMBER(HttpStatus.BAD_REQUEST, "잘못된 학번입니다."),
    USER_INVALID_VISIBILITY(HttpStatus.BAD_REQUEST, "잘못된 공개 여부 설정입니다."),
    USER_VISIBILITY_UPDATE_FAILED(HttpStatus.BAD_REQUEST, "해당 유저정보를 수정할 수 없습니다."),

    // GENERAL ERROR CODE
    GENERAL_BAD_REQUEST(HttpStatus.BAD_REQUEST, "서버에 잘못된 요청입니다."),
    GENERAL_REQUEST_INVALID_PARAMS(HttpStatus.BAD_REQUEST, "서버에 잘못된 요청입니다."),

    SQL_FILE_LOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"SQL 파일 로딩 실패."),

    GENERAL_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 알 수 없는 오류가 발생했습니다"),
    GENERAL_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서버가 작동하지 않고 있습니다."),
    GENERAL_GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "서버에서 타임아웃이 발생했습니다"),

    // CABINET POSITION
    CABINET_POSITION_NOT_FOUND(HttpStatus.NOT_FOUND,"해당 사물함 위치를 찾을 수 없습니다."),

    // BUILDING
    BUILDING_NOT_FOUND(HttpStatus.BAD_REQUEST,"해당 건물 정보를 조회할 수 없습니다."),
    INVALID_BUILDING_INFO(HttpStatus.BAD_REQUEST,"잘못된 건물 조회 정보입니다."),

    // Cabinet Bookmark
    BOOKMARK_ALREADY_EXIST(HttpStatus.BAD_REQUEST,"이미 즐겨찾기에 등록이 되어있습니다."),
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND,"해당 즐겨찾기 정보가 존재하지 않습니다.");

    // 필드 선언은 열거형 상수 뒤에 위치
    private final int statusCode;
    private final String message;
    private final String error;

    // 생성자도 열거형 상수 뒤에 위치
    ExceptionStatus(HttpStatus status, String message) {
        this.statusCode = status.value();
        this.message = message;
        this.error = status.getReasonPhrase();
    }
}

