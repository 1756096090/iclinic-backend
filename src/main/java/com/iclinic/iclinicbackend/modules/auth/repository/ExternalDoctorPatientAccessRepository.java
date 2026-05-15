package com.iclinic.iclinicbackend.modules.auth.repository;

import com.iclinic.iclinicbackend.modules.auth.entity.ExternalDoctorPatientAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExternalDoctorPatientAccessRepository extends JpaRepository<ExternalDoctorPatientAccess, Long> {

    List<ExternalDoctorPatientAccess> findByExternalDoctorIdAndActiveTrueAndExpiresAtAfter(
            Long doctorId, LocalDateTime now);

    Optional<ExternalDoctorPatientAccess> findByExternalDoctorIdAndPatientIdAndActiveTrueAndExpiresAtAfter(
            Long doctorId, Long patientId, LocalDateTime now);
}
