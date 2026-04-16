package com.iclinic.iclinicbackend.modules.branch.entity;

import com.iclinic.iclinicbackend.shared.enums.BranchType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;

// Entidad de sucursal de hospital - discriminator HOSPITAL
@Entity
@DiscriminatorValue("HOSPITAL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(callSuper = true)
public class HospitalBranch extends Branch {

    @Column
    private Integer bedCapacity;

    public HospitalBranch(String name, String address, Integer bedCapacity,
            com.iclinic.iclinicbackend.modules.company.entity.Company company) {
        super(null, name, address, BranchType.HOSPITAL, company);
        this.bedCapacity = bedCapacity;
    }
}