package com.medirandevu.doctor_service.service.classes;

import com.medirandevu.doctor_service.dto.CreateDoctorRequest;
import com.medirandevu.doctor_service.dto.DoctorResponse;
import com.medirandevu.doctor_service.entity.Department;
import com.medirandevu.doctor_service.entity.Doctor;
import com.medirandevu.doctor_service.exception.DuplicateResourceException;
import com.medirandevu.doctor_service.exception.ResourceNotFoundException;
import com.medirandevu.doctor_service.repository.DepartmentRepository;
import com.medirandevu.doctor_service.repository.DoctorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorServiceImplTest {

    @Mock DoctorRepository doctorRepository;
    @Mock DepartmentRepository departmentRepository;
    @InjectMocks DoctorServiceImpl doctorService;

    private final Department cardiology = Department.builder().id(3L).name("Kardiyoloji").build();

    private Doctor doctor(long id, long userId) {
        return Doctor.builder()
                .id(id).userId(userId).department(cardiology)
                .firstName("Ayşe").lastName("Yılmaz").title("Uzm. Dr.")
                .build();
    }

    private final DoctorResponse expected =
            new DoctorResponse(1L, 7L, "Ayşe", "Yılmaz", "Uzm. Dr.", 3L, "Kardiyoloji");

    // ---------- getAll ----------

    @Test
    void getAll_withoutDepartment_returnsAllActiveDoctors() {
        when(doctorRepository.findAllByActiveTrue()).thenReturn(List.of(doctor(1L, 7L)));

        assertThat(doctorService.getAll(null)).containsExactly(expected);
        verify(doctorRepository, never()).findAllByDepartmentIdAndActiveTrue(anyLong());
    }

    @Test
    void getAll_withDepartment_filtersByDepartment() {
        when(doctorRepository.findAllByDepartmentIdAndActiveTrue(3L)).thenReturn(List.of(doctor(1L, 7L)));

        assertThat(doctorService.getAll(3L)).containsExactly(expected);
        verify(doctorRepository, never()).findAllByActiveTrue();
    }

    // ---------- getById / getByUserId ----------

    @Test
    void getById_returnsDoctor() {
        when(doctorRepository.findWithDepartmentById(1L)).thenReturn(Optional.of(doctor(1L, 7L)));

        assertThat(doctorService.getById(1L)).isEqualTo(expected);
    }

    @Test
    void getById_throwsNotFoundWhenMissing() {
        when(doctorRepository.findWithDepartmentById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getByUserId_returnsDoctor() {
        when(doctorRepository.findWithDepartmentByUserId(7L)).thenReturn(Optional.of(doctor(1L, 7L)));

        assertThat(doctorService.getByUserId(7L)).isEqualTo(expected);
    }

    @Test
    void getByUserId_throwsNotFoundWhenNoProfile() {
        when(doctorRepository.findWithDepartmentByUserId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.getByUserId(7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- create ----------

    @Test
    void create_trimsFieldsAndSavesDoctor() {
        when(doctorRepository.existsByUserId(7L)).thenReturn(false);
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(cardiology));
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(inv -> {
            Doctor d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        DoctorResponse result = doctorService.create(
                new CreateDoctorRequest(7L, 3L, " Ayşe ", " Yılmaz ", " Uzm. Dr. "));

        ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorRepository).save(captor.capture());
        Doctor saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getDepartment()).isSameAs(cardiology);
        assertThat(saved.getFirstName()).isEqualTo("Ayşe");
        assertThat(saved.getLastName()).isEqualTo("Yılmaz");
        assertThat(saved.getTitle()).isEqualTo("Uzm. Dr.");
        assertThat(saved.isActive()).isTrue();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void create_throwsDuplicateWhenUserAlreadyHasProfile() {
        when(doctorRepository.existsByUserId(7L)).thenReturn(true);

        assertThatThrownBy(() -> doctorService.create(
                new CreateDoctorRequest(7L, 3L, "Ayşe", "Yılmaz", "Uzm. Dr.")))
                .isInstanceOf(DuplicateResourceException.class);

        verifyNoInteractions(departmentRepository);
        verify(doctorRepository, never()).save(any());
    }

    @Test
    void create_throwsNotFoundWhenDepartmentMissing() {
        when(doctorRepository.existsByUserId(7L)).thenReturn(false);
        when(departmentRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.create(
                new CreateDoctorRequest(7L, 3L, "Ayşe", "Yılmaz", "Uzm. Dr.")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("3");

        verify(doctorRepository, never()).save(any());
    }

    @Test
    void create_throwsNotFoundWhenDepartmentInactive() {
        cardiology.setActive(false);
        when(doctorRepository.existsByUserId(7L)).thenReturn(false);
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(cardiology));

        assertThatThrownBy(() -> doctorService.create(
                new CreateDoctorRequest(7L, 3L, "Ayşe", "Yılmaz", "Uzm. Dr.")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(doctorRepository, never()).save(any());
    }
}
