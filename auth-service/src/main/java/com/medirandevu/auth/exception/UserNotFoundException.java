package com.medirandevu.auth.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("This user not exist: " +id);
    }
}
