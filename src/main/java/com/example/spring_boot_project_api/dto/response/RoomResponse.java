package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.RoomStatus;

import lombok.Data;

@Data
public class RoomResponse {

    private Long id;
    private Long ownerId;
    private Long houseId;
    private Long roomTypeId;
    private String roomNumber;
    private String name;
    private Integer floorNumber;
    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;
    private Integer capacity;
    private RoomStatus status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
}
