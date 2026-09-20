package com.iclinic.iclinicbackend.modules.branch.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.shared.enums.BranchType;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZoneId;

// Entidad base de sucursal con herencia SINGLE_TABLE
@Entity
@Table(name = "branches")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "branch_type", nullable = false)
    private BranchType branchType;

    /**
     * Zona horaria IANA de la sucursal, p. ej. {@code America/Guayaquil}.
     * <p>
     * Define qué es "hoy" y qué son "las 9:00" <em>aquí</em>. Con una empresa
     * ecuatoriana y otra colombiana en la misma instalación, la zona del servidor
     * no sirve para nada que decida disponibilidad.
     * <p>
     * La valida una clave foránea contra el catálogo {@code timezones}, poblado
     * desde {@code pg_timezone_names}: una expresión regular rechazaría
     * {@code UTC} y aceptaría {@code Marte/Olympus}.
     */
    @Column(nullable = false)
    private String timezone;

    /**
     * La zona como {@link ZoneId}, para resolver horas de pared contra instantes.
     * <p>
     * {@code @JsonIgnore} no es cosmetico: sin el, Jackson lo trata como la
     * propiedad {@code zoneId} al serializar la entidad y revienta con 500 si
     * {@code timezone} aun no esta puesto.
     */
    @Transient
    @JsonIgnore
    public ZoneId getZoneId() {
        return ZoneId.of(timezone);
    }

    @JsonBackReference
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}