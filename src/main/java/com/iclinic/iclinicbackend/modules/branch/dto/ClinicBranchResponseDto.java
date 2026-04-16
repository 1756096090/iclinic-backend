package com.iclinic.iclinicbackend.modules.branch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de respuesta para sucursal de clínica.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ClinicBranchResponse", description = "DTO de respuesta para sucursal de clínica")
public class ClinicBranchResponseDto extends BranchResponseDto {

    @Schema(description = "¿Tiene laboratorio?")
    private Boolean hasLaboratory;
}

