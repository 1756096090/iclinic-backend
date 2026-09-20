package com.iclinic.iclinicbackend.modules.appointment.entity;

import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.shared.entity.ActivableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "branch_blocked_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BranchBlockedSlot extends ActivableEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "start_date_time", nullable = false)
    private Instant startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private Instant endDateTime;

    @Column(length = 255)
    private String reason;

    /**
     * Empresa propietaria, denormalizada. NO es redundante: es la columna sobre
     * la que actua la politica de RLS y la que respalda la FK compuesta. Se
     * deriva del padre en {@code @PrePersist} para que ningun camino de alta
     * pueda dejarla incoherente.
     */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @PrePersist
    public void derivarEmpresa() {
        if (this.companyId == null && branch != null) this.companyId = branch.getCompany().getId();
    }
}