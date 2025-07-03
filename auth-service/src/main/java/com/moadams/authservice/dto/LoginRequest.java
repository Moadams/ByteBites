package com.moadams.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Email(message = "Enter a valid email address")
        @NotBlank(message = "Email must not be blank")
        String email,

        @NotBlank(message = "Password cannot be blank")
        String password
) {
}
