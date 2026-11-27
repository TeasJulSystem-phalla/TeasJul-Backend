package com.example.spring_boot_project_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contract_sequences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractSequence {

    @Id
    @Column(name = "sequence_name", length = 50, nullable = false)
    private String sequenceName;

    @Column(name = "next_value", nullable = false)
    private Long nextValue;
}
