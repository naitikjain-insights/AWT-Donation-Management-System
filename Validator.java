package com.awt.donation.util;

import java.util.regex.Pattern;

/**
 * Input validation rules. Every method throws IllegalArgumentException with a
 * friendly message when the value is not acceptable.
 */
public final class Validator {

    public static final double MAX_AMOUNT = 100_000_000.0; // Rs. 10 crore

    private static final Pattern NAME = Pattern.compile("^[\\p{L}][\\p{L} .'-]{1,59}$");
    private static final Pattern PHONE = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private Validator() {
    }

    public static void name(String value) {
        if (value == null || !NAME.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException(
                    "Name must be 2-60 characters (letters, spaces, . ' - only).");
        }
    }

    public static void phone(String value) {
        if (value == null || !PHONE.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException(
                    "Phone must be a 10-digit mobile number starting with 6, 7, 8 or 9.");
        }
    }

    /** Email is optional: empty is fine, but if given it must look like an email. */
    public static void emailOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (value.trim().length() > 60 || !EMAIL.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("Email looks invalid (example: name@example.com).");
        }
    }

    /** Address is optional, maximum 150 characters. */
    public static void address(String value) {
        if (value != null && value.trim().length() > 150) {
            throw new IllegalArgumentException("Address can be at most 150 characters.");
        }
    }

    public static void amount(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (value > MAX_AMOUNT) {
            throw new IllegalArgumentException("Amount is too large (maximum Rs. 10,00,00,000).");
        }
    }

    public static void password(String value) {
        if (value == null || value.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
    }
}
