package com.iclinic.iclinicbackend.modules.company.mapper;

import com.iclinic.iclinicbackend.modules.company.dto.ColombianCompanyResponseDto;
import com.iclinic.iclinicbackend.modules.company.dto.CompanyResponseDto;
import com.iclinic.iclinicbackend.modules.company.dto.CompanyUnifiedResponseDto;
import com.iclinic.iclinicbackend.modules.company.dto.EcuadorianCompanyResponseDto;
import com.iclinic.iclinicbackend.modules.company.entity.ColombianCompany;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyResponseDto toResponseDto(Company company) {
        if (company instanceof EcuadorianCompany ecuadorianCompany) {
            return toEcuadorianResponseDto(ecuadorianCompany);
        } else if (company instanceof ColombianCompany colombianCompany) {
            return toColombianResponseDto(colombianCompany);
        }
        throw new IllegalArgumentException("Tipo de empresa no soportado: " + company.getClass().getName());
    }

    public EcuadorianCompanyResponseDto toEcuadorianResponseDto(EcuadorianCompany company) {
        EcuadorianCompanyResponseDto dto = new EcuadorianCompanyResponseDto();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setRuc(company.getRuc());
        return dto;
    }

    public ColombianCompanyResponseDto toColombianResponseDto(ColombianCompany company) {
        ColombianCompanyResponseDto dto = new ColombianCompanyResponseDto();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setNit(company.getNit());
        return dto;
    }

    public CompanyUnifiedResponseDto toUnifiedResponseDto(Company company) {
        return CompanyUnifiedResponseDto.builder()
                .id(company.getId())
                .name(company.getName())
                .companyType(company.getCompanyType())
                .companyTypeDisplayName(company.getCompanyType().getDisplayName())
                .taxIdLabel(company.getCompanyType().getTaxIdName())
                .taxId(company.getTaxId())
                .build();
    }
}
