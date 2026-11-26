package com.medirandevu.doctor_service.service.classes;

import com.medirandevu.doctor_service.dto.CreateDepartmentRequest;
import com.medirandevu.doctor_service.dto.DepartmentResponse;
import com.medirandevu.doctor_service.entity.Department;
import com.medirandevu.doctor_service.exception.DuplicateResourceException;
import com.medirandevu.doctor_service.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock DepartmentRepository departmentRepository;
    @InjectMocks DepartmentServiceImpl departmentService;

    @Test
    void getAll_mapsActiveDepartmentsInRepositoryOrder() {
        when(departmentRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(
                Department.builder().id(1L).name("Dahiliye").description("İç hastalıkları").build(),
                Department.builder().id(2L).name("Kardiyoloji").build()));

        List<DepartmentResponse> result = departmentService.getAll();

        assertThat(result).containsExactly(
                new DepartmentResponse(1L, "Dahiliye", "İç hastalıkları"),
                new DepartmentResponse(2L, "Kardiyoloji", null));
    }

    @Test
    void getAll_returnsEmptyListWhenNoDepartments() {
        when(departmentRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of());

        assertThat(departmentService.getAll()).isEmpty();
    }

    @Test
    void create_trimsNameAndSavesActiveDepartment() {
        when(departmentRepository.existsByNameIgnoreCase("Kardiyoloji")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> {
            Department d = inv.getArgument(0);
            d.setId(10L);
            return d;
        });

        DepartmentResponse result = departmentService.create(
                new CreateDepartmentRequest("  Kardiyoloji  ", "Kalp"));

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(captor.capture());
        Department saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Kardiyoloji");
        assertThat(saved.getDescription()).isEqualTo("Kalp");
        assertThat(saved.isActive()).isTrue();

        assertThat(result).isEqualTo(new DepartmentResponse(10L, "Kardiyoloji", "Kalp"));
    }

    @Test
    void create_throwsDuplicateWhenNameExists() {
        when(departmentRepository.existsByNameIgnoreCase("Kardiyoloji")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.create(new CreateDepartmentRequest(" Kardiyoloji ", null)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Kardiyoloji");

        verify(departmentRepository, never()).save(any());
    }
}
