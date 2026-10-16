package com.awt.donation.util;

import com.awt.donation.model.Donation;
import com.awt.donation.model.Donor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds the text of a donation receipt and saves it to the receipts folder. */
public final class ReceiptGenerator {

    private static final int WIDTH = 60;

    private ReceiptGenerator() {
    }

    public static String build(Donation donation, Donor donor) {
        StringBuilder sb = new StringBuilder();
        String thick = repeat('=', WIDTH);
        String thin = repeat('-', WIDTH);

        sb.append(thick).append('\n');
        sb.append(center("AWT WELFARE FOUNDATION")).append('\n');
        sb.append(center("DONATION RECEIPT")).append('\n');
        sb.append(thick).append('\n');
        row(sb, "Receipt No", donation.getReceiptNo());
        row(sb, "Date", DateUtil.show(donation.getDate()));
        sb.append(thin).append('\n');

        sb.append("Received with thanks from:\n");
        row(sb, "Donor ID", String.valueOf(donor.getId()));
        row(sb, "Name", donor.getName());
        row(sb, "Phone", donor.getPhone());
        if (!donor.getEmail().isEmpty()) {
            row(sb, "Email", donor.getEmail());
        }
        if (!donor.getAddress().isEmpty()) {
            row(sb, "Address", donor.getAddress());
        }
        sb.append(thin).append('\n');

        row(sb, "Amount", MoneyUtil.inr(donation.getAmount()));
        row(sb, "In words", MoneyUtil.amountInWords(donation.getAmount()));
        row(sb, "Payment Mode", donation.getMode().getLabel());
        row(sb, "Purpose", donation.getPurpose());
        if (!donation.getRemarks().isEmpty()) {
            row(sb, "Remarks", donation.getRemarks());
        }
        sb.append(thin).append('\n');

        sb.append("Thank you for your generous contribution. Your support\n");
        sb.append("helps us serve those who need it most.\n\n");
        sb.append("This is a computer-generated receipt and does not\n");
        sb.append("require a signature.\n");
        sb.append(thick);
        return sb.toString();
    }

    /** Saves the receipt as receipts/Receipt_00007.txt and returns the file path. */
    public static Path save(Donation donation, String text) {
        Path dir = FileUtil.outputDir("receipts");
        Path file = dir.resolve(String.format(Locale.ROOT, "Receipt_%05d.txt", donation.getId()));
        try {
            Files.createDirectories(dir);
            Files.write(file, text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save receipt", e);
        }
        return file;
    }

    // ------------------------------------------------------------ helpers

    private static void row(StringBuilder sb, String label, String value) {
        String prefix = String.format(Locale.ROOT, "%-13s: ", label);
        String indent = repeat(' ', prefix.length());
        List<String> parts = wrap(value, WIDTH - prefix.length());
        for (int i = 0; i < parts.size(); i++) {
            sb.append(i == 0 ? prefix : indent).append(parts.get(i)).append('\n');
        }
    }

    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            while (word.length() > width) {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= width) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (current.length() > 0 || lines.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static String center(String text) {
        int pad = Math.max(0, (WIDTH - text.length()) / 2);
        return repeat(' ', pad) + text;
    }

    private static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
