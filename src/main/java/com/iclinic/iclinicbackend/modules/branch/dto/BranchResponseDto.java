package com.iclinic.iclinicbackend.modules.branch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO base de respuesta para sucursales.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "BranchResponse", description = "DTO base de respuesta para sucursales")
public abstract class BranchResponseDto {

    @Schema(description = "ID de la sucursal")
    protected Long id;

    @Schema(description = "Nombre de la sucursal")
    protected String name;

    @Schema(description = "Dirección de la sucursal")
    protected String address;
}

