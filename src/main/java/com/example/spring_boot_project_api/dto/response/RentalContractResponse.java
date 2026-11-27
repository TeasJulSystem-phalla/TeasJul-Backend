package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalContractResponse {
    private Long id;
    private Long ownerId;

    /**
     * Party A identity as snapshotted onto the contract at creation time. Null
     * fields mean the owner left that detail blank (or the contract predates the
     * snapshot) - they are not back-filled from current settings on read.
     */
    private String ownerFullName;
    private String ownerGender;
    private LocalDate ownerDateOfBirth;
    private String ownerNationality;
    private String ownerNationalId;
    private String ownerAddress;
    private String ownerHouseNumber;
    private String ownerStreetNumber;
    private String ownerSangkat;
    private String ownerKhan;
    private String ownerCityProvince;
    private String ownerPhone;
    private String ownerOccupation;

    /**
     * Party B identity as snapshotted onto the contract.
     */
    private String tenantHouseNumber;
    private String tenantStreetNumber;
    private String tenantSangkat;
    private String tenantKhan;
    private String tenantCityProvince;

    /**
     * Article 1 rental-property address snapshot.
     */
    private String houseHouseNumber;
    private String houseStreetNumber;
    private String houseSangkat;
    private String houseKhan;
    private String houseCityProvince;

    private String contractNo;
    private Long contractNumber;
    private Long roomId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;
    private Integer dueDay;
    private RentalContractStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RentalContractResponse(
            Long id,
            Long ownerId,
            String ownerFullName,
            String ownerGender,
            LocalDate ownerDateOfBirth,
            String ownerNationality,
            String ownerNationalId,
            String ownerAddress,
            String ownerPhone,
            String ownerOccupation,
            String contractNo,
            Long roomId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal monthlyRent,
            BigDecimal securityDeposit,
            Integer dueDay,
            RentalContractStatus status,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, ownerId, ownerFullName, ownerGender, ownerDateOfBirth, ownerNationality,
                ownerNationalId, ownerAddress, null, null, null, null, null, ownerPhone,
                ownerOccupation, null, null, null, null, null, null, null, null, null, null,
                contractNo, null, roomId, startDate, endDate, monthlyRent, securityDeposit, dueDay,
                status, notes, createdAt, updatedAt);
    }
}
