package com.iclinic.iclinicbackend.modules.company.service;

import com.iclinic.iclinicbackend.modules.company.dto.ColombianCompanyResponseDto;
import com.iclinic.iclinicbackend.modules.company.dto.CompanyUnifiedResponseDto;
import com.iclinic.iclinicbackend.modules.company.dto.CreateColombianCompanyRequestDto;
import com.iclinic.iclinicbackend.modules.company.dto.CreateCompanyRequestDto;
import com.iclinic.iclinicbackend.modules.company.dto.CreateCompanyUnifiedRequestDto;
import com.iclinic.iclinicbackend.modules.company.dto.CreateEcuadorianCompanyRequestDto;
import com.iclinic.iclinicbackend.modules.company.dto.EcuadorianCompanyResponseDto;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.company.mapper.CompanyMapper;
import com.iclinic.iclinicbackend.modules.company.repository.CompanyRepository;
import com.iclinic.iclinicbackend.modules.company.strategy.CompanyCreationStrategy;
import com.iclinic.iclinicbackend.modules.company.strategy.CompanyCreationStrategyRegistry;
import com.iclinic.iclinicbackend.shared.exception.CompanyNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final CompanyCreationStrategyRegistry strategyRegistry;

    // ── Create (unified) ──────────────────────────────────────────────────────

    @Override
    public CompanyUnifiedResponseDto createCompany(CreateCompanyUnifiedRequestDto dto) {
        log.info("Creating {} company with taxId: {}", dto.getCompanyType(), dto.getTaxId());
        CompanyCreationStrategy strategy = strategyRegistry.getStrategy(dto.getCompanyType().name());
        strategy.validateUniqueTaxId(dto.getTaxId());
        // DTO unificado no lleva sucursales; branches=null → factory retorna lista vacía
        CreateCompanyRequestDto request = new CreateCompanyRequestDto(
                dto.getName(), dto.getTaxId(), null, dto.getCompanyType().name());
        Company saved = companyRepository.save(strategy.createCompany(request));
        return companyMapper.toUnifiedResponseDto(saved);
    }

    // ── Create (endpoints específicos por compatibilidad) ─────────────────────

    @Override
    public EcuadorianCompanyResponseDto createEcuadorianCompany(CreateEcuadorianCompanyRequestDto dto) {
        log.info("Creating ecuadorian company with RUC: {}", dto.getRuc());
        CompanyCreationStrategy strategy = strategyRegistry.getStrategy("ECUADORIAN");
        strategy.validateUniqueTaxId(dto.getRuc());
        CreateCompanyRequestDto request = new CreateCompanyRequestDto(
                dto.getName(), dto.getRuc(), dto.getBranches(), "ECUADORIAN");
        Company saved = companyRepository.save(strategy.createCompany(request));
        return companyMapper.toEcuadorianResponseDto(
                (com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany) saved);
    }

    @Override
    public ColombianCompanyResponseDto createColombianCompany(CreateColombianCompanyRequestDto dto) {
        log.info("Creating colombian company with NIT: {}", dto.getNit());
        CompanyCreationStrategy strategy = strategyRegistry.getStrategy("COLOMBIAN");
        strategy.validateUniqueTaxId(dto.getNit());
        CreateCompanyRequestDto request = new CreateCompanyRequestDto(
                dto.getName(), dto.getNit(), dto.getBranches(), "COLOMBIAN");
        Company saved = companyRepository.save(strategy.createCompany(request));
        return companyMapper.toColombianResponseDto(
                (com.iclinic.iclinicbackend.modules.company.entity.ColombianCompany) saved);
    }

    // ── Read / Delete ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Company> findAll() {
        return companyRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        companyRepository.delete(findById(id));
    }
}