package com.iclinic.iclinicbackend.modules.auth.entity;

import com.iclinic.iclinicbackend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "external_doctor_patient_access")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalDoctorPatientAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "external_doctor_id", nullable = false)
    private User externalDoctor;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private Long companyId;

    @Column
    private Long branchId;

    @Column
    private Long appointmentId;

    @Column
    private Long conversationId;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;
}
