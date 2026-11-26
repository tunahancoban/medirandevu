package com.medirandevu.doctor_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Weekly working hours of a doctor; at most one row per doctor and day of week. */
@Entity
@Table(name = "schedules", uniqueConstraints = @UniqueConstraint(name="uq_schedule_doctor_day", columnNames = {"doctor_id", "day_of_week"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Schedule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(name= "day_of_week", nullable=false, length =10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    // Length of one appointment slot
    @Column(name = "slot_minutes", nullable = false)
    private Integer slotMinutes;
}
