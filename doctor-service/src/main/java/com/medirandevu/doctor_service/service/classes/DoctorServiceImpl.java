package com.medirandevu.doctor_service.service.classes;

import com.medirandevu.doctor_service.dto.CreateDoctorRequest;
import com.medirandevu.doctor_service.dto.DoctorResponse;
import com.medirandevu.doctor_service.entity.Department;
import com.medirandevu.doctor_service.entity.Doctor;
import com.medirandevu.doctor_service.exception.DuplicateResourceException;
import com.medirandevu.doctor_service.exception.ResourceNotFoundException;
import com.medirandevu.doctor_service.repository.DepartmentRepository;
import com.medirandevu.doctor_service.repository.DoctorRepository;
import com.medirandevu.doctor_service.service.interfaces.DoctorService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorServiceImpl implements DoctorService {
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    public List<DoctorResponse> getAll(Long departmentId) {
        List<Doctor> doctors = (departmentId == null)
                ? doctorRepository.findAllByActiveTrue()
                : doctorRepository.findAllByDepartmentIdAndActiveTrue(departmentId);
        return doctors.stream().map(this::toResponse).toList();
    }

    @Override
    public DoctorResponse getById(Long id) {
        return doctorRepository.findWithDepartmentById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Doktor bulunamadı: " + id));
    }

    @Override
    public DoctorResponse getByUserId(Long userId) {
        return doctorRepository.findWithDepartmentByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Bu hesaba bağlı doktor profili yok"));    }

    @Override
    @Transactional
    public DoctorResponse create(CreateDoctorRequest request) {
        if (doctorRepository.existsByUserId(request.userId())) {
            throw new DuplicateResourceException("Bu kullanıcıya ait doktor profili zaten var");
        }
        // Inactive departments are treated as missing
        Department department = departmentRepository.findById(request.departmentId())
                .filter(Department::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bölüm bulunamadı: " + request.departmentId()));

        Doctor saved = doctorRepository.save(Doctor.builder()
                .userId(request.userId())
                .department(department)
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .title(request.title().trim())
                .build());
        return toResponse(saved);
    }

    // Expects the department to be loaded already (see @EntityGraph queries)
    public DoctorResponse toResponse(Doctor d) {
        return new DoctorResponse(
                d.getId(), d.getUserId(),
                d.getFirstName(), d.getLastName(), d.getTitle(),
                d.getDepartment().getId(), d.getDepartment().getName());
    }
}
