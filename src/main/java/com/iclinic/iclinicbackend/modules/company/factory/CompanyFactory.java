package com.iclinic.iclinicbackend.modules.company.factory;

import com.iclinic.iclinicbackend.modules.branch.dto.BranchRequestDto;
import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.branch.factory.BranchFactory;
import com.iclinic.iclinicbackend.modules.company.entity.ColombianCompany;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany;
import com.iclinic.iclinicbackend.shared.enums.CompanyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Factory para construir entidades Company y delegar la creación de Branch a BranchFactory.
 * Centraliza la lógica de construcción evitando duplicación en Strategies o Service.
 */
@Component
@RequiredArgsConstructor
public class CompanyFactory {

    private final BranchFactory branchFactory;

    public EcuadorianCompany buildEcuadorian(String name, String ruc, List<BranchRequestDto> branches) {
        EcuadorianCompany company = new EcuadorianCompany();
        company.setName(name);
        company.setRuc(ruc);
        company.setCompanyType(CompanyType.ECUADORIAN);
        company.setBranches(createBranchesFromDto(branches, company));
        return company;
    }

    public ColombianCompany buildColombian(String name, String nit, List<BranchRequestDto> branches) {
        ColombianCompany company = new ColombianCompany();
        company.setName(name);
        company.setNit(nit);
        company.setCompanyType(CompanyType.COLOMBIAN);
        company.setBranches(createBranchesFromDto(branches, company));
        return company;
    }

    /**
     * Convierte una lista de BranchRequestDto en entidades Branch,
     * delegando a BranchFactory para evitar duplicar la lógica de discriminación.
     */
    public List<Branch> createBranchesFromDto(List<BranchRequestDto> branchDtos, Company company) {
        if (branchDtos == null) return new ArrayList<>();
        return branchDtos.stream()
                .map(dto -> branchFactory.createBranchFromDto(dto, company))
                .collect(Collectors.toList());
    }
}
