package com.iclinic.iclinicbackend.modules.branch.entity;

import com.iclinic.iclinicbackend.shared.enums.BranchType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;

// Entidad de sucursal de clínica - discriminator CLINIC
@Entity
@DiscriminatorValue("CLINIC")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(callSuper = true)
public class ClinicBranch extends Branch {

    @Column
    private Boolean hasLaboratory;

    public ClinicBranch(String name, String address, Boolean hasLaboratory,
            com.iclinic.iclinicbackend.modules.company.entity.Company company) {
        super(null, name, address, BranchType.CLINIC, company);
        this.hasLaboratory = hasLaboratory;
    }
}