package com.medirandevu.doctor_service.repository;

import com.medirandevu.doctor_service.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<Department> findAllByActiveTrueOrderByNameAsc();
}
