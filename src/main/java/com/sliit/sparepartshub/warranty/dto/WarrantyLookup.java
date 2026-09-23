package com.sliit.sparepartshub.warranty.dto;

import com.sliit.sparepartshub.entity.RmaClaim;
import com.sliit.sparepartshub.entity.SerialNumber;

import java.time.LocalDate;

/**
 * Read-only view used by the warranty lookup screen. Eligibility is calculated
 * server-side from the real sale, product warranty period, serial state and
 * open-claim state; the browser never decides whether a claim is valid.
 */
public class WarrantyLookup {

    private final SerialNumber serial;
    private final boolean soldByStore;
    private final LocalDate expiry;
    private final String status;
    private final boolean eligible;
    private final String eligibilityMessage;
    private final RmaClaim openClaim;

    public WarrantyLookup(SerialNumber serial,
                          boolean soldByStore,
                          LocalDate expiry,
                          String status,
                          boolean eligible,
                          String eligibilityMessage,
                          RmaClaim openClaim) {
        this.serial = serial;
        this.soldByStore = soldByStore;
        this.expiry = expiry;
        this.status = status;
        this.eligible = eligible;
        this.eligibilityMessage = eligibilityMessage;
        this.openClaim = openClaim;
    }

    public SerialNumber getSerial() {
        return serial;
    }

    public boolean isSoldByStore() {
        return soldByStore;
    }

    public LocalDate getExpiry() {
        return expiry;
    }

    public String getStatus() {
        return status;
    }

    public boolean isEligible() {
        return eligible;
    }

    public String getEligibilityMessage() {
        return eligibilityMessage;
    }

    public RmaClaim getOpenClaim() {
        return openClaim;
    }
}
