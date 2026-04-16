package com.iclinic.iclinicbackend.modules.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

// Usuario internacional — documento: Pasaporte
@Entity
@DiscriminatorValue("INTERNATIONAL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class InternationalUser extends User {

    @Column(length = 30)
    private String passportNumber;

    @Column(length = 100)
    private String nationality; // Ej: "Venezolana", "Brasileña", etc.

    @Override
    public String getDocumentNumber() {
        return passportNumber;
    }
}
