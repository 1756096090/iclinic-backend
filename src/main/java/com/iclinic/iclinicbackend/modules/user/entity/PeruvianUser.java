package com.iclinic.iclinicbackend.modules.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

// Usuario peruano — documento: DNI (8 dígitos) o RUC (11 dígitos)
@Entity
@DiscriminatorValue("PERUVIAN")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PeruvianUser extends User {

    @Column(length = 20)
    private String documentNumber;

    @Override
    public String getDocumentNumber() {
        return documentNumber;
    }
}
