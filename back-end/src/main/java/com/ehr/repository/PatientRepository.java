package com.ehr.repository;
import com.ehr.models.Patient;
import com.ehr.models.User; // Import User model
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // Import Optional

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUser(User user); // New method

    @Query("SELECT p FROM Patient p JOIN p.user u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "u.phoneNumber LIKE CONCAT('%', :searchTerm, '%')")
    List<Patient> searchPatients(@Param("searchTerm") String searchTerm);

    @Query("SELECT p FROM Patient p JOIN p.user u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "u.phoneNumber LIKE CONCAT('%', :searchTerm, '%')")
    Page<Patient> searchPatientsPaginated(@Param("searchTerm") String searchTerm, Pageable pageable);
}
