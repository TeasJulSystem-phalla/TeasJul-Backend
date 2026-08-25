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
        return new RentalContractResponse(
                contract.getId(),
                contract.getOwnerId(),
                contract.getContractNo(),
                contract.getRoomId(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getMonthlyRent(),
                contract.getSecurityDeposit(),
                contract.getDueDay(),
                contract.getStatus(),
                contract.getNotes(),
                contract.getCreatedAt(),
                contract.getUpdatedAt()
        );
    }
    public RentalContract toEntity(RentalContractRequest request) {
        return RentalContract.builder()
                .ownerId(request.getOwnerId())
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
    public void updateEntityFromDto(RentalContractRequest request, RentalContract contract) {
        if (request == null || contract == null) {
            return;
        }
        contract.setOwnerId(request.getOwnerId());
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

