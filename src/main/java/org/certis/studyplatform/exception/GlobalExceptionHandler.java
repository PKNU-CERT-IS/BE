package org.certis.studyplatform.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.response.GlobalResponseHandler;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * Global Exception Handler
 *
 * Clean Architecture 계층별 예외를 적절한 HTTP 응답으로 변환
 * GlobalResponseHandler를 사용한 일관된 응답 형식 제공
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // =================================================================
    // PRESENTATION LAYER EXCEPTIONS
    // =================================================================

    /**
     * Presentation Layer 커스텀 예외 처리
     */
    @ExceptionHandler(PresentationException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handlePresentationException(
            PresentationException ex, WebRequest request) {
        log.warn("Presentation layer exception: {}", ex.getMessage());

        return GlobalResponseHandler.error(
                ex.getStatus().getStatusCode(),
                ex.getMessage(),
                createErrorDetails("PRESENTATION_ERROR", request)
        );
    }

    /**
     * Bean Validation 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Bean validation failed: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> errorDetails = createErrorDetails("VALIDATION_ERROR", request);
        errorDetails.put("fieldErrors", fieldErrors);

        return GlobalResponseHandler.error(
                HttpStatus.BAD_REQUEST.value(),
                "입력 데이터 검증에 실패했습니다",
                errorDetails
        );
    }

    /**
     * DtoException 처리
     */
    @ExceptionHandler(DtoException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleDtoException(
            DtoException ex, WebRequest request) {
        log.warn("DTO validation exception: {}", ex.getMessage());

        return GlobalResponseHandler.error(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                createErrorDetails("DTO_VALIDATION_ERROR", request)
        );
    }

    /**
     * 제약 조건 위반 처리
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        log.warn("Constraint validation failed: {}", ex.getMessage());

        String violations = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        return GlobalResponseHandler.error(
                HttpStatus.BAD_REQUEST.value(),
                "제약 조건 위반: " + violations,
                createErrorDetails("CONSTRAINT_VIOLATION", request)
        );
    }

    /**
     * 요청 본문이 누락되거나 읽을 수 없는 경우 처리
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("HTTP message not readable: {}", ex.getMessage());

        // Enum 타입 변환 실패(예: AttachedType) 케이스를 구체적으로 처리
        Throwable cause = ex.getCause();
        if (cause instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException ife) {
            Class<?> targetType = ife.getTargetType();
            if (targetType != null && targetType.isEnum()) {
                String invalidValue = String.valueOf(ife.getValue());
                String[] allowed = java.util.Arrays.stream(targetType.getEnumConstants())
                        .map(Object::toString)
                        .toArray(String[]::new);

                String enumName = targetType.getSimpleName();
                String message = String.format("'%s' 값 '%s'은(는) 유효하지 않습니다. 허용값: %s",
                        enumName, invalidValue, String.join(", ", allowed));

                return GlobalResponseHandler.error(
                        HttpStatus.BAD_REQUEST.value(),
                        message,
                        createErrorDetails("ENUM_VALUE_INVALID", request)
                );
            }
        }

        String message = "요청 본문이 누락되었거나 올바르지 않습니다";

        // 구체적인 에러 메시지 제공
        if (ex.getMessage() != null && ex.getMessage().contains("Required request body is missing")) {
            message = "요청 본문이 필요합니다";
        } else if (ex.getMessage() != null && ex.getMessage().contains("JSON parse error")) {
            message = "JSON 형식이 올바르지 않습니다";
        }

        return GlobalResponseHandler.error(
                HttpStatus.BAD_REQUEST.value(),
                message,
                createErrorDetails("INVALID_REQUEST_BODY", request)
        );
    }

    /**
     * 요청 파라미터가 누락된 경우 처리
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleMissingParams(
            MissingServletRequestParameterException ex, WebRequest request) {
        log.warn("Missing request parameter: {}", ex.getMessage());

        String message = String.format("필수 파라미터가 누락되었습니다: %s", ex.getParameterName());

        return GlobalResponseHandler.error(
                HttpStatus.BAD_REQUEST.value(),
                message,
                createErrorDetails("MISSING_PARAMETER", request)
        );
    }

    /**
     * 미디어 타입이 지원되지 않는 경우 처리
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, WebRequest request) {
        log.warn("Media type not supported: {}", ex.getMessage());

        String message = "지원하지 않는 미디어 타입입니다";

        return GlobalResponseHandler.error(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                message,
                createErrorDetails("UNSUPPORTED_MEDIA_TYPE", request)
        );
    }

    /**
     * 타입 변환 오류 처리 (일반적인 타입 변환)
     */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleTypeMismatch(
            TypeMismatchException ex, WebRequest request) {
        log.warn("Type mismatch: {}", ex.getMessage());

        String message = String.format("타입 변환 오류: %s", ex.getPropertyName());

        return GlobalResponseHandler.error(
                HttpStatus.BAD_REQUEST.value(),
                message,
                createErrorDetails("TYPE_MISMATCH", request)
        );
    }

    /**
     * 경로 변수 및 메서드 파라미터 타입 변환 오류 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.warn("Method argument type mismatch: {}", ex.getMessage());

        String requiredTypeName = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "required type";
        String message = String.format("'%s' 파라미터의 값 '%s'을(를) %s 타입으로 변환할 수 없습니다",
                ex.getName(), ex.getValue(), requiredTypeName);

        return GlobalResponseHandler.error(
                HttpStatus.BAD_REQUEST.value(),
                message,
                createErrorDetails("METHOD_ARGUMENT_TYPE_MISMATCH", request)
        );
    }

    /**
     * HTTP 메서드 오류 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {
        log.warn("Method not supported: {}", ex.getMessage());

        return GlobalResponseHandler.error(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "지원하지 않는 HTTP 메서드입니다: " + ex.getMethod(),
                createErrorDetails("METHOD_NOT_ALLOWED", request)
        );
    }

    /**
     * 리소스를 찾을 수 없는 경우 처리 (404 Not Found)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleNoResourceFound(
            NoResourceFoundException ex, WebRequest request) {
        log.warn("No resource found: {}", ex.getMessage());

        String message = "요청한 리소스를 찾을 수 없습니다";

        // API 경로인지 확인하여 적절한 메시지 제공
        if (ex.getResourcePath().startsWith("api/")) {
            message = "존재하지 않는 API 엔드포인트입니다";
        }

        return GlobalResponseHandler.error(
                HttpStatus.NOT_FOUND.value(),
                message,
                createErrorDetails("RESOURCE_NOT_FOUND", request)
        );
    }

    // =================================================================
    // APPLICATION LAYER EXCEPTIONS
    // =================================================================

    /**
     * Application Layer 커스텀 예외 처리
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleApplicationException(
            ApplicationException ex, WebRequest request) {
        log.warn("Application layer exception: {}", ex.getMessage());

        return GlobalResponseHandler.error(
                ex.getStatus().getStatusCode(),
                ex.getMessage(),
                createErrorDetails("APPLICATION_ERROR", request)
        );
    }

    // =================================================================
    // DOMAIN LAYER EXCEPTIONS
    // =================================================================

    /**
     * Domain Layer 커스텀 예외 처리
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleDomainException(
            DomainException ex, WebRequest request) {
        log.warn("Domain layer exception: {}", ex.getMessage());

        return GlobalResponseHandler.error(
                ex.getStatus().getStatusCode(),
                ex.getMessage(),
                createErrorDetails("DOMAIN_ERROR", request)
        );
    }

    // =================================================================
    // INFRASTRUCTURE LAYER EXCEPTIONS
    // =================================================================

    /**
     * Infrastructure Layer 커스텀 예외 처리
     */
    @ExceptionHandler(InfrastructureException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleInfrastructureException(
            InfrastructureException ex, WebRequest request) {
        log.error("Infrastructure layer exception: {}", ex.getMessage(), ex);

        return GlobalResponseHandler.error(
                ex.getStatus().getStatusCode(),
                ex.getMessage(),
                createErrorDetails("INFRASTRUCTURE_ERROR", request)
        );
    }

    /**
     * Database 무결성 위반 처리
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, WebRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage(), ex);

        String message = "데이터 무결성 제약 조건 위반입니다";
        String raw = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        // 일반적인 제약 조건 위반 메시지 변환
        if (raw.contains("unique")) {
            message = "중복된 데이터로 인해 처리할 수 없습니다";
        } else if (raw.contains("foreign key")) {
            message = "참조 무결성 제약으로 인해 처리할 수 없습니다";
        } else if (raw.contains("not null")) {
            message = "필수 데이터가 누락되어 처리할 수 없습니다";
        }

        return GlobalResponseHandler.error(
                HttpStatus.CONFLICT.value(),
                message,
                createErrorDetails("DATA_INTEGRITY_VIOLATION", request)
        );
    }

    // =================================================================
    // SYSTEM EXCEPTIONS
    // =================================================================

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());

        return GlobalResponseHandler.error(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                createErrorDetails("INVALID_ARGUMENT", request)
        );
    }

    /**
     * IllegalStateException 처리 (Repository에서 비즈니스 규칙 위반 시 사용)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleIllegalState(
            IllegalStateException ex, WebRequest request) {
        log.warn("Illegal state (business rule violation): {}", ex.getMessage());

        return GlobalResponseHandler.error(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                createErrorDetails("BUSINESS_RULE_VIOLATION", request)
        );
    }

    /**
     * 일반적인 Runtime Exception 처리
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        log.error("Unexpected runtime exception: {}", ex.getMessage(), ex);

        return GlobalResponseHandler.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                createErrorDetails("RUNTIME_ERROR", request)
        );
    }

    /**
     * 트랜잭션 롤백 예외 처리
     * - 내부에서 예외가 발생해 트랜잭션이 rollback-only가 되었는데 외부에서 commit을 시도할 때 발생
     * - 가능한 경우 원인 예외를 언랩해서 보다 적절한 상태코드로 응답
     */
    @ExceptionHandler({UnexpectedRollbackException.class, TransactionSystemException.class})
    public ResponseEntity<GlobalResponseHandler<Object>> handleTransactionRollback(Exception ex, WebRequest request) {
        Throwable cause = ex.getCause();
        // 가능한 경우 원인 예외로 위임 처리
        if (cause != null) {
            return delegateToSpecificHandler(cause, request);
        }

        // 원인 예외가 없는 경우 스택 포함 상세 로그로 추적성 강화
        log.warn("Transaction rolled back without cause. Message={}, path={}, thread={}",
                ex.getMessage(), getPath(request), Thread.currentThread().getName(), ex);
        return GlobalResponseHandler.error(
                HttpStatus.CONFLICT.value(),
                "요청 처리 중 트랜잭션이 롤백되었습니다",
                createErrorDetails("TRANSACTION_ROLLBACK", request)
        );
    }

    /**
     * 모든 예외의 최종 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleAllExceptions(
            Exception ex, WebRequest request) {
        log.error("Unexpected exception: {}", ex.getMessage(), ex);

        return GlobalResponseHandler.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "예상치 못한 오류가 발생했습니다. 관리자에게 문의해주세요.",
                createErrorDetails("UNEXPECTED_ERROR", request)
        );
    }

    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleCompletionException(
            CompletionException ex, WebRequest request) {

        Throwable cause = ex.getCause();
        log.warn("CompletionException caught, unwrapping cause: {}",
                cause != null ? cause.getClass().getSimpleName() : "null");

        // 원본 예외가 있는 경우 해당 예외로 처리 (안전 언랩: CompletionException 연쇄 방지)
        if (cause != null) {
            Throwable root = cause;
            int guard = 0;
            while (root instanceof CompletionException && root.getCause() != null && guard < 10) {
                root = root.getCause();
                guard++;
            }
            return delegateToSpecificHandler(root, request);
        }

        // 원본 예외가 없는 경우 일반 처리
        log.error("CompletionException without cause: {}", ex.getMessage(), ex);
        return GlobalResponseHandler.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "비동기 작업 처리 중 오류가 발생했습니다",
                createErrorDetails("COMPLETION_ERROR", request)
        );
    }

    

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<GlobalResponseHandler<Object>> handleAuthorizationDenied(
            AuthorizationDeniedException ex, WebRequest request) {

        log.warn("Authorization denied for request: {}",  ex.getMessage());

        return GlobalResponseHandler.error(
                HttpStatus.FORBIDDEN.value(),
                "접근 권한이 없습니다",
                createErrorDetails("AUTHORIZATION_ERROR", request)
        );
    }

    // =================================================================
    // HELPER METHODS
    // =================================================================

    /**
     * 에러 상세 정보 생성
     */
    private Map<String, Object> createErrorDetails(String errorType, WebRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("timestamp", OffsetDateTime.now());
        details.put("type", errorType);
        details.put("path", getPath(request));
        return details;
    }

    /**
     * 원본 예외 타입에 따라 적절한 핸들러로 위임
     */
    private ResponseEntity<GlobalResponseHandler<Object>> delegateToSpecificHandler(
            Throwable cause, WebRequest request) {

        // Domain Layer 예외
        if (cause instanceof DomainException domainEx) {
            return GlobalResponseHandler.error(
                    domainEx.getStatus().getStatusCode(),
                    domainEx.getMessage(),
                    createErrorDetails("DOMAIN_ERROR", request)
            );
        }

        // Application Layer 예외
        if (cause instanceof ApplicationException appEx) {
            return GlobalResponseHandler.error(
                    appEx.getStatus().getStatusCode(),
                    appEx.getMessage(),
                    createErrorDetails("APPLICATION_ERROR", request)
            );
        }

        // Infrastructure Layer 예외
        if (cause instanceof InfrastructureException infraEx) {
            log.error("Infrastructure layer exception (async): {}", infraEx.getMessage(), infraEx);
            return GlobalResponseHandler.error(
                    infraEx.getStatus().getStatusCode(),
                    infraEx.getMessage(),
                    createErrorDetails("INFRASTRUCTURE_ERROR", request)
            );
        }

        // Presentation Layer 예외
        if (cause instanceof PresentationException presEx) {
            return GlobalResponseHandler.error(
                    presEx.getStatus().getStatusCode(),
                    presEx.getMessage(),
                    createErrorDetails("PRESENTATION_ERROR", request)
            );
        }

        // DTO 예외
        if (cause instanceof DtoException dtoEx) {
            return GlobalResponseHandler.error(
                    HttpStatus.BAD_REQUEST.value(),
                    dtoEx.getMessage(),
                    createErrorDetails("DTO_VALIDATION_ERROR", request)
            );
        }

        // 데이터 무결성 위반
        if (cause instanceof DataIntegrityViolationException dataEx) {
            String message = "데이터 무결성 제약 조건 위반입니다";
            String msg = dataEx.getMessage() != null ? dataEx.getMessage().toLowerCase() : "";
            if (msg.contains("unique")) {
                message = "중복된 데이터로 인해 처리할 수 없습니다";
            } else if (msg.contains("foreign key")) {
                message = "참조 무결성 제약으로 인해 처리할 수 없습니다";
            } else if (msg.contains("not null")) {
                message = "필수 데이터가 누락되어 처리할 수 없습니다";
            }
            return GlobalResponseHandler.error(
                    HttpStatus.CONFLICT.value(),
                    message,
                    createErrorDetails("DATA_INTEGRITY_VIOLATION", request)
            );
        }

        // 제약 조건 위반
        if (cause instanceof ConstraintViolationException constraintEx) {
            String violations = constraintEx.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(java.util.stream.Collectors.joining(", "));
            return GlobalResponseHandler.error(
                    HttpStatus.BAD_REQUEST.value(),
                    "제약 조건 위반: " + violations,
                    createErrorDetails("CONSTRAINT_VIOLATION", request)
            );
        }

        // Validation 예외
        if (cause instanceof MethodArgumentNotValidException validationEx) {
            java.util.Map<String, String> fieldErrors = new java.util.HashMap<>();
            validationEx.getBindingResult().getFieldErrors().forEach(error ->
                    fieldErrors.put(error.getField(), error.getDefaultMessage())
            );
            java.util.Map<String, Object> details = createErrorDetails("VALIDATION_ERROR", request);
            details.put("fieldErrors", fieldErrors);
            return GlobalResponseHandler.error(
                    HttpStatus.BAD_REQUEST.value(),
                    "입력 데이터 검증에 실패했습니다",
                    details
            );
        }

        // IllegalArgument 예외
        if (cause instanceof IllegalArgumentException illegalArgEx) {
            return GlobalResponseHandler.error(
                    HttpStatus.BAD_REQUEST.value(),
                    illegalArgEx.getMessage(),
                    createErrorDetails("INVALID_ARGUMENT", request)
            );
        }

        // IllegalState 예외
        if (cause instanceof IllegalStateException illegalStateEx) {
            return GlobalResponseHandler.error(
                    HttpStatus.CONFLICT.value(),
                    illegalStateEx.getMessage(),
                    createErrorDetails("BUSINESS_RULE_VIOLATION", request)
            );
        }

        // 그 외의 RuntimeException
        if (cause instanceof RuntimeException runtimeEx) {
            log.error("Unexpected runtime exception (async): {}", runtimeEx.getMessage(), runtimeEx);
            return GlobalResponseHandler.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                    createErrorDetails("RUNTIME_ERROR", request)
            );
        }

        // 일반 Exception
        log.error("Unhandled exception type in async context: {}", cause.getClass().getSimpleName(), cause);
        return GlobalResponseHandler.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "비동기 작업에서 예상치 못한 오류가 발생했습니다",
                createErrorDetails("ASYNC_UNEXPECTED_ERROR", request)
        );
    }

    /**
     * 요청 경로 추출
     */
    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}