package com.medirandevu.auth.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Credentials for the initial admin account ({@code app.admin.*}), read by AdminInitializer.
 * Override the defaults with ADMIN_EMAIL / ADMIN_PASSWORD outside local development.
 */
@Validated
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        @NotBlank @Email String email,
        @NotBlank String password) {}
