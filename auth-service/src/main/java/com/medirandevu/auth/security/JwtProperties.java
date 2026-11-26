package com.medirandevu.auth.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds the {@code jwt.*} entries in application.properties.
 * {@code @Validated} makes the application fail on startup if a required value is missing.
 *
 * @param secret       Base64-encoded HMAC key, at least 32 bytes once decoded (override with JWT_SECRET)
 * @param expirationMs token lifetime in milliseconds; also used as the cookie's Max-Age
 * @param cookieName   name of the cookie that carries the token
 * @param cookieSecure whether the cookie gets the Secure flag (HTTPS only); true in production
 */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(@NotBlank String secret, @Positive long expirationMs, @NotBlank String cookieName, boolean cookieSecure) {

}
