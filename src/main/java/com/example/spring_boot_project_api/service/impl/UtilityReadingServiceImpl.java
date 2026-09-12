package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateUtilityReadingRequest;
import com.example.spring_boot_project_api.dto.request.MonthlyReadingBulkRequest;
import com.example.spring_boot_project_api.dto.response.BulkUtilityReadingResponse;
import com.example.spring_boot_project_api.dto.response.MonthlyReadingRowResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UtilityReadingResponse;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.InvalidUtilityOperationException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.exception.UtilityMeterNotFoundException;
import com.example.spring_boot_project_api.exception.UtilityReadingNotFoundException;
import com.example.spring_boot_project_api.mapper.UtilityReadingMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityMeter;
import com.example.spring_boot_project_api.model.UtilityReading;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UtilityMeterRepository;
import com.example.spring_boot_project_api.repository.UtilityReadingRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.UtilityReadingService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UtilityReadingServiceImpl implements UtilityReadingService {

    private final UtilityReadingRepository readingRepository;
    private final UtilityMeterRepository meterRepository;
    private final UserRepository userRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final HouseRepository houseRepository;
    private final RoomRepository roomRepository;
    private final UtilityTypeRepository utilityTypeRepository;
    private final UtilityReadingMapper readingMapper;
    private final SecurityUtils securityUtils;
    private final HouseAccessService houseAccessService;

    @Override
    @Transactional
    public UtilityReadingResponse createReading(CreateUtilityReadingRequest request) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_CREATE);
        UtilityMeter meter = resolveMeter(request.meterId());

        if (meter.getStatus() != UtilityMeterStatus.ACTIVE) {
            throw new InvalidUtilityOperationException(
                    "Cannot record a reading for a meter that is not active");
        }

        if (readingRepository.existsByMeterIdAndReadingDate(meter.getId(), request.readingDate())) {
            throw new DuplicateResourceException(
                    "A reading already exists for this meter on " + request.readingDate());
        }

        BigDecimal previousReading = readingRepository.findTopByMeterIdOrderByReadingDateDesc(meter.getId())
                .map(UtilityReading::getCurrentReading)
                .orElse(BigDecimal.ZERO);

        BigDecimal currentReading = request.currentReading();

        if (currentReading.compareTo(previousReading) < 0) {
            throw new InvalidUtilityOperationException(
                    "Current reading must not be less than the previous reading");
        }

        BigDecimal usage = currentReading.subtract(previousReading);

        UtilityReading reading = readingMapper.toEntity(request);
        reading.setMeter(meter);
        reading.setPreviousReading(previousReading);
        reading.setUsage(usage);

        return readingMapper.toResponse(readingRepository.save(reading));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UtilityReadingResponse> getReadings(Long meterId, LocalDate fromDate,
            LocalDate toDate, Pageable pageable) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_VIEW);
        List<Long> accessibleHouseIds = houseAccessService.getAccessibleHouseIds();

        Specification<UtilityReading> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("meter").get("houseId").in(accessibleHouseIds));
            if (meterId != null) {
                predicates.add(cb.equal(root.get("meter").get("id"), meterId));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("readingDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("readingDate"), toDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return toPageResponse(readingRepository.findAll(spec, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public UtilityReadingResponse getReadingById(Long id) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_VIEW);
        UtilityReading reading = readingRepository.findById(id)
                .orElseThrow(() -> UtilityReadingNotFoundException.forId(id));

        resolveMeter(reading.getMeter().getId());

        return readingMapper.toResponse(reading);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyReadingRowResponse> getMonthlySheet(Long houseId, Integer year, Integer month) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_VIEW);
        verifyHouseAccess(houseId);

        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = YearMonth.of(year, month).atEndOfMonth();

        Map<Long, Room> roomById = roomRepository.findByHouseId(houseId).stream()
                .collect(Collectors.toMap(Room::getId, room -> room));

        Map<Long, UtilityType> typeById = utilityTypeRepository
                .findByOwnerIdIn(houseAccessService.getScopedOwnerIds()).stream()
                .collect(Collectors.toMap(UtilityType::getId, type -> type));

        List<UtilityMeter> meters = meterRepository.findByHouseId(houseId).stream()
                .filter(meter -> meter.getStatus() == UtilityMeterStatus.ACTIVE)
                .toList();

        List<MonthlyReadingRowResponse> rows = new ArrayList<>();
        for (UtilityMeter meter : meters) {
            Room room = roomById.get(meter.getRoomId());
            if (room == null) {
                continue;
            }

            BigDecimal previous = readingRepository
                    .findTopByMeterIdAndReadingDateBeforeOrderByReadingDateDesc(meter.getId(), periodStart)
                    .map(UtilityReading::getCurrentReading)
                    .orElse(BigDecimal.ZERO);

            Optional<UtilityReading> recorded = readingRepository
                    .findTopByMeterIdAndReadingDateBetweenOrderByReadingDateDesc(meter.getId(), periodStart, periodEnd);

            rows.add(new MonthlyReadingRowResponse(
                    room.getId(),
                    room.getRoomNumber(),
                    meter.getId(),
                    meter.getMeterNumber(),
                    meter.getUtilityTypeId(),
                    typeById.containsKey(meter.getUtilityTypeId())
                            ? typeById.get(meter.getUtilityTypeId()).getName()
                            : null,
                    typeById.containsKey(meter.getUtilityTypeId())
                            ? typeById.get(meter.getUtilityTypeId()).getUnit()
                            : null,
                    recorded.map(UtilityReading::getId).orElse(null),
                    previous,
                    recorded.map(UtilityReading::getCurrentReading).orElse(null),
                    recorded.map(UtilityReading::getUsage).orElse(null),
                    recorded.map(UtilityReading::getReadingDate).orElse(null)
            ));
        }

        rows.sort(Comparator.comparing(MonthlyReadingRowResponse::roomNumber)
                .thenComparing(row -> row.utilityTypeName() == null ? "" : row.utilityTypeName()));

        return rows;
    }

    @Override
    @Transactional
    public BulkUtilityReadingResponse createReadingsBulk(MonthlyReadingBulkRequest request) {
        securityUtils.requirePermission(PermissionConstants.UTILITY_CREATE);
        verifyHouseAccess(request.houseId());

        LocalDate periodStart = LocalDate.of(request.billingYear(), request.billingMonth(), 1);
        LocalDate periodEnd = YearMonth.of(request.billingYear(), request.billingMonth()).atEndOfMonth();
        LocalDate readingDate = request.readingDate() != null ? request.readingDate() : periodEnd;

        if (readingDate.isBefore(periodStart) || readingDate.isAfter(periodEnd)) {
            throw new InvalidUtilityOperationException("Reading date must be within the billing month");
        }

        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<UtilityReading> toSave = new ArrayList<>();

        for (MonthlyReadingBulkRequest.MonthlyReadingRowRequest row : request.rows()) {
            UtilityMeter meter = resolveMeter(row.meterId());

            if (!meter.getHouseId().equals(request.houseId())) {
                throw new InvalidUtilityOperationException("Meter does not belong to the selected house");
            }
            if (meter.getStatus() != UtilityMeterStatus.ACTIVE) {
                throw new InvalidUtilityOperationException(
                        "Cannot record a reading for a meter that is not active");
            }

            BigDecimal previous = readingRepository
                    .findTopByMeterIdAndReadingDateBeforeOrderByReadingDateDesc(meter.getId(), periodStart)
                    .map(UtilityReading::getCurrentReading)
                    .orElse(BigDecimal.ZERO);

            BigDecimal current = row.currentReading();
            if (current.compareTo(previous) < 0) {
                throw new InvalidUtilityOperationException(
                        "Current reading must not be less than the previous reading (meter " + meter.getMeterNumber() + ")");
            }
            BigDecimal usage = current.subtract(previous);

            Optional<UtilityReading> existing = readingRepository
                    .findTopByMeterIdAndReadingDateBetweenOrderByReadingDateDesc(meter.getId(), periodStart, periodEnd);

            if (existing.isPresent()) {
                UtilityReading reading = existing.get();
                if (reading.getCurrentReading().compareTo(current) == 0) {
                    skipped++;
                    continue;
                }
                reading.setCurrentReading(current);
                reading.setUsage(usage);
                toSave.add(reading);
                updated++;
            } else {
                toSave.add(UtilityReading.builder()
                        .meter(meter)
                        .previousReading(previous)
                        .currentReading(current)
                        .usage(usage)
                        .readingDate(readingDate)
                        .build());
                created++;
            }
        }

        if (!toSave.isEmpty()) {
            readingRepository.saveAll(toSave);
        }

        return new BulkUtilityReadingResponse(created, updated, skipped);
    }

    private UtilityMeter resolveMeter(Long meterId) {
        Long currentOwnerId = getCurrentOwner().getId();

        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            List<Long> approvedHouseIds = userHouseAccessRepository.findHouseIdsByUserId(securityUtils.getCurrentUserId());
            UtilityMeter meter = meterRepository.findById(meterId)
                    .orElseThrow(() -> UtilityMeterNotFoundException.forId(meterId));
            if (!approvedHouseIds.contains(meter.getHouseId())) {
                throw UtilityMeterNotFoundException.forId(meterId);
            }
            return meter;
        }

        return meterRepository.findByIdAndOwnerId(meterId, currentOwnerId)
                .orElseThrow(() -> UtilityMeterNotFoundException.forId(meterId));
    }

    private PageResponse<UtilityReadingResponse> toPageResponse(Page<UtilityReading> page) {
        List<UtilityReadingResponse> content = page.getContent().stream()
                .map(readingMapper::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private void verifyHouseAccess(Long houseId) {
        Long currentUserId = securityUtils.getCurrentUserId();

        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> HouseNotFoundException.forId(houseId));

        // The house owner can always access its data (including anything created
        // by a manager assigned to the house); managers only see assigned houses.
        if (house.getOwner().getId().equals(currentUserId)) {
            return;
        }

        if (userHouseAccessRepository.existsByUserIdAndHouseId(currentUserId, houseId)) {
            return;
        }

        throw HouseNotFoundException.forId(houseId);
    }

    private User getCurrentOwner() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }
}
