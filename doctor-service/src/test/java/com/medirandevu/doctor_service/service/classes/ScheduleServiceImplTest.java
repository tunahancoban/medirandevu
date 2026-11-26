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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static java.time.DayOfWeek.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");
    // Fixed "now": Thursday 2026-11-26 10:10 (Istanbul), so slot results are deterministic
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);
    private static final Clock CLOCK = Clock.fixed(TODAY.atTime(10, 10).atZone(ZONE).toInstant(), ZONE);

    @Mock ScheduleRepository scheduleRepository;
    @Mock DoctorRepository doctorRepository;

    private ScheduleServiceImpl scheduleService;

    private final Doctor doctor = Doctor.builder().id(1L).userId(7L).build();

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleServiceImpl(scheduleRepository, doctorRepository, CLOCK);
    }

    private Schedule schedule(DayOfWeek day, LocalTime start, LocalTime end, int slot) {
        return Schedule.builder().doctor(doctor).dayOfWeek(day)
                .startTime(start).endTime(end).slotMinutes(slot).build();
    }

    // ---------- getSchedule ----------

    @Test
    void getSchedule_returnsDaysSortedByDayOfWeek() {
        when(doctorRepository.existsById(1L)).thenReturn(true);
        when(scheduleRepository.findAllByDoctorId(1L)).thenReturn(List.of(
                schedule(FRIDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30),
                schedule(MONDAY, LocalTime.of(13, 0), LocalTime.of(17, 0), 20)));

        assertThat(scheduleService.getSchedule(1L)).containsExactly(
                new ScheduleResponse(MONDAY, LocalTime.of(13, 0), LocalTime.of(17, 0), 20),
                new ScheduleResponse(FRIDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30));
    }

    @Test
    void getSchedule_throwsNotFoundForUnknownDoctor() {
        when(doctorRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> scheduleService.getSchedule(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(scheduleRepository);
    }

    // ---------- updateMySchedule ----------

    @Test
    @SuppressWarnings("unchecked")
    void updateMySchedule_replacesExistingScheduleAndReturnsSorted() {
        when(doctorRepository.findByUserId(7L)).thenReturn(Optional.of(doctor));
        when(scheduleRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<ScheduleResponse> result = scheduleService.updateMySchedule(7L, new WeeklyScheduleRequest(List.of(
                new ScheduleRequest(WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 15),
                new ScheduleRequest(MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), 60))));

        InOrder inOrder = inOrder(scheduleRepository);
        inOrder.verify(scheduleRepository).deleteAllByDoctorId(1L);
        ArgumentCaptor<List<Schedule>> captor = ArgumentCaptor.forClass(List.class);
        inOrder.verify(scheduleRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(2)
                .allSatisfy(s -> assertThat(s.getDoctor()).isSameAs(doctor));

        assertThat(result).containsExactly(
                new ScheduleResponse(MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), 60),
                new ScheduleResponse(WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 15));
    }

    @Test
    void updateMySchedule_emptyListClearsSchedule() {
        when(doctorRepository.findByUserId(7L)).thenReturn(Optional.of(doctor));
        when(scheduleRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(scheduleService.updateMySchedule(7L, new WeeklyScheduleRequest(List.of()))).isEmpty();
        verify(scheduleRepository).deleteAllByDoctorId(1L);
    }

    @Test
    void updateMySchedule_throwsNotFoundWhenNoDoctorProfile() {
        when(doctorRepository.findByUserId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.updateMySchedule(7L, new WeeklyScheduleRequest(List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(scheduleRepository);
    }

    @Test
    void updateMySchedule_rejectsDuplicateDay() {
        assertInvalid(List.of(
                new ScheduleRequest(MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30),
                new ScheduleRequest(MONDAY, LocalTime.of(13, 0), LocalTime.of(17, 0), 30)),
                "birden fazla");
    }

    @Test
    void updateMySchedule_rejectsEndNotAfterStart() {
        assertInvalid(List.of(new ScheduleRequest(MONDAY, LocalTime.of(12, 0), LocalTime.of(12, 0), 30)),
                "bitiş saati");
        assertInvalid(List.of(new ScheduleRequest(MONDAY, LocalTime.of(12, 0), LocalTime.of(9, 0), 30)),
                "bitiş saati");
    }

    @Test
    void updateMySchedule_rejectsWorkingTimeShorterThanOneSlot() {
        assertInvalid(List.of(new ScheduleRequest(MONDAY, LocalTime.of(9, 0), LocalTime.of(9, 20), 30)),
                "en az bir slot");
    }

    @Test
    void updateMySchedule_acceptsWorkingTimeEqualToOneSlot() {
        when(doctorRepository.findByUserId(7L)).thenReturn(Optional.of(doctor));
        when(scheduleRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(scheduleService.updateMySchedule(7L, new WeeklyScheduleRequest(List.of(
                new ScheduleRequest(MONDAY, LocalTime.of(9, 0), LocalTime.of(9, 30), 30))))).hasSize(1);
    }

    private void assertInvalid(List<ScheduleRequest> days, String messagePart) {
        when(doctorRepository.findByUserId(7L)).thenReturn(Optional.of(doctor));

        assertThatThrownBy(() -> scheduleService.updateMySchedule(7L, new WeeklyScheduleRequest(days)))
                .isInstanceOf(InvalidScheduleException.class)
                .hasMessageContaining(messagePart);
        verify(scheduleRepository, never()).deleteAllByDoctorId(anyLong());
        verify(scheduleRepository, never()).saveAll(anyList());
    }

    // ---------- getSlots ----------

    @Test
    void getSlots_throwsNotFoundForUnknownDoctor() {
        when(doctorRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> scheduleService.getSlots(99L, TODAY))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSlots_returnsEmptyForPastDate() {
        when(doctorRepository.existsById(1L)).thenReturn(true);

        assertThat(scheduleService.getSlots(1L, TODAY.minusDays(1))).isEmpty();
        verifyNoInteractions(scheduleRepository);
    }

    @Test
    void getSlots_returnsEmptyWhenDoctorDoesNotWorkThatDay() {
        when(doctorRepository.existsById(1L)).thenReturn(true);
        when(scheduleRepository.findByDoctorIdAndDayOfWeek(1L, FRIDAY)).thenReturn(Optional.empty());

        assertThat(scheduleService.getSlots(1L, TODAY.plusDays(1))).isEmpty();
    }

    @Test
    void getSlots_futureDate_returnsAllFullSlotsAndDropsPartialTail() {
        LocalDate friday = TODAY.plusDays(1);
        when(doctorRepository.existsById(1L)).thenReturn(true);
        when(scheduleRepository.findByDoctorIdAndDayOfWeek(1L, FRIDAY)).thenReturn(Optional.of(
                schedule(FRIDAY, LocalTime.of(9, 0), LocalTime.of(10, 10), 30)));

        assertThat(scheduleService.getSlots(1L, friday)).containsExactly(
                new SlotResponse(friday.atTime(9, 0), friday.atTime(9, 30)),
                new SlotResponse(friday.atTime(9, 30), friday.atTime(10, 0)));
    }

    @Test
    void getSlots_today_skipsSlotsStartingAtOrBeforeNow() {
        when(doctorRepository.existsById(1L)).thenReturn(true);
        when(scheduleRepository.findByDoctorIdAndDayOfWeek(1L, THURSDAY)).thenReturn(Optional.of(
                schedule(THURSDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), 10)));

        // now = 10:10 → the 10:10 slot counts as started too (cursor.isAfter(now))
        assertThat(scheduleService.getSlots(1L, TODAY)).containsExactly(
                new SlotResponse(TODAY.atTime(10, 20), TODAY.atTime(10, 30)),
                new SlotResponse(TODAY.atTime(10, 30), TODAY.atTime(10, 40)),
                new SlotResponse(TODAY.atTime(10, 40), TODAY.atTime(10, 50)),
                new SlotResponse(TODAY.atTime(10, 50), TODAY.atTime(11, 0)));
    }

    @Test
    void getSlots_today_returnsEmptyWhenWorkingHoursAlreadyOver() {
        when(doctorRepository.existsById(1L)).thenReturn(true);
        when(scheduleRepository.findByDoctorIdAndDayOfWeek(1L, THURSDAY)).thenReturn(Optional.of(
                schedule(THURSDAY, LocalTime.of(8, 0), LocalTime.of(10, 0), 30)));

        assertThat(scheduleService.getSlots(1L, TODAY)).isEmpty();
    }
}
