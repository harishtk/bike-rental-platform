package com.bikerental.reservation.api.common;

public record ErrorResponse<T>(
        int status,
        String code,
        String message,
        T data
) {

    public static <T> ErrorResponse<T> create(
            int status,
            String code,
            String message,
            T data
    ) {
        return new ErrorResponse<>(
                status,
                code,
                message,
                data
        );
    }
}
