package com.awt.donation.dao;

import com.awt.donation.model.Donation;
import com.awt.donation.util.FileUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for donations. Saves to data/donations.txt after every change.
 */
public class DonationDao {

    private static final String FILE = "donations.txt";

    private final List<Donation> donations = new ArrayList<>();

    public DonationDao() {
        load();
    }

    private void load() {
        donations.clear();
        for (String line : FileUtil.readLines(FILE)) {
            try {
                donations.add(Donation.fromRecord(line));
            } catch (RuntimeException e) {
                System.err.println("Skipping unreadable line in " + FILE + ": " + line);
            }
        }
    }

    private void save() {
        List<String> lines = new ArrayList<>();
        for (Donation donation : donations) {
            lines.add(donation.toRecord());
        }
        FileUtil.writeLines(FILE, lines);
    }

    private int nextId() {
        int max = 0;
        for (Donation donation : donations) {
            max = Math.max(max, donation.getId());
        }
        return max + 1;
    }

    public Donation add(Donation donation) {
        donation.setId(nextId());
        donations.add(donation);
        save();
        return donation;
    }

    public Optional<Donation> findById(int id) {
        for (Donation donation : donations) {
            if (donation.getId() == id) {
                return Optional.of(donation);
            }
        }
        return Optional.empty();
    }

    public List<Donation> getAll() {
        return new ArrayList<>(donations);
    }

    public List<Donation> findByDonorId(int donorId) {
        List<Donation> result = new ArrayList<>();
        for (Donation donation : donations) {
            if (donation.getDonorId() == donorId) {
                result.add(donation);
            }
        }
        return result;
    }

    public boolean existsForDonor(int donorId) {
        for (Donation donation : donations) {
            if (donation.getDonorId() == donorId) {
                return true;
            }
        }
        return false;
    }
}
