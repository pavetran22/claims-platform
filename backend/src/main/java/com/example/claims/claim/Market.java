package com.example.claims.claim;

/** The six markets. Each claim is held in its market's local currency. */
public enum Market {
    MY("MYR"), SG("SGD"), HK("HKD"), TH("THB"), ID("IDR"), PH("PHP");

    private final String currency;

    Market(String currency) {
        this.currency = currency;
    }

    public String currency() {
        return currency;
    }
}
