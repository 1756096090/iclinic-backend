package com.iclinic.iclinicbackend.modules.company.dto;

import com.iclinic.iclinicbackend.shared.enums.CompanyType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * DTO de respuesta unificado para empresas.
 * Incluye companyType y el campo fiscal específico (ruc o nit).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "CompanyUnifiedResponse", description = "DTO de respuesta unificado para empresas")
public class CompanyUnifiedResponseDto {

    @Schema(description = "ID de la empresa")
    private Long id;

    @Schema(description = "Nombre de la empresa")
    private String name;

    @Schema(description = "Tipo de empresa")
    private CompanyType companyType;

    @Schema(description = "Nombre del tipo en español")
    private String companyTypeDisplayName;

    @Schema(description = "Nombre del identificador fiscal (RUC o NIT)")
    private String taxIdLabel;

    @Schema(description = "Valor del identificador fiscal")
    private String taxId;
}

