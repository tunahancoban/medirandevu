package com.medirandevu.doctor_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "departments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;
    // Soft-delete flag: inactive departments are hidden from listings and can't get new doctors
    @Builder.Default
    @Column(nullable = false)
    private boolean active=true;

}
