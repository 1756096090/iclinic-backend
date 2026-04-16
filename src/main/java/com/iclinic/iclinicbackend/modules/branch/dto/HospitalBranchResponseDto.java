package com.iclinic.iclinicbackend.modules.branch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de respuesta para sucursal de hospital.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "HospitalBranchResponse", description = "DTO de respuesta para sucursal de hospital")
public class HospitalBranchResponseDto extends BranchResponseDto {

    @Schema(description = "Capacidad de camas")
    private Integer bedCapacity;
}

