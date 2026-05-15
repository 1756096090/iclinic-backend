package com.iclinic.iclinicbackend.modules.auth.controller;

import com.iclinic.iclinicbackend.modules.auth.dto.ExternalDoctorAccessRequestDto;
import com.iclinic.iclinicbackend.modules.auth.dto.ExternalDoctorAccessResponseDto;
import com.iclinic.iclinicbackend.modules.auth.service.ExternalDoctorAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/external-doctor-access")
@RequiredArgsConstructor
public class ExternalDoctorAccessController {

    private final ExternalDoctorAccessService service;

    @PostMapping
    public ResponseEntity<ExternalDoctorAccessResponseDto> create(@RequestBody ExternalDoctorAccessRequestDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @GetMapping("/my-patients")
    public ResponseEntity<List<ExternalDoctorAccessResponseDto>> getMyPatients() {
        return ResponseEntity.ok(service.getMyPatients());
    }

    @GetMapping("/patients/{patientId}")
    public ResponseEntity<ExternalDoctorAccessResponseDto> getPatientAccess(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.getPatientAccess(patientId));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ExternalDoctorAccessResponseDto> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(service.deactivate(id));
    }
}
