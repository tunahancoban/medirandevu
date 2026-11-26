package com.medirandevu.doctor_service.controller;

import com.medirandevu.doctor_service.dto.CreateDoctorRequest;
import com.medirandevu.doctor_service.dto.DoctorResponse;
import com.medirandevu.doctor_service.dto.ScheduleResponse;
import com.medirandevu.doctor_service.dto.SlotResponse;
import com.medirandevu.doctor_service.dto.WeeklyScheduleRequest;
import com.medirandevu.doctor_service.service.interfaces.DoctorService;
import com.medirandevu.doctor_service.service.interfaces.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final ScheduleService scheduleService;

    // Public: active doctors, optionally filtered by department
    @GetMapping
    public ResponseEntity<List<DoctorResponse>> getDoctors(
            @RequestParam(required = false) Long departmentId) {
        return ResponseEntity.ok(doctorService.getAll(departmentId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponse> getDoctorById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getById(id));
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<List<ScheduleResponse>> getSchedule(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getSchedule(id));
    }

    // Public: free slots for one day, e.g. /api/doctors/1/slots?date=2026-11-27
    @GetMapping("/{id}/slots")
    public ResponseEntity<List<SlotResponse>> getSlots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(scheduleService.getSlots(id, date));
    }


    // Admin links an existing auth-service user to a new doctor profile
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.create(request));
    }


    // "/me" endpoints act on the doctor profile of the logged-in user
    @GetMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorResponse> getMe(Authentication authentication) {
        return ResponseEntity.ok(doctorService.getByUserId(currentUserId(authentication)));
    }

    @PutMapping("/me/schedule")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<ScheduleResponse>> updateMySchedule(
            Authentication authentication,
            @Valid @RequestBody WeeklyScheduleRequest request) {
        return ResponseEntity.ok(
                scheduleService.updateMySchedule(currentUserId(authentication), request));
    }

    // The JWT subject (authentication name) holds the auth-service user id
    private Long currentUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}