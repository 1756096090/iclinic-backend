package com.iclinic.iclinicbackend.modules.branch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateClinicBranchRequest", description = "DTO para crear una sucursal de clínica")
public class CreateClinicBranchRequestDto extends BranchRequestDto {

    @NotNull(message = "El indicador de laboratorio es requerido")
    @Schema(description = "¿Tiene laboratorio?", example = "true")
    private Boolean hasLaboratory;
}
