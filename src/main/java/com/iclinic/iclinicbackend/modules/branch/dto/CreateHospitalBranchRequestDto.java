package com.iclinic.iclinicbackend.modules.branch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateHospitalBranchRequest", description = "DTO para crear una sucursal de hospital")
public class CreateHospitalBranchRequestDto extends BranchRequestDto {

    @NotNull(message = "La capacidad de camas es requerida")
    @Positive(message = "La capacidad de camas debe ser mayor a 0")
    @Schema(description = "Capacidad de camas", example = "50")
    private Integer bedCapacity;
}

