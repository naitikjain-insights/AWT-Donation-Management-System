package com.awt.donation.util;

import java.io.Console;
import java.time.LocalDate;
import java.util.Scanner;
import java.util.function.Consumer;

/** Reads and validates what the user types in the console. */
public class InputUtil {

    /** Thrown when the input stream is closed (for example Ctrl+D / Ctrl+Z). */
    public static class InputEndedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public InputEndedException() {
            super("Input stream closed");
        }
    }

    private final Scanner scanner;

    public InputUtil(Scanner scanner) {
        this.scanner = scanner;
    }

    private String rawLine(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        if (!scanner.hasNextLine()) {
            throw new InputEndedException();
        }
        return scanner.nextLine();
    }

    /** Reads one line and trims spaces around it. */
    public String readLine(String prompt) {
        return rawLine(prompt).trim();
    }

    /** Keeps asking until the validator accepts the text. */
    public String readValidated(String prompt, Consumer<String> validator) {
        while (true) {
            String value = readLine(prompt);
            try {
                validator.accept(value);
                return value;
            } catch (IllegalArgumentException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    /** Like readValidated, but pressing Enter (empty text) is allowed and returns "". */
    public String readOptionalValidated(String prompt, Consumer<String> validator) {
        while (true) {
            String value = readLine(prompt);
            if (value.isEmpty()) {
                return value;
            }
            try {
                validator.accept(value);
                return value;
            } catch (IllegalArgumentException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt);
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            if (max == Integer.MAX_VALUE) {
                System.out.println("  ! Please enter a whole number (minimum " + min + ").");
            } else {
                System.out.println("  ! Please enter a number between " + min + " and " + max + ".");
            }
        }
    }

    /** Reads a rupee amount like 2500 or 2,500.50 (at most 2 decimal places). */
    public double readAmount(String prompt) {
        while (true) {
            String text = readLine(prompt).replace(",", "");
            if (!text.matches("\\d+(\\.\\d{1,2})?")) {
                System.out.println("  ! Enter a valid amount, for example 2500 or 2500.50");
                continue;
            }
            try {
                double value = Double.parseDouble(text);
                Validator.amount(value);
                return value;
            } catch (IllegalArgumentException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    /** Reads a dd-MM-yyyy date. If defaultValue is not null, pressing Enter returns it. */
    public LocalDate readDate(String prompt, LocalDate defaultValue) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            try {
                return DateUtil.parse(text);
            } catch (IllegalArgumentException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    public boolean confirm(String prompt) {
        while (true) {
            String text = readLine(prompt).toLowerCase();
            if (text.equals("y") || text.equals("yes")) {
                return true;
            }
            if (text.equals("n") || text.equals("no")) {
                return false;
            }
            System.out.println("  ! Please type y or n.");
        }
    }

    /** Hides the typing when run from a real terminal; plain input inside some IDE consoles. */
    public String readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] chars = console.readPassword(prompt);
            if (chars == null) {
                throw new InputEndedException();
            }
            return new String(chars);
        }
        return rawLine(prompt);
    }
}
