package com.moadams.restaurantservice.dto;

public record CustomApiResponse<T>(
        boolean success,
        String message,
        int statusCode,
        T data
) {
}