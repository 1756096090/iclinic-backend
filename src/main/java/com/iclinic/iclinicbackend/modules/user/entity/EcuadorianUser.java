package com.iclinic.iclinicbackend.modules.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

// Usuario ecuatoriano — documento: Cédula (10 dígitos) o RUC (13 dígitos)
@Entity
@DiscriminatorValue("ECUADORIAN")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class EcuadorianUser extends User {

    @Column(length = 20)
    private String documentNumber;

    @Override
    public String getDocumentNumber() {
        return documentNumber;
    }
}
