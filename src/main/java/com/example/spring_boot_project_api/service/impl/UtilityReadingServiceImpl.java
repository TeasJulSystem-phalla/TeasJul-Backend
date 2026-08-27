package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateUtilityReadingRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UtilityReadingResponse;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.InvalidUtilityOperationException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.exception.UtilityMeterNotFoundException;
import com.example.spring_boot_project_api.exception.UtilityReadingNotFoundException;
import com.example.spring_boot_project_api.mapper.UtilityReadingMapper;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityMeter;
import com.example.spring_boot_project_api.model.UtilityReading;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UtilityMeterRepository;
import com.example.spring_boot_project_api.repository.UtilityReadingRepository;
import com.example.spring_boot_project_api.service.UtilityReadingService;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UtilityReadingServiceImpl implements UtilityReadingService {

    private final UtilityReadingRepository readingRepository;
    private final UtilityMeterRepository meterRepository;
    private final UserRepository userRepository;
    private final UtilityReadingMapper readingMapper;

    @Override
    @Transactional
    public UtilityReadingResponse createReading(CreateUtilityReadingRequest request) {
        Long ownerId = getCurrentOwner().getId();

        UtilityMeter meter = meterRepository.findByIdAndOwnerId(request.meterId(), ownerId)
                .orElseThrow(() -> UtilityMeterNotFoundException.forId(request.meterId()));

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
        Long ownerId = getCurrentOwner().getId();

        Specification<UtilityReading> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("meter").get("owner").get("id"), ownerId));
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
        Long ownerId = getCurrentOwner().getId();

        UtilityReading reading = readingRepository.findById(id)
                .orElseThrow(() -> UtilityReadingNotFoundException.forId(id));

        if (!reading.getMeter().getOwner().getId().equals(ownerId)) {
            throw UtilityReadingNotFoundException.forId(id);
        }

        return readingMapper.toResponse(reading);
    }

    private PageResponse<UtilityReadingResponse> toPageResponse(Page<UtilityReading> page) {
        List<UtilityReadingResponse> content = page.getContent().stream()
                .map(readingMapper::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private User getCurrentOwner() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }
}
