package com.medirandevu.auth.config;

import com.medirandevu.auth.entity.User;
import com.medirandevu.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the first ADMIN account on startup, because public registration can only create
 * patients and doctors can only be created by an admin.
 *
 * <p>Runs once the application context is ready (and Flyway has migrated the schema).
 * It does nothing if any admin already exists, so restarting the app or changing
 * ADMIN_EMAIL / ADMIN_PASSWORD later doesn't create extra admins or reset the password.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(User.Role.ADMIN)) {
            log.info("Admin account is exist");
            return;
        }

        User admin = User.builder()
                .email(adminProperties.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(adminProperties.password()))
                .role(User.Role.ADMIN)
                .build();

        userRepository.save(admin);
        log.info("First admin account is created: {}", admin.getEmail());
    }
}