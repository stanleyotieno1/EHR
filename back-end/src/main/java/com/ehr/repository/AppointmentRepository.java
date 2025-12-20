package com.ehr.repository;

import com.ehr.models.Appointment;
import com.ehr.models.Patient;
import com.ehr.models.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientOrderBySlot_StartTimeDesc(Patient patient);
    List<Appointment> findBySlot_DoctorOrderBySlot_StartTimeDesc(Staff doctor);
}