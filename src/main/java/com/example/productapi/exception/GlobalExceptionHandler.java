package com.example.productapi.exception;

import com.example.productapi.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(ProductNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), HttpHeaders.EMPTY);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .sorted().collect(Collectors.joining("; "));
        return error(status, message, headers);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return error(status, "Request body must contain valid JSON with compatible field values", headers);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        String message = knownStatus == null ? "Request could not be processed" : knownStatus.getReasonPhrase();
        return error(status, message, headers);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception exception) {
        log.error("Unexpected request failure", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", HttpHeaders.EMPTY);
    }

    private ResponseEntity<Object> error(HttpStatusCode status, String message, HttpHeaders headers) {
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        String reason = knownStatus == null ? "Error" : knownStatus.getReasonPhrase();
        return new ResponseEntity<>(new ApiError(LocalDateTime.now(), status.value(), reason, message),
                headers, status);
    }
}
