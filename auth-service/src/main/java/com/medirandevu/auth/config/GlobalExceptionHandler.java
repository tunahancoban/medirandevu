package com.medirandevu.auth.config;

import com.medirandevu.auth.exception.EmailAlreadyExistsException;
import com.medirandevu.auth.exception.InvalidCredentialsException;
import com.medirandevu.auth.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.security.access.AccessDeniedException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns exceptions thrown from controllers into RFC 9457 {@link ProblemDetail} JSON responses.
 * Errors raised inside the security filter chain (e.g. the 401 entry point) never reach this class.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 409 – email is already registered (checked before saving)
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailExists(EmailAlreadyExistsException ex){
        return problem(HttpStatus.CONFLICT, "Email already exists", ex.getMessage());
    }

    // 409 – DB UNIQUE constraint hit, e.g. two concurrent registrations with the same email
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Conflict", "Kayıt mevcut verilerle çakışıyor");
    }

    // 401 – wrong email or password (deliberately not saying which)
    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid credentials", ex.getMessage());
    }

    // 403 – authenticated but missing the required role (@PreAuthorize)
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", "Bu işlem için yetkiniz yok");
    }

    // 404 – user not found
    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "User not found", ex.getMessage());
    }

    // 400 – @Valid constraints violated; returns the first message per field under "errors"
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e ->
                errors.putIfAbsent(e.getField(), e.getDefaultMessage()));

        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validation failed",
                "İstek alanlarından bazıları geçersiz");
        pd.setProperty("errors", errors);
        return pd;
    }

    // 400 – malformed JSON or missing body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed request", "İstek gövdesi okunamadı");
    }

    // 500 – anything else; full details go to the log, not to the client
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
                "Beklenmeyen bir hata oluştu");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        return pd;
    }
}
