package com.example.spring_boot_project_api.mapper;


import com.example.spring_boot_project_api.dto.request.RentalContractRequest;
import com.example.spring_boot_project_api.dto.response.RentalContractResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.model.RentalContract;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class RentalContractMapper {
    public RentalContractResponse toResponse(RentalContract contract) {
        if (contract == null) {
            return null;
        }
        return RentalContractResponse.builder()
                .id(contract.getId())
                .ownerId(contract.getOwnerId())
                .ownerFullName(contract.getOwnerFullName())
                .ownerGender(contract.getOwnerGender())
                .ownerDateOfBirth(contract.getOwnerDateOfBirth())
                .ownerNationality(contract.getOwnerNationality())
                .ownerNationalId(contract.getOwnerNationalId())
                .ownerAddress(contract.getOwnerAddress())
                .ownerHouseNumber(contract.getOwnerHouseNumber())
                .ownerStreetNumber(contract.getOwnerStreetNumber())
                .ownerSangkat(contract.getOwnerSangkat())
                .ownerKhan(contract.getOwnerKhan())
                .ownerCityProvince(contract.getOwnerCityProvince())
                .ownerPhone(contract.getOwnerPhone())
                .ownerOccupation(contract.getOwnerOccupation())
                .tenantHouseNumber(contract.getTenantHouseNumber())
                .tenantStreetNumber(contract.getTenantStreetNumber())
                .tenantSangkat(contract.getTenantSangkat())
                .tenantKhan(contract.getTenantKhan())
                .tenantCityProvince(contract.getTenantCityProvince())
                .houseHouseNumber(contract.getHouseHouseNumber())
                .houseStreetNumber(contract.getHouseStreetNumber())
                .houseSangkat(contract.getHouseSangkat())
                .houseKhan(contract.getHouseKhan())
                .houseCityProvince(contract.getHouseCityProvince())
                .contractNo(contract.getContractNo())
                .contractNumber(contract.getContractNumber())
                .roomId(contract.getRoomId())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .monthlyRent(contract.getMonthlyRent())
                .securityDeposit(contract.getSecurityDeposit())
                .dueDay(contract.getDueDay())
                .status(contract.getStatus())
                .notes(contract.getNotes())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .build();
    }
    public RentalContract toEntity(RentalContractRequest request) {
        return RentalContract.builder()
                .contractNo(request.getContractNo())
                .roomId(request.getRoomId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .monthlyRent(request.getMonthlyRent())
                .securityDeposit(request.getSecurityDeposit() != null ? request.getSecurityDeposit() : BigDecimal.ZERO)
                .dueDay(request.getDueDay() != null ? request.getDueDay() : 5)
                .status(request.getStatus() != null ? request.getStatus() : RentalContractStatus.DRAFT)
                .notes(request.getNotes())
                .build();
    }
    /**
     * Copies the editable contract terms onto an existing contract. The Party A
     * snapshot is deliberately not touched here: a contract keeps the signer
     * identity it was created with. Only the contract service re-snapshots, and
     * only when the contract actually changes owner.
     */
    public void updateEntityFromDto(RentalContractRequest request, RentalContract contract) {
        if (request == null || contract == null) {
            return;
        }
        contract.setContractNo(request.getContractNo());
        contract.setRoomId(request.getRoomId());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setMonthlyRent(request.getMonthlyRent());
        
        if (request.getSecurityDeposit() != null) {
            contract.setSecurityDeposit(request.getSecurityDeposit());
        }
        if (request.getDueDay() != null) {
            contract.setDueDay(request.getDueDay());
        }
        if (request.getStatus() != null) {
            contract.setStatus(request.getStatus());
        }
        
        contract.setNotes(request.getNotes());
    }
}

