package com.awt.donation.service;

import com.awt.donation.util.FileUtil;
import com.awt.donation.util.Validator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin login. The password is never stored as plain text - only its SHA-256 hash
 * is saved in data/admin.txt. First run creates the default login: admin / admin123.
 */
public class AuthService {

    private static final String FILE = "admin.txt";
    private static final String DEFAULT_USER = "admin";
    private static final String DEFAULT_PASSWORD = "admin123";

    private final String username;
    private String passwordHash;

    public AuthService() {
        List<String> lines = FileUtil.readLines(FILE);
        String[] parts = lines.isEmpty() ? new String[0] : FileUtil.split(lines.get(0));
        if (parts.length >= 2) {
            username = parts[0];
            passwordHash = parts[1];
        } else {
            username = DEFAULT_USER;
            passwordHash = hash(DEFAULT_PASSWORD);
            save();
        }
    }

    public boolean login(String user, String password) {
        return username.equals(user)
                && MessageDigest.isEqual(
                        passwordHash.getBytes(StandardCharsets.UTF_8),
                        hash(password).getBytes(StandardCharsets.UTF_8));
    }

    public void changePassword(String oldPassword, String newPassword) {
        if (!login(username, oldPassword)) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        Validator.password(newPassword);
        passwordHash = hash(newPassword);
        save();
    }

    private void save() {
        List<String> lines = new ArrayList<>();
        lines.add(username + FileUtil.DELIMITER + passwordHash);
        FileUtil.writeLines(FILE, lines);
    }

    private static String hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(("AWT-salt::" + text).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available on this system", e);
        }
    }
}
