package com.goldatech.paymentservice.domain.model;

public enum TelcoProvider {
    MTN("MTN"),
    AIRTEL_TIGO("Airtel-Tigo"),
    TELECEL("Telecel"),
    G_MONEY("G-Money");

    private final String displayName;

    TelcoProvider(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

