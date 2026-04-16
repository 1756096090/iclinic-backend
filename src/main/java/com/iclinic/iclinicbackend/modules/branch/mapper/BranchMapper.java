package com.iclinic.iclinicbackend.modules.branch.mapper;

import com.iclinic.iclinicbackend.modules.branch.dto.BranchResponseDto;
import com.iclinic.iclinicbackend.modules.branch.dto.BranchUnifiedResponseDto;
import com.iclinic.iclinicbackend.modules.branch.dto.ClinicBranchResponseDto;
import com.iclinic.iclinicbackend.modules.branch.dto.HospitalBranchResponseDto;
import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.branch.entity.ClinicBranch;
import com.iclinic.iclinicbackend.modules.branch.entity.HospitalBranch;
import org.springframework.stereotype.Component;

@Component
public class BranchMapper {

    public BranchResponseDto toResponseDto(Branch branch) {
        if (branch instanceof ClinicBranch clinicBranch) {
            return toClinicResponseDto(clinicBranch);
        } else if (branch instanceof HospitalBranch hospitalBranch) {
            return toHospitalResponseDto(hospitalBranch);
        }
        throw new IllegalArgumentException("Tipo de sucursal no soportado: " + branch.getClass().getName());
    }

    public ClinicBranchResponseDto toClinicResponseDto(ClinicBranch branch) {
        ClinicBranchResponseDto dto = new ClinicBranchResponseDto();
        dto.setId(branch.getId());
        dto.setName(branch.getName());
        dto.setAddress(branch.getAddress());
        dto.setHasLaboratory(branch.getHasLaboratory());
        return dto;
    }

    public HospitalBranchResponseDto toHospitalResponseDto(HospitalBranch branch) {
        HospitalBranchResponseDto dto = new HospitalBranchResponseDto();
        dto.setId(branch.getId());
        dto.setName(branch.getName());
        dto.setAddress(branch.getAddress());
        dto.setBedCapacity(branch.getBedCapacity());
        return dto;
    }

    public BranchUnifiedResponseDto toUnifiedResponseDto(Branch branch) {
        BranchUnifiedResponseDto.BranchUnifiedResponseDtoBuilder builder = BranchUnifiedResponseDto.builder()
                .id(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .branchType(branch.getBranchType())
                .branchTypeDisplayName(branch.getBranchType().getDisplayName())
                .companyId(branch.getCompany() != null ? branch.getCompany().getId() : null);

        if (branch instanceof ClinicBranch clinic) {
            builder.hasLaboratory(clinic.getHasLaboratory());
        } else if (branch instanceof HospitalBranch hospital) {
            builder.bedCapacity(hospital.getBedCapacity());
        }

        return builder.build();
    }
}
