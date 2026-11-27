package com.example.spring_boot_project_api.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.model.ContractSequence;
import com.example.spring_boot_project_api.repository.ContractSequenceRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.service.ContractNumberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractNumberServiceImpl implements ContractNumberService {

    public static final String RENTAL_CONTRACT_SEQUENCE = "RENTAL_CONTRACT";

    private final ContractSequenceRepository sequenceRepository;
    private final RentalContractRepository contractRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long nextContractNumber() {
        ContractSequence sequence = sequenceRepository.findBySequenceNameWithLock(RENTAL_CONTRACT_SEQUENCE)
                .orElseGet(() -> {
                    long initial = contractRepository.findMaxContractNumber().orElse(0L) + 1;
                    log.info("Initializing contract sequence '{}' with start value {}", RENTAL_CONTRACT_SEQUENCE, initial);
                    return sequenceRepository.save(new ContractSequence(RENTAL_CONTRACT_SEQUENCE, initial));
                });

        long assigned = sequence.getNextValue();
        sequence.setNextValue(assigned + 1);
        sequenceRepository.save(sequence);
        log.debug("Allocated contract number: {}", assigned);
        return assigned;
    }
}
