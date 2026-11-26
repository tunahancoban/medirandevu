package com.medirandevu.doctor_service.repository;

import com.medirandevu.doctor_service.entity.Doctor;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // Methods with @EntityGraph fetch the department in the same query, so mapping to DoctorResponse
    // doesn't trigger a lazy load (open-in-view is disabled)

    boolean existsByUserId(Long userId);

    @EntityGraph(attributePaths = "department")
    Optional<Doctor> findWithDepartmentById(Long id);

    @EntityGraph(attributePaths = "department")
    Optional<Doctor> findWithDepartmentByUserId(Long userId);

    // No department fetch: used when only the doctor's id is needed
    Optional<Doctor> findByUserId(Long userId);

    @EntityGraph(attributePaths = "department")
    List<Doctor> findAllByActiveTrue();

    @EntityGraph(attributePaths = "department")
    List<Doctor> findAllByDepartmentIdAndActiveTrue(Long departmentId);
}