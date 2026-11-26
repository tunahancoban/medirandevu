package com.medirandevu.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    public enum Role { PATIENT, DOCTOR, ADMIN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // Stored as text ("PATIENT"), not ordinal, so reordering the enum can't corrupt data.
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private Role role;

    // Disabled users cannot log in. @Builder.Default keeps the default when using User.builder().
    @Builder.Default
    private boolean enabled = true;

    // Filled by the database (DEFAULT NOW() in V1__create_users.sql); JPA never writes it.
    @Column(name = "created_at", insertable = false , updatable = false)
    private LocalDateTime createdAt;
}
