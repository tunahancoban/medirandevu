package com.medirandevu.doctor_service.service.classes;

import com.medirandevu.doctor_service.dto.CreateDepartmentRequest;
import com.medirandevu.doctor_service.dto.DepartmentResponse;
import com.medirandevu.doctor_service.entity.Department;
import com.medirandevu.doctor_service.exception.DuplicateResourceException;
import com.medirandevu.doctor_service.repository.DepartmentRepository;
import com.medirandevu.doctor_service.service.interfaces.DepartmentService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository departmentRepository;


    @Override
    public List<DepartmentResponse> getAll() {
        return departmentRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request) {
        // Uniqueness is case-insensitive; the DB unique constraint covers concurrent inserts
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Bu isimde bir bölüm zaten var: " + name);
        }
        Department saved = departmentRepository.save(Department.builder()
                .name(name)
                .description(request.description())
                .build());
        return toResponse(saved);
    }

    public DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(d.getId(), d.getName(), d.getDescription());    }
}
