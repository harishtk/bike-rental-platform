package com.bikerental.bike.api.common;

import com.bikerental.bike.application.station.StationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StationNotFoundException.class)
    public ApiResponse<Void> handleStationNotFound(
            StationNotFoundException exception
    ) {
        return ApiResponse.success(
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage(),
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<List<ValidationError>> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        List<ValidationError> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ValidationError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        return ApiResponse.success(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors
        );
    }

    public record ValidationError(
            String field,
            String message
    ) {

    }
}
