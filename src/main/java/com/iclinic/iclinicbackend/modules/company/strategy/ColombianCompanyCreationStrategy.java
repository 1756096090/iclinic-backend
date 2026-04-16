package com.iclinic.iclinicbackend.modules.company.strategy;

import com.iclinic.iclinicbackend.modules.company.dto.CreateCompanyRequestDto;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.company.factory.CompanyFactory;
import com.iclinic.iclinicbackend.modules.company.repository.ColombianCompanyRepository;
import com.iclinic.iclinicbackend.shared.enums.CompanyType;
import com.iclinic.iclinicbackend.shared.exception.DuplicateCompanyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Estrategia para crear empresas colombianas.
 * Delega la construcción de la entidad a CompanyFactory (SRP).
 */
@Component
@RequiredArgsConstructor
public class ColombianCompanyCreationStrategy implements CompanyCreationStrategy {

    private final ColombianCompanyRepository colombianCompanyRepository;
    private final CompanyFactory companyFactory;

    @Override
    public String getCompanyType() {
        return CompanyType.COLOMBIAN.name();
    }

    @Override
    public void validateUniqueTaxId(String taxId) {
        if (colombianCompanyRepository.findByNit(taxId).isPresent()) {
            throw new DuplicateCompanyException("colombiana", taxId);
        }
    }

    @Override
    public Company createCompany(CreateCompanyRequestDto dto) {
        return companyFactory.buildColombian(dto.getName(), dto.getTaxId(), dto.getBranches());
    }
}
