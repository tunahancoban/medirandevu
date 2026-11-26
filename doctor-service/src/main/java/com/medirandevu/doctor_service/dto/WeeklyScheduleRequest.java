package com.medirandevu.doctor_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Full weekly schedule; replaces the existing one. Days left out mean the doctor doesn't work that day. */
public record WeeklyScheduleRequest(
        @NotNull @Size(max = 7) List<@Valid ScheduleRequest> days) {}
