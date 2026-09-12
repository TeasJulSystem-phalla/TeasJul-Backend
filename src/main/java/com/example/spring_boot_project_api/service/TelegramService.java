package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.model.Payment;

public interface TelegramService {

    /**
     * Sends a Telegram message when a payment is initiated (e.g. a tenant
     * starts an online payment). Best-effort: failures are logged and never
     * propagated to the caller.
     */
    void sendPaymentCreated(Payment payment);

    /**
     * Sends a Telegram message when a payment is successfully paid (manual
     * recording or ABA-confirmed online payment). Best-effort: failures are
     * logged and never propagated to the caller.
     */
    void sendPaymentSuccess(Payment payment);
}