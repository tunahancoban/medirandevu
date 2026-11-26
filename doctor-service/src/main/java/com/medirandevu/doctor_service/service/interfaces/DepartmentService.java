package com.medirandevu.doctor_service.service.interfaces;

import com.medirandevu.doctor_service.dto.CreateDepartmentRequest;
import com.medirandevu.doctor_service.dto.DepartmentResponse;
import com.medirandevu.doctor_service.entity.Department;

import java.util.List;

public interface DepartmentService {
    List<DepartmentResponse> getAll();
    DepartmentResponse create(CreateDepartmentRequest request);
}
