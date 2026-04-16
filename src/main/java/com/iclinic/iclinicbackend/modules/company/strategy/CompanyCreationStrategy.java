package com.iclinic.iclinicbackend.modules.company.strategy;

import com.iclinic.iclinicbackend.modules.company.dto.CreateCompanyRequestDto;
import com.iclinic.iclinicbackend.modules.company.entity.Company;

/**
 * Interfaz Strategy para crear empresas de diferentes tipos.
 * Permite agregar nuevos tipos de empresas sin modificar código existente.
 *
 * Responsabilidades:
 *  - Validar unicidad del tax ID (RUC / NIT)
 *  - Construir la entidad Company vía CompanyFactory
 * El mapeo a DTO es responsabilidad de CompanyMapper (Single Responsibility).
 */
public interface CompanyCreationStrategy {

    /** Retorna el tipo de empresa que maneja esta estrategia (e.g. "ECUADORIAN"). */
    String getCompanyType();

    /** Valida que el identificador fiscal no esté duplicado. */
    void validateUniqueTaxId(String taxId);

    /** Crea y retorna la entidad Company sin persistirla. */
    Company createCompany(CreateCompanyRequestDto dto);
}
