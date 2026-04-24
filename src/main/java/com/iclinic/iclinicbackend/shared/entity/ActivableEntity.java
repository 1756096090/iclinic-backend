package com.iclinic.iclinicbackend.shared.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class ActivableEntity extends BaseEntity {

    @Column(nullable = false)
    private Boolean active;

    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.active == null) {
            this.active = true;
        }
    }
}