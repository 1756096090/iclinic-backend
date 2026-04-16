package com.iclinic.iclinicbackend.modules.branch.service;

import com.iclinic.iclinicbackend.modules.branch.dto.BranchUnifiedResponseDto;
import com.iclinic.iclinicbackend.modules.branch.dto.ClinicBranchResponseDto;
import com.iclinic.iclinicbackend.modules.branch.dto.CreateBranchUnifiedRequestDto;
import com.iclinic.iclinicbackend.modules.branch.dto.CreateClinicBranchRequestDto;
import com.iclinic.iclinicbackend.modules.branch.dto.CreateHospitalBranchRequestDto;
import com.iclinic.iclinicbackend.modules.branch.dto.HospitalBranchResponseDto;
import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.branch.entity.ClinicBranch;
import com.iclinic.iclinicbackend.modules.branch.entity.HospitalBranch;
import com.iclinic.iclinicbackend.modules.branch.mapper.BranchMapper;
import com.iclinic.iclinicbackend.modules.branch.repository.BranchRepository;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.company.repository.CompanyRepository;
import com.iclinic.iclinicbackend.shared.enums.BranchType;
import com.iclinic.iclinicbackend.shared.exception.BranchNotFoundException;
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
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;
    private final BranchMapper branchMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BranchUnifiedResponseDto> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(branchMapper::toUnifiedResponseDto)
                .toList();
    }

    @Override
    public BranchUnifiedResponseDto createBranch(CreateBranchUnifiedRequestDto dto) {
        log.info("Creating {} branch for company: {}", dto.getBranchType(), dto.getCompanyId());
        Company company = loadCompany(dto.getCompanyId());

        Branch branch = switch (dto.getBranchType()) {
            case CLINIC -> {
                if (dto.getHasLaboratory() == null) {
                    throw new IllegalArgumentException("hasLaboratory es requerido para sucursales de tipo CLINIC");
                }
                yield buildClinic(dto.getName(), dto.getAddress(), dto.getHasLaboratory(), company);
            }
            case HOSPITAL -> {
                if (dto.getBedCapacity() == null || dto.getBedCapacity() <= 0) {
                    throw new IllegalArgumentException("bedCapacity debe ser mayor a 0 para sucursales de tipo HOSPITAL");
                }
                yield buildHospital(dto.getName(), dto.getAddress(), dto.getBedCapacity(), company);
            }
        };

        return branchMapper.toUnifiedResponseDto(branchRepository.save(branch));
    }

    @Override
    public BranchUnifiedResponseDto updateBranch(Long id, CreateBranchUnifiedRequestDto dto) {
        log.info("Updating branch: {}", id);
        Branch existing = branchRepository.findById(id)
                .orElseThrow(() -> new BranchNotFoundException(id));
        existing.setName(dto.getName());
        existing.setAddress(dto.getAddress());
        if (existing instanceof ClinicBranch clinic && dto.getHasLaboratory() != null) {
            clinic.setHasLaboratory(dto.getHasLaboratory());
        }
        if (existing instanceof HospitalBranch hospital && dto.getBedCapacity() != null) {
            hospital.setBedCapacity(dto.getBedCapacity());
        }
        return branchMapper.toUnifiedResponseDto(branchRepository.save(existing));
    }

    @Override
    public ClinicBranchResponseDto createClinicBranch(Long companyId, CreateClinicBranchRequestDto dto) {
        log.info("Creating clinic branch for company: {}", companyId);
        Company company = loadCompany(companyId);
        ClinicBranch saved = (ClinicBranch) branchRepository.save(
                buildClinic(dto.getName(), dto.getAddress(), dto.getHasLaboratory(), company));
        return branchMapper.toClinicResponseDto(saved);
    }

    @Override
    public HospitalBranchResponseDto createHospitalBranch(Long companyId, CreateHospitalBranchRequestDto dto) {
        log.info("Creating hospital branch for company: {}", companyId);
        Company company = loadCompany(companyId);
        HospitalBranch saved = (HospitalBranch) branchRepository.save(
                buildHospital(dto.getName(), dto.getAddress(), dto.getBedCapacity(), company));
        return branchMapper.toHospitalResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Branch findById(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new BranchNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> findByCompanyId(Long companyId) {
        return branchRepository.findByCompanyId(companyId);
    }

    @Override
    public void deleteById(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new BranchNotFoundException(id));
        branchRepository.delete(branch);
    }

    private Company loadCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));
    }

    private ClinicBranch buildClinic(String name, String address, Boolean hasLaboratory, Company company) {
        ClinicBranch branch = new ClinicBranch();
        branch.setName(name);
        branch.setAddress(address);
        branch.setHasLaboratory(hasLaboratory);
        branch.setBranchType(BranchType.CLINIC);
        branch.setCompany(company);
        return branch;
    }

    private HospitalBranch buildHospital(String name, String address, Integer bedCapacity, Company company) {
        HospitalBranch branch = new HospitalBranch();
        branch.setName(name);
        branch.setAddress(address);
        branch.setBedCapacity(bedCapacity);
        branch.setBranchType(BranchType.HOSPITAL);
        branch.setCompany(company);
        return branch;
    }
}
