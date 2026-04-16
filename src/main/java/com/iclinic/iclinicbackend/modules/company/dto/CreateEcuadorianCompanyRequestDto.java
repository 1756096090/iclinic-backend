package com.iclinic.iclinicbackend.modules.company.dto;

import com.iclinic.iclinicbackend.modules.branch.dto.BranchRequestDto;
import com.iclinic.iclinicbackend.shared.validation.ValidRuc;
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
    name = "CreateEcuadorianCompanyRequest",
    description = "DTO para crear una empresa ecuatoriana"
)
public class CreateEcuadorianCompanyRequestDto {

    @NotBlank(message = "El nombre de la empresa es requerido")
    @Schema(description = "Nombre de la empresa", example = "Clinica Dental XYZ")
    private String name;

    @NotBlank(message = "El RUC es requerido")
    @ValidRuc
    @Schema(description = "RUC ecuatoriano (13 dígitos)", example = "1712345678901")
    private String ruc;

    @Valid
    @Schema(description = "Lista de sucursales")
    private List<BranchRequestDto> branches;
}