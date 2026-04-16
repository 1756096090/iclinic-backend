package com.iclinic.iclinicbackend.modules.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de respuesta para empresa ecuatoriana.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "EcuadorianCompanyResponse",
    description = "DTO de respuesta para empresa ecuatoriana"
)
public class EcuadorianCompanyResponseDto extends CompanyResponseDto {

    @Schema(description = "RUC ecuatoriano")
    private String ruc;
}

