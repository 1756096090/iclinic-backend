package com.iclinic.iclinicbackend.modules.branch.dto;

import com.iclinic.iclinicbackend.shared.enums.BranchType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * DTO de respuesta unificado para sucursales.
 * Incluye branchType y los campos específicos de cada tipo (null si no aplica).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "BranchUnifiedResponse", description = "DTO de respuesta unificado para sucursales")
public class BranchUnifiedResponseDto {

    @Schema(description = "ID de la sucursal")
    private Long id;

    @Schema(description = "Nombre de la sucursal")
    private String name;

    @Schema(description = "Dirección de la sucursal")
    private String address;

    @Schema(description = "Tipo de sucursal")
    private BranchType branchType;

    @Schema(description = "Nombre del tipo en español")
    private String branchTypeDisplayName;

    @Schema(description = "ID de la empresa")
    private Long companyId;

    // CLINIC
    @Schema(description = "¿Tiene laboratorio? (solo CLINIC)")
    private Boolean hasLaboratory;

    // HOSPITAL
    @Schema(description = "Capacidad de camas (solo HOSPITAL)")
    private Integer bedCapacity;
}

