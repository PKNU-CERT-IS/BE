package org.certis.studyplatform.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Global exception handler to manage all exceptions across the application.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle ControllerException
     */
    @ExceptionHandler(ControllerException.class)
    public ResponseEntity<CustomExceptionStatus> handleApiException(ControllerException ex, WebRequest request) {
        log.error("ApiException: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getStatus(), request);
    }

    /**
     * Handle ServiceException
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<CustomExceptionStatus> handleApplicationException(ServiceException ex, WebRequest request) {
        log.error("ApplicationException: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getStatus(), request);
    }

    /**
     * Handle VoException
     */
    @ExceptionHandler(VoException.class)
    public ResponseEntity<CustomExceptionStatus> handleDaoException(VoException ex, WebRequest request) {
        log.error("DaoException: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getStatus(), request);
    }

    /**
     * Handle DtoException
     */
    @ExceptionHandler(DtoException.class)
    public ResponseEntity<CustomExceptionStatus> handleDtoException(DtoException ex, WebRequest request) {
        log.error("DtoException: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getStatus(), request);
    }

    /**
     * Handle EntityException
     */
    @ExceptionHandler(EntityException.class)
    public ResponseEntity<CustomExceptionStatus> handleDomainException(EntityException ex, WebRequest request) {
        log.error("DomainException: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getStatus(), request);
    }

    /**
     * Handle RepositoryException
     */
    @ExceptionHandler(RepositoryException.class)
    public ResponseEntity<CustomExceptionStatus> handleRepositoryException(RepositoryException ex, WebRequest request) {
        log.error("RepositoryException: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getStatus(), request);
    }

    /**
     * Handle UtilException
     */
    @ExceptionHandler(UtilException.class)
    public ResponseEntity<CustomExceptionStatus> handleUtilException(UtilException ex, WebRequest request) {
        log.error("UtilException: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getStatus(), request);
    }

    /**
     * Handle MiddlewareException
     */
    @ExceptionHandler(MiddlewareException.class)
    public ResponseEntity<CustomExceptionStatus> handleMiddlewareException(MiddlewareException ex, WebRequest request) {
        log.error("MiddlewareException: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getStatus(), request);
    }

    /**
     * Handle AsyncException for asynchronous processing errors
     */
    @ExceptionHandler(AsyncException.class)
    public ResponseEntity<CustomExceptionStatus> handleAsyncException(AsyncException ex, WebRequest request) {
        log.error("AsyncException: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getStatus(), request);
    }

    /**
     * Handle MethodArgumentNotValidException (DTO 검증 실패)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomExceptionStatus> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request) {
        log.error("MethodArgumentNotValidException: {}", ex.getMessage(), ex);

        // 필드별 오류 메시지를 "field: message" 형태로 결합
        String fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        // CustomExceptionStatus 객체 생성
        CustomExceptionStatus errorResponse = new CustomExceptionStatus(
                ExceptionStatus.GENERAL_REQUEST_INVALID_PARAMS,
                fieldErrors
        );

        return ResponseEntity.status(ExceptionStatus.GENERAL_REQUEST_INVALID_PARAMS.getStatusCode()).body(errorResponse);
    }

    /**
     * Handle ConstraintViolationException (경로 변수, 요청 파라미터 검증 실패)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CustomExceptionStatus> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        log.error("ConstraintViolationException: {}", ex.getMessage(), ex);

        String violations = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        CustomExceptionStatus errorResponse = new CustomExceptionStatus(
                ExceptionStatus.GENERAL_REQUEST_INVALID_PARAMS,
                violations
        );

        return ResponseEntity.status(ExceptionStatus.GENERAL_REQUEST_INVALID_PARAMS.getStatusCode()).body(errorResponse);
    }

    /**
     * Handle MethodArgumentTypeMismatchException (예: 잘못된 Enum 값)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CustomExceptionStatus> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.error("MethodArgumentTypeMismatchException: {}", ex.getMessage(), ex);

        String message = ExceptionStatus.GENERAL_REQUEST_INVALID_PARAMS.getMessage();

        CustomExceptionStatus errorResponse = new CustomExceptionStatus(
                ExceptionStatus.GENERAL_REQUEST_INVALID_PARAMS,
                message
        );

        return ResponseEntity.status(ExceptionStatus.GENERAL_REQUEST_INVALID_PARAMS.getStatusCode()).body(errorResponse);
    }

    /**
     * Handle Generic Exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomExceptionStatus> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled Exception: {}", ex.getMessage(), ex);
        CustomExceptionStatus errorResponse = new CustomExceptionStatus(
                ExceptionStatus.GENERAL_INTERNAL_SERVER_ERROR,
                "서버에서 알 수 없는 오류가 발생했습니다."
        );
        return ResponseEntity.status(ExceptionStatus.GENERAL_INTERNAL_SERVER_ERROR.getStatusCode()).body(errorResponse);
    }

    /**
     * Build the error response based on ExceptionStatus
     */
    private ResponseEntity<CustomExceptionStatus> buildErrorResponse(ExceptionStatus errorCode, WebRequest request) {
        CustomExceptionStatus errorResponse = new CustomExceptionStatus(
                errorCode,
                errorCode.getMessage()
        );
        return ResponseEntity.status(errorCode.getStatusCode()).body(errorResponse);
    }
}
