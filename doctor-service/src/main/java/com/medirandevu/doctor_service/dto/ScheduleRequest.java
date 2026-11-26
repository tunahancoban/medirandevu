package com.medirandevu.doctor_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleRequest(@NotNull DayOfWeek dayOfWeek,
                              @NotNull LocalTime startTime,
                              @NotNull LocalTime endTime,
                              @NotNull @Min(5) @Max(120) Integer slotMinutes) { }
