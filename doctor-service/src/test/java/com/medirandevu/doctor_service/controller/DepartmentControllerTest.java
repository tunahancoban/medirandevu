package com.medirandevu.doctor_service.controller;

import com.medirandevu.doctor_service.config.GlobalExceptionHandler;
import com.medirandevu.doctor_service.dto.CreateDepartmentRequest;
import com.medirandevu.doctor_service.dto.DepartmentResponse;
import com.medirandevu.doctor_service.exception.DuplicateResourceException;
import com.medirandevu.doctor_service.service.interfaces.DepartmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the controller and GlobalExceptionHandler with standalone MockMvc, without a Spring context.
 * Security (@PreAuthorize) is not active at this layer.
 */
@ExtendWith(MockitoExtension.class)
class DepartmentControllerTest {

    @Mock DepartmentService departmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new DepartmentController(departmentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void getDepartments_returnsList() throws Exception {
        when(departmentService.getAll()).thenReturn(List.of(
                new DepartmentResponse(1L, "Dahiliye", "İç hastalıkları")));

        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Dahiliye"))
                .andExpect(jsonPath("$[0].description").value("İç hastalıkları"));
    }

    @Test
    void createDepartment_returns201() throws Exception {
        when(departmentService.create(new CreateDepartmentRequest("Kardiyoloji", "Kalp")))
                .thenReturn(new DepartmentResponse(5L, "Kardiyoloji", "Kalp"));

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Kardiyoloji\",\"description\":\"Kalp\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Kardiyoloji"));
    }

    @Test
    void createDepartment_returns400WhenNameBlank() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists());

        verifyNoInteractions(departmentService);
    }

    @Test
    void createDepartment_returns400WhenNameTooLong() throws Exception {
        String longName = "a".repeat(101);
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + longName + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createDepartment_returns400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request"));
    }

    @Test
    void createDepartment_returns409WhenDuplicate() throws Exception {
        when(departmentService.create(any()))
                .thenThrow(new DuplicateResourceException("Bu isimde bir bölüm zaten var: Kardiyoloji"));

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Kardiyoloji\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Bu isimde bir bölüm zaten var: Kardiyoloji"));
    }

    @Test
    void createDepartment_returns409OnDbUniqueViolation() throws Exception {
        when(departmentService.create(any()))
                .thenThrow(new DataIntegrityViolationException("uq_departments_name"));

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Kardiyoloji\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Kayıt mevcut verilerle çakışıyor"));
    }

    @Test
    void unexpectedError_returns500WithoutLeakingDetails() throws Exception {
        when(departmentService.getAll()).thenThrow(new IllegalStateException("db password=secret"));

        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Beklenmeyen bir hata oluştu"));
    }
}
