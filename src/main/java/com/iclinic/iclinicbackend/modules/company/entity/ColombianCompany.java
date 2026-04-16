package com.iclinic.iclinicbackend.modules.company.entity;

import com.iclinic.iclinicbackend.shared.enums.CompanyType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;

// Entidad de empresa colombiana con NIT - discriminator COLOMBIAN
@Entity
@DiscriminatorValue("COLOMBIAN")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(callSuper = true)
public class ColombianCompany extends Company {

    @Column(nullable = true, unique = true, length = 20)
    private String nit;

    public ColombianCompany(String name, String nit) {
        super(null, name, CompanyType.COLOMBIAN, new java.util.ArrayList<>());
        this.nit = nit;
    }

    @Override
    public String getTaxId() {
        return this.nit;
    }
}