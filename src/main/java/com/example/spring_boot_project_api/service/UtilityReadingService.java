package com.example.spring_boot_project_api.service;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.request.CreateUtilityReadingRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UtilityReadingResponse;

public interface UtilityReadingService {

    UtilityReadingResponse createReading(CreateUtilityReadingRequest request);

    PageResponse<UtilityReadingResponse> getReadings(Long meterId, LocalDate fromDate,
            LocalDate toDate, Pageable pageable);

    UtilityReadingResponse getReadingById(Long id);
}
