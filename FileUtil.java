package com.awt.donation.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Small helpers for reading and writing the text data files. */
public final class FileUtil {

    /** Fields inside one record are separated by this character. */
    public static final String DELIMITER = "|";

    private FileUtil() {
    }

    /** Folder where donors.txt, donations.txt and admin.txt live (default: ./data). */
    public static Path dataDir() {
        return Paths.get(System.getProperty("awt.data", "data"));
    }

    /** Folder for generated output such as receipts and exports. */
    public static Path outputDir(String subFolder) {
        return Paths.get(System.getProperty("awt.out", "."), subFolder);
    }

    /** Reads all non-empty, non-comment lines of a data file. Missing file = empty list. */
    public static List<String> readLines(String fileName) {
        Path file = dataDir().resolve(fileName);
        List<String> result = new ArrayList<>();
        if (!Files.exists(file)) {
            return result;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String text = line.trim();
                if (!text.isEmpty() && !text.startsWith("#")) {
                    result.add(text);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
        return result;
    }

    /** Replaces the content of a data file. Writes to a temp file first so a crash cannot corrupt data. */
    public static void writeLines(String fileName, List<String> lines) {
        Path file = dataDir().resolve(fileName);
        Path temp = dataDir().resolve(fileName + ".tmp");
        try {
            Files.createDirectories(dataDir());
            Files.write(temp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + file, e);
        }
    }

    /** Removes characters that would break the one-line-per-record format. */
    public static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", "/").replace("\r", " ").replace("\n", " ").trim();
    }

    /** Splits a record line into its fields (keeps empty fields). */
    public static String[] split(String line) {
        return line.split("\\|", -1);
    }
}
