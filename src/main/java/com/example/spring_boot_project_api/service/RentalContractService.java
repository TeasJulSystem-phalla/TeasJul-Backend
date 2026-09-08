package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.RentalContractRequest;
import com.example.spring_boot_project_api.dto.response.ContractSummaryResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.RentalContractResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;

import java.util.List;

import org.springframework.data.domain.Pageable;

public interface RentalContractService {
    RentalContractResponse createContract(RentalContractRequest request);

    RentalContractResponse getContractById(Long id);

    PageResponse<RentalContractResponse> getContractsByOwner(Long ownerId, String search, RentalContractStatus status, Pageable pageable);

    List<RentalContractResponse> getContractsByRoom(Long roomId);

    PageResponse<RentalContractResponse> getContractsByStatus(RentalContractStatus status, Pageable pageable);

    ContractSummaryResponse getContractsSummary();

    RentalContractResponse updateContract(Long id, RentalContractRequest request);

    RentalContractResponse updateStatus(Long id, RentalContractStatus status);

    void deleteContract(Long id);
}
