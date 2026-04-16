package com.iclinic.iclinicbackend.modules.branch.dto;

import com.iclinic.iclinicbackend.shared.enums.BranchType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO unificado para crear sucursales de cualquier tipo.
 * El campo branchType determina el tipo y los campos adicionales aplicables:
 *   - CLINIC  → hasLaboratory (requerido)
 *   - HOSPITAL → bedCapacity  (requerido)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "CreateBranchUnifiedRequest", description = "DTO unificado para crear sucursales")
public class CreateBranchUnifiedRequestDto {

    @NotNull(message = "El ID de la empresa es requerido")
    @Schema(description = "ID de la empresa a la que pertenece la sucursal", example = "1")
    private Long companyId;

    @NotBlank(message = "El nombre de la sucursal es requerido")
    @Schema(description = "Nombre de la sucursal", example = "Sucursal Norte")
    private String name;

    @NotBlank(message = "La dirección es requerida")
    @Schema(description = "Dirección de la sucursal", example = "Av. Principal 456")
    private String address;

    @NotNull(message = "El tipo de sucursal es requerido")
    @Schema(
        description = "Tipo de sucursal",
        allowableValues = {"CLINIC", "HOSPITAL"},
        example = "CLINIC"
    )
    private BranchType branchType;

    // --- Campo específico de CLINIC ---
    @Schema(description = "¿Tiene laboratorio? (solo para CLINIC)", example = "true")
    private Boolean hasLaboratory;

    // --- Campo específico de HOSPITAL ---
    @Schema(description = "Capacidad de camas (solo para HOSPITAL)", example = "50")
    private Integer bedCapacity;
}

