package com.iclinic.iclinicbackend.modules.company.dto;

import com.iclinic.iclinicbackend.shared.enums.CompanyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO unificado para crear empresas de cualquier tipo.
 * El campo companyType determina el tipo y el campo taxId aplicable:
 *   - ECUADORIAN → taxId = RUC (13 dígitos)
 *   - COLOMBIAN  → taxId = NIT (9-10 dígitos)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "CreateCompanyUnifiedRequest", description = "DTO unificado para crear empresas")
public class CreateCompanyUnifiedRequestDto {

    @NotBlank(message = "El nombre de la empresa es requerido")
    @Schema(description = "Nombre de la empresa", example = "Clinica Dental XYZ")
    private String name;

    @NotNull(message = "El tipo de empresa es requerido")
    @Schema(
        description = "Tipo de empresa",
        allowableValues = {"ECUADORIAN", "COLOMBIAN"},
        example = "ECUADORIAN"
    )
    private CompanyType companyType;

    @NotBlank(message = "El identificador fiscal es requerido")
    @Schema(
        description = "RUC (13 dígitos) para ECUADORIAN, NIT (9-10 dígitos) para COLOMBIAN",
        example = "1712345678901"
    )
    private String taxId;
}

