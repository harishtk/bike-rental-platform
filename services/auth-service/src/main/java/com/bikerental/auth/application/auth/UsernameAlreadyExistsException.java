package com.bikerental.auth.application.auth;

public class UsernameAlreadyExistsException
        extends RuntimeException {

    public UsernameAlreadyExistsException(
            String username
    ) {
        super(
                "Username already exists: "
                        + username
        );
    }
}