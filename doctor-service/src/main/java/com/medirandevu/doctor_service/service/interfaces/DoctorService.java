package com.medirandevu.doctor_service.service.interfaces;

import com.medirandevu.doctor_service.dto.CreateDoctorRequest;
import com.medirandevu.doctor_service.dto.DoctorResponse;
import com.medirandevu.doctor_service.entity.Doctor;

import java.util.List;

public interface DoctorService {
    List<DoctorResponse> getAll(Long departmentId);
    DoctorResponse getById(Long id);
    DoctorResponse getByUserId(Long userId);
    DoctorResponse create(CreateDoctorRequest request);
}
