package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row of the monthly meter-reading sheet. Rows are grouped by room on the
 * frontend; the previous reading is taken from the last reading before the
 * billing month and the current reading is the value recorded inside the month
 * (or null when the cell still needs to be filled in).
 */
public record MonthlyReadingRowResponse(
        Long roomId,
        String roomNumber,
        Long meterId,
        String meterNumber,
        Long utilityTypeId,
        String utilityTypeName,
        String utilityTypeUnit,
        Long readingId,
        BigDecimal previousReading,
        BigDecimal currentReading,
        BigDecimal usage,
        LocalDate readingDate
) {
}