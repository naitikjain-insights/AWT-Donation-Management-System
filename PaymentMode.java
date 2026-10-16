package com.awt.donation.model;

/** The ways in which a donor can pay. */
public enum PaymentMode {
    CASH("Cash"),
    UPI("UPI"),
    CARD("Card"),
    CHEQUE("Cheque"),
    BANK_TRANSFER("Bank Transfer");

    private final String label;

    PaymentMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
