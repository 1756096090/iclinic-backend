package com.iclinic.iclinicbackend.modules.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

// Usuario colombiano — documento: Cédula de ciudadanía o NIT
@Entity
@DiscriminatorValue("COLOMBIAN")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ColombianUser extends User {

    @Column(length = 20)
    private String documentNumber;

    @Override
    public String getDocumentNumber() {
        return documentNumber;
    }
}
