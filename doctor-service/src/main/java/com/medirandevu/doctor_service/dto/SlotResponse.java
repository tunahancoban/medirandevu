package com.medirandevu.doctor_service.dto;

import java.time.LocalDateTime;

public record SlotResponse(LocalDateTime start, LocalDateTime end) {}