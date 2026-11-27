package com.jing.common.exception;


import com.jing.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBase(BaseException ex, HttpServletRequest req){
        return build(ex.getStatus(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex,
                                                             HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "Missing required header: " + ex.getHeaderName(), req, null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                                              HttpServletRequest req) {
        return build(HttpStatus.CONFLICT,
                "This record was modified by someone else, please reload and retry", req, null);
    }

    // @Valid on request body
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest req) {
        List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorResponse.FieldError(e.getField(), e.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, errors);
    }

    // @Validated on path/query params
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstrainViolation(ConstraintViolationException ex, HttpServletRequest req){
        List<ErrorResponse.FieldError> errors = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldError(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, errors);
    }
    // Malformed JSON
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest req) {
        log.warn("Unreadable request on {} {}: {}", req.getMethod(), req.getRequestURI(),
                ex.getMostSpecificCause().getMessage());

        String message = "Malformed JSON request";
        if (ex.getCause() instanceof InvalidFormatException ife && ife.getTargetType().isEnum()) {
            String field = ife.getPath().isEmpty() ? "value"
                    : ife.getPath().get(ife.getPath().size() - 1).getPropertyName();
            message = "Invalid value '%s' for field '%s'. Allowed: %s".formatted(
                    ife.getValue(), field,
                    Arrays.toString(ife.getTargetType().getEnumConstants()));
        }
        return build(HttpStatus.BAD_REQUEST, message, req, null);
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest req) {
        String message = "Invalid value '%s' for parameter '%s'".formatted(ex.getValue(), ex.getName());
        return build(HttpStatus.BAD_REQUEST, message, req, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), req, null);
    }

    // Required ?param= missing
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                            HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "Missing required parameter: " + ex.getParameterName(), req, null);
    }

    // Required multipart part missing (e.g. "file")
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex,
                                                           HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "Missing required part: " + ex.getRequestPartName(), req, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex,
                                                         HttpServletRequest req) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), req, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadSize(MaxUploadSizeExceededException ex,
                                                          HttpServletRequest req) {
        return build(HttpStatus.CONTENT_TOO_LARGE, "Uploaded file is too large", req, null);
    }

    // Unknown URL: Spring throws this instead of returning a plain 404
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex,
                                                          HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "No endpoint " + req.getMethod() + " " + req.getRequestURI(), req, null);
    }

    // Duplicate key, FK violation, etc.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest req) {
        // the real reason goes to the log, not to the client
        log.error("Data integrity violation on {} {}: {}",
                req.getMethod(), req.getRequestURI(),
                ex.getMostSpecificCause().getMessage(), ex);
        return build(HttpStatus.CONFLICT, "Data integrity violation", req, null);
    }

    // Fallback: never leak internals to the client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", req, null);
    }



    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                HttpServletRequest req, List<ErrorResponse.FieldError> fieldErrors) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                req.getRequestURI(),
                fieldErrors);
        return ResponseEntity.status(status).body(body);
    }

}
