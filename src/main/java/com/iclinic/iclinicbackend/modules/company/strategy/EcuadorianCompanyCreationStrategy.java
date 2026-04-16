package com.iclinic.iclinicbackend.modules.company.strategy;

import com.iclinic.iclinicbackend.modules.company.dto.CreateCompanyRequestDto;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.company.factory.CompanyFactory;
import com.iclinic.iclinicbackend.modules.company.repository.EcuadorianCompanyRepository;
import com.iclinic.iclinicbackend.shared.enums.CompanyType;
import com.iclinic.iclinicbackend.shared.exception.DuplicateCompanyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Estrategia para crear empresas ecuatorianas.
 * Delega la construcción de la entidad a CompanyFactory (SRP).
 */
@Component
@RequiredArgsConstructor
public class EcuadorianCompanyCreationStrategy implements CompanyCreationStrategy {

    private final EcuadorianCompanyRepository ecuadorianCompanyRepository;
    private final CompanyFactory companyFactory;

    @Override
    public String getCompanyType() {
        return CompanyType.ECUADORIAN.name();
    }

    @Override
    public void validateUniqueTaxId(String taxId) {
        if (ecuadorianCompanyRepository.findByRuc(taxId).isPresent()) {
            throw new DuplicateCompanyException("ecuatoriana", taxId);
        }
    }

    @Override
    public Company createCompany(CreateCompanyRequestDto dto) {
        return companyFactory.buildEcuadorian(dto.getName(), dto.getTaxId(), dto.getBranches());
    }
}

