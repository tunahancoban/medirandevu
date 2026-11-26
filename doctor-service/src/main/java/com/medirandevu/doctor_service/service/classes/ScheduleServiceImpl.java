package com.medirandevu.doctor_service.service.classes;

import com.medirandevu.doctor_service.dto.ScheduleRequest;
import com.medirandevu.doctor_service.dto.ScheduleResponse;
import com.medirandevu.doctor_service.dto.SlotResponse;
import com.medirandevu.doctor_service.dto.WeeklyScheduleRequest;
import com.medirandevu.doctor_service.entity.Doctor;
import com.medirandevu.doctor_service.entity.Schedule;
import com.medirandevu.doctor_service.exception.InvalidScheduleException;
import com.medirandevu.doctor_service.exception.ResourceNotFoundException;
import com.medirandevu.doctor_service.repository.DoctorRepository;
import com.medirandevu.doctor_service.repository.ScheduleRepository;
import com.medirandevu.doctor_service.service.interfaces.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;
    private final Clock clock;

    @Override
    public List<ScheduleResponse> getSchedule(Long doctorId) {
        ensureDoctorExists(doctorId);
        return scheduleRepository.findAllByDoctorId(doctorId).stream()
                .sorted(Comparator.comparing(Schedule::getDayOfWeek))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<ScheduleResponse> updateMySchedule(Long userId, WeeklyScheduleRequest request) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bu hesaba bağlı doktor profili yok"));

        validate(request.days());

        // Full replace: drop the old week, then insert the new days
        scheduleRepository.deleteAllByDoctorId(doctor.getId());

        List<Schedule> schedules = request.days().stream()
                .map(day -> Schedule.builder()
                        .doctor(doctor)
                        .dayOfWeek(day.dayOfWeek())
                        .startTime(day.startTime())
                        .endTime(day.endTime())
                        .slotMinutes(day.slotMinutes())
                        .build())
                .toList();

        return scheduleRepository.saveAll(schedules).stream()
                .sorted(Comparator.comparing(Schedule::getDayOfWeek))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<SlotResponse> getSlots(Long doctorId, LocalDate date) {
        ensureDoctorExists(doctorId);

        LocalDateTime now = LocalDateTime.now(clock);
        if (date.isBefore(now.toLocalDate())) {
            return List.of();                                  // past day → no slots
        }

        return scheduleRepository.findByDoctorIdAndDayOfWeek(doctorId, date.getDayOfWeek())
                .map(schedule -> buildSlots(schedule, date, now))
                .orElse(List.of());                            // doctor doesn't work that day
    }


    // Splits working hours into fixed-length slots; a leftover shorter than one slot is dropped
    private List<SlotResponse> buildSlots(Schedule schedule, LocalDate date, LocalDateTime now) {
        List<SlotResponse> slots = new ArrayList<>();
        LocalDateTime cursor = date.atTime(schedule.getStartTime());
        LocalDateTime dayEnd = date.atTime(schedule.getEndTime());

        while (!cursor.plusMinutes(schedule.getSlotMinutes()).isAfter(dayEnd)) {
            LocalDateTime slotEnd = cursor.plusMinutes(schedule.getSlotMinutes());
            if (cursor.isAfter(now)) {                         // skip slots that already started
                slots.add(new SlotResponse(cursor, slotEnd));
            }
            cursor = slotEnd;
        }
        return slots;
    }

    // Business rules that @Valid can't express: unique days, end after start, room for at least one slot
    private void validate(List<ScheduleRequest> days) {
        Set<DayOfWeek> seen = EnumSet.noneOf(DayOfWeek.class);
        for (ScheduleRequest day : days) {
            if (!seen.add(day.dayOfWeek())) {
                throw new InvalidScheduleException(day.dayOfWeek() + " günü birden fazla kez gönderildi");
            }
            if (!day.endTime().isAfter(day.startTime())) {
                throw new InvalidScheduleException(day.dayOfWeek() + ": bitiş saati başlangıçtan sonra olmalı");
            }
            long minutes = Duration.between(day.startTime(), day.endTime()).toMinutes();
            if (minutes < day.slotMinutes()) {
                throw new InvalidScheduleException(day.dayOfWeek() + ": çalışma süresi en az bir slot kadar olmalı");
            }
        }
    }

    private void ensureDoctorExists(Long doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doktor bulunamadı: " + doctorId);
        }
    }

    private ScheduleResponse toResponse(Schedule s) {
        return new ScheduleResponse(s.getDayOfWeek(), s.getStartTime(), s.getEndTime(), s.getSlotMinutes());
    }
}