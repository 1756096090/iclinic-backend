package com.iclinic.iclinicbackend.modules.branch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "BranchRequest", description = "DTO base para crear sucursales")
public abstract class BranchRequestDto {

    @NotBlank(message = "El nombre de la sucursal es requerido")
    @Schema(description = "Nombre de la sucursal", example = "Sucursal Centro")
    protected String name;

    @NotBlank(message = "La dirección es requerida")
    @Schema(description = "Dirección de la sucursal", example = "Calle Principal 123")
    protected String address;
}
