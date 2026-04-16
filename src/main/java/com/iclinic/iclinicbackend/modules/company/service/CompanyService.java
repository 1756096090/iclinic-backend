package com.iclinic.iclinicbackend.modules.company.service;

import com.iclinic.iclinicbackend.modules.company.dto.ColombianCompanyResponseDto;
import com.iclinic.iclinicbackend.modules.company.dto.CompanyUnifiedResponseDto;
import com.iclinic.iclinicbackend.modules.company.dto.CreateColombianCompanyRequestDto;
import com.iclinic.iclinicbackend.modules.company.dto.CreateCompanyUnifiedRequestDto;
import com.iclinic.iclinicbackend.modules.company.dto.CreateEcuadorianCompanyRequestDto;
import com.iclinic.iclinicbackend.modules.company.dto.EcuadorianCompanyResponseDto;
import com.iclinic.iclinicbackend.modules.company.entity.Company;

import java.util.List;

// Interfaz de servicio de empresa
public interface CompanyService {
    // Método unificado (recomendado)
    CompanyUnifiedResponseDto createCompany(CreateCompanyUnifiedRequestDto dto);
    // Métodos específicos (compatibilidad)
    EcuadorianCompanyResponseDto createEcuadorianCompany(CreateEcuadorianCompanyRequestDto dto);
    ColombianCompanyResponseDto createColombianCompany(CreateColombianCompanyRequestDto dto);
    Company findById(Long id);
    List<Company> findAll();
    void deleteById(Long id);
}