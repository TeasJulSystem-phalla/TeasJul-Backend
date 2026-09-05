package com.example.spring_boot_project_api.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.request.CreateUtilityReadingRequest;
import com.example.spring_boot_project_api.dto.request.MonthlyReadingBulkRequest;
import com.example.spring_boot_project_api.dto.response.BulkUtilityReadingResponse;
import com.example.spring_boot_project_api.dto.response.MonthlyReadingRowResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UtilityReadingResponse;

public interface UtilityReadingService {

    UtilityReadingResponse createReading(CreateUtilityReadingRequest request);

    PageResponse<UtilityReadingResponse> getReadings(Long meterId, LocalDate fromDate,
            LocalDate toDate, Pageable pageable);

    UtilityReadingResponse getReadingById(Long id);

    List<MonthlyReadingRowResponse> getMonthlySheet(Long houseId, Integer year, Integer month);

    BulkUtilityReadingResponse createReadingsBulk(MonthlyReadingBulkRequest request);
}
