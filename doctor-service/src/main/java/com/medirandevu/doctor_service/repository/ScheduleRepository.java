package com.medirandevu.doctor_service.repository;

import com.medirandevu.doctor_service.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findAllByDoctorId(Long doctorId);

    Optional<Schedule> findByDoctorIdAndDayOfWeek(Long doctorId, DayOfWeek dayOfWeek);

    // Bulk JPQL delete: runs immediately in one statement instead of loading each row first
    @Modifying
    @Query("delete from Schedule s where s.doctor.id = :doctorId")
    void deleteAllByDoctorId(@Param("doctorId") Long doctorId);
}

