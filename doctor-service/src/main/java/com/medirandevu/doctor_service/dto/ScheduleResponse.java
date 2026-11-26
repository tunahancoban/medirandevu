package com.medirandevu.doctor_service.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Working hours of a single weekday. */
public record ScheduleResponse(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, Integer slotMinutes) {}
