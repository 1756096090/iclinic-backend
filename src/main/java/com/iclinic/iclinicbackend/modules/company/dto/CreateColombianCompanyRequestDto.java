package com.iclinic.iclinicbackend.modules.company.dto;

import com.iclinic.iclinicbackend.modules.branch.dto.BranchRequestDto;
import com.iclinic.iclinicbackend.shared.validation.ValidNit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "CreateColombianCompanyRequest",
    description = "DTO para crear una empresa colombiana"
)
public class CreateColombianCompanyRequestDto {

    @NotBlank(message = "El nombre de la empresa es requerido")
    @Schema(description = "Nombre de la empresa", example = "Clinica Dental ABC")
    private String name;

    @NotBlank(message = "El NIT es requerido")
    @ValidNit
    @Schema(description = "NIT colombiano (9-10 dígitos)", example = "901234567")
    private String nit;

    @Valid
    @Schema(description = "Lista de sucursales")
    private List<BranchRequestDto> branches;
}