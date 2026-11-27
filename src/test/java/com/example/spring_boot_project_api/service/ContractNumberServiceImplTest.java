package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_project_api.model.ContractSequence;
import com.example.spring_boot_project_api.repository.ContractSequenceRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.service.impl.ContractNumberServiceImpl;

@ExtendWith(MockitoExtension.class)
class ContractNumberServiceImplTest {

    @Mock
    private ContractSequenceRepository sequenceRepository;

    @Mock
    private RentalContractRepository contractRepository;

    @InjectMocks
    private ContractNumberServiceImpl contractNumberService;

    @Test
    void nextContractNumber_whenSequenceExists_incrementsAndReturnsCurrent() {
        ContractSequence seq = new ContractSequence("RENTAL_CONTRACT", 5L);
        when(sequenceRepository.findBySequenceNameWithLock("RENTAL_CONTRACT"))
                .thenReturn(Optional.of(seq));

        Long allocated = contractNumberService.nextContractNumber();

        assertThat(allocated).isEqualTo(5L);
        assertThat(seq.getNextValue()).isEqualTo(6L);
        verify(sequenceRepository).save(seq);
    }

    @Test
    void nextContractNumber_whenSequenceNotExists_initializesFromMaxContractNumberPlusOne() {
        when(sequenceRepository.findBySequenceNameWithLock("RENTAL_CONTRACT"))
                .thenReturn(Optional.empty());
        when(contractRepository.findMaxContractNumber())
                .thenReturn(Optional.of(10L));
        when(sequenceRepository.save(any(ContractSequence.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Long allocated = contractNumberService.nextContractNumber();

        assertThat(allocated).isEqualTo(11L);
        verify(sequenceRepository, times(2)).save(any(ContractSequence.class));
    }

    @Test
    void nextContractNumber_whenSequenceAndContractsEmpty_initializesToOne() {
        when(sequenceRepository.findBySequenceNameWithLock("RENTAL_CONTRACT"))
                .thenReturn(Optional.empty());
        when(contractRepository.findMaxContractNumber())
                .thenReturn(Optional.empty());
        when(sequenceRepository.save(any(ContractSequence.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Long allocated = contractNumberService.nextContractNumber();

        assertThat(allocated).isEqualTo(1L);
        verify(sequenceRepository, times(2)).save(any(ContractSequence.class));
    }
}