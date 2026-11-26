package com.medirandevu.auth.exception;

public class EmailAlreadyExistsException extends RuntimeException{
    public EmailAlreadyExistsException(String email){
        super("This email is already taken: " +email);
    }
}
