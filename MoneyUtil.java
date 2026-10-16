package com.awt.donation.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Formatting of rupee amounts (Indian digit grouping) and amount-in-words. */
public final class MoneyUtil {

    private static final String[] ONES = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen",
            "Eighteen", "Nineteen"};

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

    private MoneyUtil() {
    }

    /** 1234567.5 -> "Rs. 12,34,567.50" */
    public static String inr(double amount) {
        BigDecimal value = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
        String[] parts = value.abs().toPlainString().split("\\.");
        String digits = parts[0];

        String grouped;
        if (digits.length() <= 3) {
            grouped = digits;
        } else {
            String lastThree = digits.substring(digits.length() - 3);
            String rest = digits.substring(0, digits.length() - 3);
            StringBuilder sb = new StringBuilder();
            while (rest.length() > 2) {
                sb.insert(0, "," + rest.substring(rest.length() - 2));
                rest = rest.substring(0, rest.length() - 2);
            }
            sb.insert(0, rest);
            grouped = sb + "," + lastThree;
        }
        return "Rs. " + (value.signum() < 0 ? "-" : "") + grouped + "." + parts[1];
    }

    /** 2500.50 -> "Rupees Two Thousand Five Hundred and Paise Fifty Only" */
    public static String amountInWords(double amount) {
        long rupees = (long) amount;
        int paise = (int) Math.round((amount - rupees) * 100);
        if (paise == 100) {
            rupees++;
            paise = 0;
        }
        StringBuilder sb = new StringBuilder("Rupees ").append(words(rupees));
        if (paise > 0) {
            sb.append(" and Paise ").append(words(paise));
        }
        return sb.append(" Only").toString();
    }

    /** Number to words using the Indian system (thousand, lakh, crore). */
    private static String words(long n) {
        if (n == 0) {
            return "Zero";
        }
        StringBuilder sb = new StringBuilder();
        if (n >= 10000000L) {
            sb.append(words(n / 10000000L)).append(" Crore ");
            n %= 10000000L;
        }
        if (n >= 100000L) {
            sb.append(words(n / 100000L)).append(" Lakh ");
            n %= 100000L;
        }
        if (n >= 1000L) {
            sb.append(words(n / 1000L)).append(" Thousand ");
            n %= 1000L;
        }
        if (n >= 100L) {
            sb.append(ONES[(int) (n / 100L)]).append(" Hundred ");
            n %= 100L;
        }
        if (n > 0) {
            if (n < 20) {
                sb.append(ONES[(int) n]);
            } else {
                sb.append(TENS[(int) (n / 10)]);
                if (n % 10 > 0) {
                    sb.append(' ').append(ONES[(int) (n % 10)]);
                }
            }
        }
        return sb.toString().trim();
    }
}
