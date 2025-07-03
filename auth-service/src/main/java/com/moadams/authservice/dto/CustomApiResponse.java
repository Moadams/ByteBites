package com.moadams.authservice.dto;

import java.time.LocalDateTime;

public record CustomApiResponse<T>(
        boolean success,
        String message,
        T data,
        LocalDateTime timestamp
) {
    public static <T> CustomApiResponse<T> success(T data) {
        return new CustomApiResponse<>(true, "Success", data, LocalDateTime.now());
    }

    public static <T> CustomApiResponse<T> success(String message, T data) {
        return new CustomApiResponse<>(true, message, data, LocalDateTime.now());
    }

    public static CustomApiResponse<Void> success(String message) {
        return new CustomApiResponse<>(true, message, null, LocalDateTime.now());
    }


    public static <T> CustomApiResponse<T> error(String message) {
        return new CustomApiResponse<>(false, message, null, LocalDateTime.now());
    }

    public static <T> CustomApiResponse<T> error(String message, T data) {
        return new CustomApiResponse<>(false, message, data, LocalDateTime.now());
    }
}