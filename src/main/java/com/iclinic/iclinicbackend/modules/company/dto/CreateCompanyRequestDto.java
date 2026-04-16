package com.iclinic.iclinicbackend.modules.company.dto;

import com.iclinic.iclinicbackend.modules.branch.dto.BranchRequestDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO genérico para crear empresas de cualquier tipo.
 * Reduce la duplicación de código al tener un solo DTO reutilizable.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateCompanyRequest", description = "DTO genérico para crear empresas")
public class CreateCompanyRequestDto {

    @NotBlank(message = "El nombre de la empresa es requerido")
    @Schema(description = "Nombre de la empresa", example = "Clinica Dental XYZ")
    private String name;

    @NotBlank(message = "El identificador fiscal es requerido")
    @Schema(
        description = "RUC para Ecuador (13 dígitos) o NIT para Colombia (9-10 dígitos)",
        example = "1712345678901"
    )
    private String taxId;

    @Valid
    @Schema(description = "Lista de sucursales")
    private List<BranchRequestDto> branches;

    @NotBlank(message = "El tipo de empresa es requerido")
    @Schema(
        description = "Tipo de empresa",
        allowableValues = {"ECUADORIAN", "COLOMBIAN"},
        example = "ECUADORIAN"
    )
    private String companyType;
}
