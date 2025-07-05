package com.moadams.authservice.exception;

import com.moadams.authservice.dto.CustomApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles IllegalArgumentException, typically for business rule violations (e.g., user already exists).
     * Returns HTTP 409 Conflict.
     * @param ex The IllegalArgumentException.
     * @return A ResponseEntity containing a CustomApiResponse with error details.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CustomApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(CustomApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles BadCredentialsException, specifically for failed login attempts.
     * Returns HTTP 401 Unauthorized.
     * @param ex The BadCredentialsException.
     * @return A ResponseEntity containing a CustomApiResponse with error details.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<CustomApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CustomApiResponse.error("Invalid email or password."));
    }

    /**
     * Handles MethodArgumentNotValidException, which occurs when @Valid fails on DTOs.
     * Extracts all validation errors and returns them.
     * Returns HTTP 400 Bad Request.
     * @param ex The MethodArgumentNotValidException.
     * @return A ResponseEntity containing a CustomApiResponse with validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CustomApiResponse.error("Validation failed", errors));
    }


    /**
     * Catches any other unhandled exceptions.
     * Returns HTTP 500 Internal Server Error.
     * @param ex The generic Exception.
     * @return A ResponseEntity containing a CustomApiResponse with generic error message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomApiResponse<Void>> handleGenericException(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CustomApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}