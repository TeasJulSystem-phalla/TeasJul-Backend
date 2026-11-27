package com.example.spring_boot_project_api.service;

public interface ContractNumberService {
    /**
     * Generates the next sequential, unique, and non-reusable contract number
     * using a concurrency-safe database sequence lock.
     *
     * @return next sequential contract number (1, 2, 3...)
     */
    Long nextContractNumber();
}
