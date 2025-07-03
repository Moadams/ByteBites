package com.moadams.authservice.controller;

import com.moadams.authservice.dto.AuthResponse; // Your JwtResponse structure
import com.moadams.authservice.dto.CustomApiResponse;
import com.moadams.authservice.dto.LoginRequest; // Using LoginRequest as discussed
import com.moadams.authservice.dto.UserRegistrationRequest;
import com.moadams.authservice.model.User;
import com.moadams.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Handles user registration requests.
     * Exceptions (like IllegalArgumentException, validation errors) are handled by GlobalExceptionHandler.
     * @param request UserRegistrationRequest DTO containing email and password.
     * @return ResponseEntity with CustomApiResponse indicating success.
     */
    @PostMapping("/register")
    public ResponseEntity<CustomApiResponse<Void>> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        User registeredUser = authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomApiResponse.success("User registered successfully with email: " + registeredUser.getEmail()));
    }

    /**
     * Handles user login requests.
     * Exceptions (like BadCredentialsException, validation errors) are handled by GlobalExceptionHandler.
     * @param request LoginRequest DTO containing email and password.
     * @return ResponseEntity with CustomApiResponse containing JWT token on success.
     */
    @PostMapping("/login")
    public ResponseEntity<CustomApiResponse<AuthResponse>> loginUser(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.loginUser(request);
        return ResponseEntity.ok(CustomApiResponse.success("Login successful", authResponse));
    }
}