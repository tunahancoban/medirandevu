package com.medirandevu.doctor_service.service.interfaces;

import com.medirandevu.doctor_service.dto.ScheduleResponse;
import com.medirandevu.doctor_service.dto.SlotResponse;
import com.medirandevu.doctor_service.dto.WeeklyScheduleRequest;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {
    List<ScheduleResponse> getSchedule(Long doctorId);
    List<ScheduleResponse> updateMySchedule(Long userId, WeeklyScheduleRequest request);
    List<SlotResponse> getSlots(Long doctorId, LocalDate date);
}