package com.awt.donation.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** Date display / parsing in the dd-MM-yyyy format that is common in India. */
public final class DateUtil {

    public static final String PATTERN = "dd-MM-yyyy";

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT);

    private DateUtil() {
    }

    public static String show(LocalDate date) {
        return date == null ? "-" : date.format(FORMAT);
    }

    public static LocalDate parse(String text) {
        try {
            return LocalDate.parse(text.trim(), FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date. Use " + PATTERN + " (for example 25-12-2026).");
        }
    }
}
