package com.goldatech.authservice.domain.model;

public enum PlanType {
    PAYMENT_REQUEST("Payment Request"),
    PAYOUTS("Payouts"),
    RECURRING_PAYMENTS("Recurring Payments"),
    ENTERPRISE_FULL_ACCESS("Enterprise Full Access");


    private final String displayName;

    PlanType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
