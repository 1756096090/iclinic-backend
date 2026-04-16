package com.iclinic.iclinicbackend.modules.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CompanyResponse", description = "DTO base de respuesta para empresas")
public abstract class CompanyResponseDto {

    @Schema(description = "ID de la empresa")
    private Long id;

    @Schema(description = "Nombre de la empresa")
    private String name;
}
