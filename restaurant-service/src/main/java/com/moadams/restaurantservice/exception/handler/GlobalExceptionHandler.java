package com.moadams.restaurantservice.exception.handler;

import com.moadams.restaurantservice.dto.CustomApiResponse;
import com.moadams.restaurantservice.exception.ResourceNotFoundException;
import com.moadams.restaurantservice.exception.UnauthorizedAccessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles ResourceNotFoundException.
     * Returns HTTP 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<CustomApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        log.warn("ResourceNotFoundException caught: {}", ex.getMessage());
        CustomApiResponse<Void> response = new CustomApiResponse<>(
                false,
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles UnauthorizedAccessException.
     * Returns HTTP 403 Forbidden.
     */
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<CustomApiResponse<Void>> handleUnauthorizedAccessException(
            UnauthorizedAccessException ex, WebRequest request) {
        log.warn("UnauthorizedAccessException caught: {}", ex.getMessage());
        CustomApiResponse<Void> response = new CustomApiResponse<>(
                false,
                ex.getMessage(),
                HttpStatus.FORBIDDEN.value(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles MethodArgumentNotValidException (validation errors from @Valid).
     * Returns HTTP 400 Bad Request with details of validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("MethodArgumentNotValidException caught: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        CustomApiResponse<Map<String, String>> response = new CustomApiResponse<>(
                false,
                "Validation failed: Please check your input.",
                HttpStatus.BAD_REQUEST.value(),
                errors
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles all other unhandled exceptions.
     * Returns HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomApiResponse<Void>> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        CustomApiResponse<Void> response = new CustomApiResponse<>(
                false,
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}