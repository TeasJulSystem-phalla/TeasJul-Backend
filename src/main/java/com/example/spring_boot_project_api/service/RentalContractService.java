package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.RentalContractRequest;
import com.example.spring_boot_project_api.dto.response.RentalContractResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;

import java.util.List;

public interface RentalContractService {
    RentalContractResponse createContract(RentalContractRequest request);

    RentalContractResponse getContractById(Long id);

    List<RentalContractResponse> getContractsByOwner(Long ownerId);

    List<RentalContractResponse> getContractsByRoom(Long roomId);

    List<RentalContractResponse> getContractsByStatus(RentalContractStatus status);

    RentalContractResponse updateContract(Long id, RentalContractRequest request);

    RentalContractResponse updateStatus(Long id, RentalContractStatus status);

    void deleteContract(Long id);
}
