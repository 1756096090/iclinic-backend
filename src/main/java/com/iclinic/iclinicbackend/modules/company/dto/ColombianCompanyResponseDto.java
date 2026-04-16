package com.iclinic.iclinicbackend.modules.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de respuesta para empresa colombiana.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "ColombianCompanyResponse",
    description = "DTO de respuesta para empresa colombiana"
)
public class ColombianCompanyResponseDto extends CompanyResponseDto {

    @Schema(description = "NIT colombiano")
    private String nit;
}

