package com.iclinic.iclinicbackend.modules.branch.service;

import com.iclinic.iclinicbackend.modules.branch.dto.BranchUnifiedResponseDto;
import com.iclinic.iclinicbackend.modules.branch.dto.ClinicBranchResponseDto;
import com.iclinic.iclinicbackend.modules.branch.dto.CreateBranchUnifiedRequestDto;
import com.iclinic.iclinicbackend.modules.branch.dto.CreateClinicBranchRequestDto;
import com.iclinic.iclinicbackend.modules.branch.dto.CreateHospitalBranchRequestDto;
import com.iclinic.iclinicbackend.modules.branch.dto.HospitalBranchResponseDto;
import com.iclinic.iclinicbackend.modules.branch.entity.Branch;

import java.util.List;

// Interfaz de servicio de sucursal
public interface BranchService {
    // Método unificado (recomendado)
    BranchUnifiedResponseDto createBranch(CreateBranchUnifiedRequestDto dto);
    BranchUnifiedResponseDto updateBranch(Long id, CreateBranchUnifiedRequestDto dto);
    List<BranchUnifiedResponseDto> getAllBranches();
    // Métodos específicos (compatibilidad)
    ClinicBranchResponseDto createClinicBranch(Long companyId, CreateClinicBranchRequestDto dto);
    HospitalBranchResponseDto createHospitalBranch(Long companyId, CreateHospitalBranchRequestDto dto);
    Branch findById(Long id);
    List<Branch> findByCompanyId(Long companyId);
    void deleteById(Long id);
}


