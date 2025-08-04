package org.certis.studyplatform.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@RequiredArgsConstructor
@Getter
public class CustomExceptionStatus {

    private final int statusCode;
    private final String message;
    private final String error;

    public CustomExceptionStatus(HttpStatus status, String message) {
        this.statusCode = status.value();
        this.message = message;
        this.error = status.getReasonPhrase();
    }

    public CustomExceptionStatus(ExceptionStatus status, String message) {
        this.statusCode = status.getStatusCode();
        this.message = message;
        this.error = status.getMessage();
    }
}