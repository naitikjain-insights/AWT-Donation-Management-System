package com.awt.donation.dao;

import com.awt.donation.model.Donor;
import com.awt.donation.util.FileUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Data Access Object for donors. Keeps all donors in memory and saves them to
 * data/donors.txt after every change.
 */
public class DonorDao {

    private static final String FILE = "donors.txt";

    private final List<Donor> donors = new ArrayList<>();

    public DonorDao() {
        load();
    }

    private void load() {
        donors.clear();
        for (String line : FileUtil.readLines(FILE)) {
            try {
                donors.add(Donor.fromRecord(line));
            } catch (RuntimeException e) {
                System.err.println("Skipping unreadable line in " + FILE + ": " + line);
            }
        }
    }

    private void save() {
        List<String> lines = new ArrayList<>();
        for (Donor donor : donors) {
            lines.add(donor.toRecord());
        }
        FileUtil.writeLines(FILE, lines);
    }

    private int nextId() {
        int max = 0;
        for (Donor donor : donors) {
            max = Math.max(max, donor.getId());
        }
        return max + 1;
    }

    /** Adds the donor, gives it a new id and saves. */
    public Donor add(Donor donor) {
        donor.setId(nextId());
        donors.add(donor);
        save();
        return donor;
    }

    public Optional<Donor> findById(int id) {
        for (Donor donor : donors) {
            if (donor.getId() == id) {
                return Optional.of(donor);
            }
        }
        return Optional.empty();
    }

    public Optional<Donor> findByPhone(String phone) {
        for (Donor donor : donors) {
            if (donor.getPhone().equals(phone)) {
                return Optional.of(donor);
            }
        }
        return Optional.empty();
    }

    /** Case-insensitive "name contains keyword" search. */
    public List<Donor> searchByName(String keyword) {
        String key = keyword.trim().toLowerCase(Locale.ROOT);
        List<Donor> result = new ArrayList<>();
        for (Donor donor : donors) {
            if (donor.getName().toLowerCase(Locale.ROOT).contains(key)) {
                result.add(donor);
            }
        }
        return result;
    }

    public List<Donor> getAll() {
        return new ArrayList<>(donors);
    }

    /** Saves changes made to a donor that is already in the list. */
    public boolean update(Donor donor) {
        for (int i = 0; i < donors.size(); i++) {
            if (donors.get(i).getId() == donor.getId()) {
                donors.set(i, donor);
                save();
                return true;
            }
        }
        return false;
    }

    public boolean delete(int id) {
        boolean removed = donors.removeIf(donor -> donor.getId() == id);
        if (removed) {
            save();
        }
        return removed;
    }
}
