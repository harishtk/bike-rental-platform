package com.bikerental.bike.api.common;

public record ApiResponse<T>(
        int status,
        String message,
        T data
) {

    public static <T> ApiResponse<T> success(
            int status,
            String message,
            T data
    ) {
        return new ApiResponse<>(
                status,
                message,
                data
        );
    }

    public static <T> ApiResponse<T> error(
            int status,
            String message,
            T data
    ) {
        return new ApiResponse<>(
                status,
                message,
                data
        );
    }
}
