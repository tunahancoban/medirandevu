package com.medirandevu.doctor_service.controller;

import com.medirandevu.doctor_service.config.GlobalExceptionHandler;
import com.medirandevu.doctor_service.dto.*;
import com.medirandevu.doctor_service.exception.DuplicateResourceException;
import com.medirandevu.doctor_service.exception.InvalidScheduleException;
import com.medirandevu.doctor_service.exception.ResourceNotFoundException;
import com.medirandevu.doctor_service.service.interfaces.DoctorService;
import com.medirandevu.doctor_service.service.interfaces.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the controller and GlobalExceptionHandler with standalone MockMvc, without a Spring context.
 * Security (@PreAuthorize) is not active at this layer.
 */
@ExtendWith(MockitoExtension.class)
class DoctorControllerTest {

    @Mock DoctorService doctorService;
    @Mock ScheduleService scheduleService;

    private MockMvc mockMvc;

    private static final DoctorResponse DOCTOR =
            new DoctorResponse(1L, 7L, "Ayşe", "Yılmaz", "Uzm. Dr.", 3L, "Kardiyoloji");

    // Mimics the JWT filter: principal name = auth-service user id
    private static final UsernamePasswordAuthenticationToken DOCTOR_AUTH =
            new UsernamePasswordAuthenticationToken("7", null, List.of());

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new DoctorController(doctorService, scheduleService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ---------- list / get ----------

    @Test
    void getDoctors_withoutFilter_passesNullDepartment() throws Exception {
        when(doctorService.getAll(null)).thenReturn(List.of(DOCTOR));

        mockMvc.perform(get("/api/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].departmentName").value("Kardiyoloji"));
    }

    @Test
    void getDoctors_withDepartmentFilter() throws Exception {
        when(doctorService.getAll(3L)).thenReturn(List.of(DOCTOR));

        mockMvc.perform(get("/api/doctors").param("departmentId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getDoctorById_returnsDoctor() throws Exception {
        when(doctorService.getById(1L)).thenReturn(DOCTOR);

        mockMvc.perform(get("/api/doctors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ayşe"))
                .andExpect(jsonPath("$.userId").value(7));
    }

    @Test
    void getDoctorById_returns404WhenMissing() throws Exception {
        when(doctorService.getById(99L)).thenThrow(new ResourceNotFoundException("Doktor bulunamadı: 99"));

        mockMvc.perform(get("/api/doctors/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.detail").value("Doktor bulunamadı: 99"));
    }

    // ---------- schedule / slots ----------

    @Test
    void getSchedule_returnsWeeklySchedule() throws Exception {
        when(scheduleService.getSchedule(1L)).thenReturn(List.of(
                new ScheduleResponse(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30)));

        mockMvc.perform(get("/api/doctors/1/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$[0].slotMinutes").value(30));
    }

    @Test
    void getSlots_parsesIsoDate() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 27);
        when(scheduleService.getSlots(1L, date)).thenReturn(List.of(
                new SlotResponse(date.atTime(9, 0), date.atTime(9, 30))));

        mockMvc.perform(get("/api/doctors/1/slots").param("date", "2026-11-27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].start").value("2026-11-27T09:00:00"))
                .andExpect(jsonPath("$[0].end").value("2026-11-27T09:30:00"));
    }

    @Test
    void getSlots_returns400WhenDateMissing() throws Exception {
        mockMvc.perform(get("/api/doctors/1/slots"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Missing parameter"))
                .andExpect(jsonPath("$.detail").value("Zorunlu parametre eksik: date"));

        verifyNoInteractions(scheduleService);
    }

    @Test
    void getSlots_returns400WhenDateMalformed() throws Exception {
        mockMvc.perform(get("/api/doctors/1/slots").param("date", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid parameter"))
                .andExpect(jsonPath("$.detail").value("Parametre değeri geçersiz: date"));

        verifyNoInteractions(scheduleService);
    }

    @Test
    void getDoctorById_returns400WhenIdNotNumeric() throws Exception {
        mockMvc.perform(get("/api/doctors/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Parametre değeri geçersiz: id"));

        verifyNoInteractions(doctorService);
    }

    // ---------- create ----------

    @Test
    void createDoctor_returns201() throws Exception {
        when(doctorService.create(new CreateDoctorRequest(7L, 3L, "Ayşe", "Yılmaz", "Uzm. Dr.")))
                .thenReturn(DOCTOR);

        mockMvc.perform(post("/api/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":7,"departmentId":3,"firstName":"Ayşe",
                                 "lastName":"Yılmaz","title":"Uzm. Dr."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createDoctor_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":-1,\"firstName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.userId").exists())
                .andExpect(jsonPath("$.errors.departmentId").exists())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.lastName").exists())
                .andExpect(jsonPath("$.errors.title").exists());

        verifyNoInteractions(doctorService);
    }

    @Test
    void createDoctor_returns409WhenProfileExists() throws Exception {
        when(doctorService.create(any()))
                .thenThrow(new DuplicateResourceException("Bu kullanıcıya ait doktor profili zaten var"));

        mockMvc.perform(post("/api/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":7,"departmentId":3,"firstName":"Ayşe",
                                 "lastName":"Yılmaz","title":"Uzm. Dr."}
                                """))
                .andExpect(status().isConflict());
    }

    // ---------- /me ----------

    @Test
    void getMe_usesAuthenticationNameAsUserId() throws Exception {
        when(doctorService.getByUserId(7L)).thenReturn(DOCTOR);

        mockMvc.perform(get("/api/doctors/me").principal(DOCTOR_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(7));
    }

    @Test
    void updateMySchedule_passesUserIdAndBody() throws Exception {
        WeeklyScheduleRequest expected = new WeeklyScheduleRequest(List.of(
                new ScheduleRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30)));
        when(scheduleService.updateMySchedule(7L, expected)).thenReturn(List.of(
                new ScheduleResponse(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30)));

        mockMvc.perform(put("/api/doctors/me/schedule")
                        .principal(DOCTOR_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"days":[{"dayOfWeek":"MONDAY","startTime":"09:00",
                                          "endTime":"12:00","slotMinutes":30}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"));
    }

    @Test
    void updateMySchedule_returns400WhenSlotMinutesOutOfRange() throws Exception {
        mockMvc.perform(put("/api/doctors/me/schedule")
                        .principal(DOCTOR_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"days":[{"dayOfWeek":"MONDAY","startTime":"09:00",
                                          "endTime":"12:00","slotMinutes":3}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['days[0].slotMinutes']").exists());

        verifyNoInteractions(scheduleService);
    }

    @Test
    void updateMySchedule_returns400WhenDaysMissing() throws Exception {
        mockMvc.perform(put("/api/doctors/me/schedule")
                        .principal(DOCTOR_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.days").exists());
    }

    @Test
    void updateMySchedule_returns400OnInvalidSchedule() throws Exception {
        when(scheduleService.updateMySchedule(eq(7L), any()))
                .thenThrow(new InvalidScheduleException("MONDAY günü birden fazla kez gönderildi"));

        mockMvc.perform(put("/api/doctors/me/schedule")
                        .principal(DOCTOR_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"days\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid schedule"))
                .andExpect(jsonPath("$.detail").value("MONDAY günü birden fazla kez gönderildi"));
    }
}
