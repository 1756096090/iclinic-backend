package com.iclinic.iclinicbackend.modules.company.entity;

import com.iclinic.iclinicbackend.shared.enums.CompanyType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;

// Entidad de empresa ecuatoriana con RUC - discriminator ECUADORIAN
@Entity
@DiscriminatorValue("ECUADORIAN")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(callSuper = true)
public class EcuadorianCompany extends Company {

    @Column(nullable = true, unique = true, length = 20)
    private String ruc;

    public EcuadorianCompany(String name, String ruc) {
        super(null, name, CompanyType.ECUADORIAN, new java.util.ArrayList<>());
        this.ruc = ruc;
    }

    @Override
    public String getTaxId() {
        return this.ruc;
    }
}