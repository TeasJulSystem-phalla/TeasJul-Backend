package com.example.spring_boot_project_api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.spring_boot_project_api.enums.RentalContractStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;


@Entity
@jakarta.persistence.Table(name = "rental_contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalContract {
    
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
    
        @Column(name = "owner_id", nullable = false)
        @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_houses_owner"))
        private Long ownerId;
    
        @Column(name = "contract_no", nullable = false, length = 50)
        private String contractNo;
    
        @Column(name = "room_id", nullable = false)
        @JoinColumn(name = "room_id", nullable = false, foreignKey = @ForeignKey(name = "fk_room") )
        private Long roomId;
    
        @Column(name = "start_date", nullable = false)
        private LocalDate startDate;
    
        @Column(name = "end_date")
        private LocalDate endDate;
    
        @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 2)
        private BigDecimal monthlyRent;
    
        @Column(name = "security_deposit", nullable = false, precision = 12, scale = 2)
        private BigDecimal securityDeposit = BigDecimal.ZERO;
    
        @Column(name = "due_day", nullable = false)
        private Integer dueDay = 5;
    
        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        private RentalContractStatus status = RentalContractStatus.DRAFT;
    
        @Column(name = "notes", columnDefinition = "TEXT")
        private String notes;
    
        @CreationTimestamp
        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;
    
        @UpdateTimestamp
        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt;
}