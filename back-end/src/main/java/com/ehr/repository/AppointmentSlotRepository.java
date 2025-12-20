package com.ehr.repository;

import com.ehr.models.AppointmentSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import com.ehr.models.Staff;

@Repository
public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {
    List<AppointmentSlot> findByStatusAndStartTimeBetweenOrderByStartTimeAsc(AppointmentSlot.SlotStatus status, LocalDateTime from, LocalDateTime to);
    List<AppointmentSlot> findByDoctorAndStartTimeBetweenOrderByStartTimeAsc(Staff doctor, LocalDateTime from, LocalDateTime to);
}
