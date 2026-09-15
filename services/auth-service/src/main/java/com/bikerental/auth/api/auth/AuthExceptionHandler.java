package com.bikerental.auth.api.auth;

import com.bikerental.auth.application.auth.InvalidCredentialsException;
import com.bikerental.auth.application.auth.UsernameAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(
            UsernameAlreadyExistsException.class
    )
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> usernameExists(
            UsernameAlreadyExistsException exception
    ) {
        return Map.of(
                "message",
                exception.getMessage()
        );
    }

    @ExceptionHandler(
            InvalidCredentialsException.class
    )
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> invalidCredentials(
            InvalidCredentialsException exception
    ) {
        return Map.of(
                "message",
                exception.getMessage()
        );
    }
}