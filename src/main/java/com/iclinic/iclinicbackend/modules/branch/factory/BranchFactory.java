package com.iclinic.iclinicbackend.modules.branch.factory;

import com.iclinic.iclinicbackend.modules.branch.dto.BranchRequestDto;
import com.iclinic.iclinicbackend.modules.branch.dto.CreateClinicBranchRequestDto;
import com.iclinic.iclinicbackend.modules.branch.dto.CreateHospitalBranchRequestDto;
import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.branch.entity.ClinicBranch;
import com.iclinic.iclinicbackend.modules.branch.entity.HospitalBranch;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.shared.enums.BranchType;
import org.springframework.stereotype.Component;

// Factory para crear instancias de Branch según el tipo especificado
@Component
public class BranchFactory {

    /**
     * Método polimórfico que delega al método concreto según el subtipo del DTO.
     * Evita duplicar la lógica de discriminación en CompanyFactory o cualquier otro lugar.
     */
    public Branch createBranchFromDto(BranchRequestDto dto, Company company) {
        if (dto instanceof CreateClinicBranchRequestDto clinic) {
            return createClinicBranch(clinic, company);
        } else if (dto instanceof CreateHospitalBranchRequestDto hospital) {
            return createHospitalBranch(hospital, company);
        }
        throw new IllegalArgumentException("Tipo de sucursal no soportado: " + dto.getClass().getName());
    }

    public ClinicBranch createClinicBranch(CreateClinicBranchRequestDto dto, Company company) {
        ClinicBranch branch = new ClinicBranch();
        branch.setName(dto.getName());
        branch.setAddress(dto.getAddress());
        branch.setHasLaboratory(dto.getHasLaboratory());
        branch.setBranchType(BranchType.CLINIC);
        branch.setCompany(company);
        return branch;
    }

    public HospitalBranch createHospitalBranch(CreateHospitalBranchRequestDto dto, Company company) {
        HospitalBranch branch = new HospitalBranch();
        branch.setName(dto.getName());
        branch.setAddress(dto.getAddress());
        branch.setBedCapacity(dto.getBedCapacity());
        branch.setBranchType(BranchType.HOSPITAL);
        branch.setCompany(company);
        return branch;
    }
}

