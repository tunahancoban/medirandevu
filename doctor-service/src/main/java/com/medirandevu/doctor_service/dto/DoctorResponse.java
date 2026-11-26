package com.medirandevu.doctor_service.dto;

public record DoctorResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String title,
        Long departmentId,
        String departmentName) {}