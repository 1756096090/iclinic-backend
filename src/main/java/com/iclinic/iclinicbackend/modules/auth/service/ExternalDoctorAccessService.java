package com.iclinic.iclinicbackend.modules.auth.service;

import com.iclinic.iclinicbackend.modules.auth.dto.ExternalDoctorAccessRequestDto;
import com.iclinic.iclinicbackend.modules.auth.dto.ExternalDoctorAccessResponseDto;
import com.iclinic.iclinicbackend.modules.auth.entity.ExternalDoctorPatientAccess;
import com.iclinic.iclinicbackend.modules.auth.repository.ExternalDoctorPatientAccessRepository;
import com.iclinic.iclinicbackend.modules.user.entity.User;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExternalDoctorAccessService {

    private final ExternalDoctorPatientAccessRepository accessRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public ExternalDoctorAccessResponseDto create(ExternalDoctorAccessRequestDto dto) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != UserRole.SUPER_ADMIN && currentUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin permiso para crear accesos");
        }
        if (currentUser.getRole() == UserRole.ADMIN) {
            currentUserService.assertCanAccessCompany(dto.getCompanyId());
        }

        User doctor = userRepository.findById(dto.getExternalDoctorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor no encontrado"));
        if (doctor.getRole() != UserRole.EXTERNAL_DOCTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario no es un EXTERNAL_DOCTOR");
        }

        ExternalDoctorPatientAccess access = ExternalDoctorPatientAccess.builder()
                .externalDoctor(doctor)
                .patientId(dto.getPatientId())
                .companyId(dto.getCompanyId())
                .branchId(dto.getBranchId())
                .appointmentId(dto.getAppointmentId())
                .conversationId(dto.getConversationId())
                .reason(dto.getReason())
                .active(true)
                .expiresAt(dto.getExpiresAt())
                .createdBy(currentUser)
                .build();

        return toResponseDto(accessRepository.save(access));
    }

    @Transactional(readOnly = true)
    public List<ExternalDoctorAccessResponseDto> getMyPatients() {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != UserRole.EXTERNAL_DOCTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo disponible para EXTERNAL_DOCTOR");
        }
        return accessRepository
                .findByExternalDoctorIdAndActiveTrueAndExpiresAtAfter(currentUser.getId(), LocalDateTime.now())
                .stream().map(this::toResponseDto).toList();
    }

    @Transactional(readOnly = true)
    public ExternalDoctorAccessResponseDto getPatientAccess(Long patientId) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != UserRole.EXTERNAL_DOCTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo disponible para EXTERNAL_DOCTOR");
        }
        return accessRepository
                .findByExternalDoctorIdAndPatientIdAndActiveTrueAndExpiresAtAfter(
                        currentUser.getId(), patientId, LocalDateTime.now())
                .map(this::toResponseDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin acceso a este paciente"));
    }

    public ExternalDoctorAccessResponseDto deactivate(Long id) {
        User currentUser = currentUserService.getCurrentUser();
        ExternalDoctorPatientAccess access = accessRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acceso no encontrado"));
        if (currentUser.getRole() == UserRole.ADMIN) {
            currentUserService.assertCanAccessCompany(access.getCompanyId());
        } else if (currentUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin permiso");
        }
        access.setActive(false);
        return toResponseDto(accessRepository.save(access));
    }

    private ExternalDoctorAccessResponseDto toResponseDto(ExternalDoctorPatientAccess a) {
        String doctorName = a.getExternalDoctor().getFirstName() + " " + a.getExternalDoctor().getLastName();
        return ExternalDoctorAccessResponseDto.builder()
                .id(a.getId())
                .externalDoctorId(a.getExternalDoctor().getId())
                .externalDoctorName(doctorName)
                .patientId(a.getPatientId())
                .companyId(a.getCompanyId())
                .branchId(a.getBranchId())
                .appointmentId(a.getAppointmentId())
                .conversationId(a.getConversationId())
                .reason(a.getReason())
                .active(a.getActive())
                .expiresAt(a.getExpiresAt())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
