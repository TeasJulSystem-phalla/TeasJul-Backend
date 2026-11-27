package com.example.spring_boot_project_api.service.impl;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.OwnerContractInfoResponse;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.OwnerSettingService;

import lombok.RequiredArgsConstructor;

/**
 * Encapsulates the logic for freezing immutable address snapshots onto
 * {@link RentalContract} records at contract creation time.
 *
 * <p>Party A address is sourced from owner contract-party settings JSON.
 * Party B address is sourced from {@link Tenant} structured address fields.
 * Article 1 address is sourced from {@link House} structured address fields
 * (reusing {@code cityProvince}).</p>
 *
 * <p>Snapshot values are copied as-is without cross-party fallback or fabricated
 * components. These snapshots are never overwritten during normal contract updates.</p>
 */
@Component
@RequiredArgsConstructor
public class ContractAddressSnapshotHelper {

    private final OwnerSettingService ownerSettingService;
    private final UserRepository userRepository;

    /**
     * Freezes the owner's own contract information (Party A) onto the contract,
     * including legacy identity fields and additive structured address fields.
     */
    public void snapshotOwner(RentalContract contract, Long ownerId) {
        if (contract == null || ownerId == null) {
            return;
        }
        OwnerContractInfoResponse info = ownerSettingService.getContractInfo(ownerId);
        if (info == null) {
            info = new OwnerContractInfoResponse("", null, null, "", "", "", "", "", "", "", "", "", "");
        }
        User owner = userRepository.findById(ownerId).orElse(null);

        String fullName = firstNonBlank(info.fullName(), ownerFullName(owner));
        contract.setOwnerFullName(fullName);
        contract.setOwnerGender(blankToNull(info.gender() == null ? null : info.gender().name()));
        contract.setOwnerDateOfBirth(info.dateOfBirth());
        contract.setOwnerNationality(blankToNull(info.nationality()));
        contract.setOwnerNationalId(blankToNull(info.nationalId()));
        contract.setOwnerAddress(blankToNull(info.currentAddress()));
        contract.setOwnerPhone(firstNonBlank(info.phone(), owner == null ? null : owner.getPhone()));
        contract.setOwnerOccupation(blankToNull(info.occupation()));

        // Structured Party A snapshot
        contract.setOwnerHouseNumber(blankToNull(info.houseNumber()));
        contract.setOwnerStreetNumber(blankToNull(info.streetNumber()));
        contract.setOwnerSangkat(blankToNull(info.sangkat()));
        contract.setOwnerKhan(blankToNull(info.khan()));
        contract.setOwnerCityProvince(blankToNull(info.cityProvince()));
    }

    /**
     * Freezes the tenant's structured address (Party B) onto the contract.
     */
    public void snapshotTenant(RentalContract contract, Tenant tenant) {
        if (contract == null || tenant == null) {
            return;
        }
        contract.setTenantHouseNumber(blankToNull(tenant.getHouseNumber()));
        contract.setTenantStreetNumber(blankToNull(tenant.getStreetNumber()));
        contract.setTenantSangkat(blankToNull(tenant.getSangkat()));
        contract.setTenantKhan(blankToNull(tenant.getKhan()));
        contract.setTenantCityProvince(blankToNull(tenant.getCityProvince()));
    }

    /**
     * Freezes the rental property's structured address (Article 1) onto the contract.
     */
    public void snapshotHouse(RentalContract contract, House house) {
        if (contract == null || house == null) {
            return;
        }
        contract.setHouseHouseNumber(blankToNull(house.getHouseNumber()));
        contract.setHouseStreetNumber(blankToNull(house.getStreetNumber()));
        contract.setHouseSangkat(blankToNull(house.getSangkat()));
        contract.setHouseKhan(blankToNull(house.getKhan()));
        contract.setHouseCityProvince(blankToNull(house.getCityProvince()));
    }

    /**
     * Captures snapshots for all three parties (Owner, House, Tenant).
     */
    public void snapshotAll(RentalContract contract, Long ownerId, House house, Tenant tenant) {
        snapshotOwner(contract, ownerId);
        snapshotHouse(contract, house);
        snapshotTenant(contract, tenant);
    }

    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String ownerFullName(User owner) {
        if (owner == null) {
            return null;
        }
        String first = owner.getFirstName() == null ? "" : owner.getFirstName().trim();
        String last = owner.getLastName() == null ? "" : owner.getLastName().trim();
        return (first + " " + last).trim();
    }

    private static String firstNonBlank(String preferred, String fallback) {
        String a = preferred == null ? "" : preferred.trim();
        return a.isEmpty() ? blankToNull(fallback) : a;
    }
}
