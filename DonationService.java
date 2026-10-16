package com.awt.donation.service;

import com.awt.donation.dao.DonationDao;
import com.awt.donation.dao.DonorDao;
import com.awt.donation.model.Donation;
import com.awt.donation.model.Donor;
import com.awt.donation.model.PaymentMode;
import com.awt.donation.util.Validator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Business logic of the application: validation rules, donor/donation operations
 * and all report calculations. The console UI only talks to this class.
 */
public class DonationService {

    public static final String[] PURPOSES = {
            "General Fund", "Education", "Food Distribution", "Medical Aid",
            "Clothing", "Shelter", "Other"};

    /** Total amount and number of donations of one donor (used for the "Top donors" report). */
    public static class DonorSummary {
        private final Donor donor;
        private double total;
        private int count;

        DonorSummary(Donor donor) {
            this.donor = donor;
        }

        public Donor getDonor() { return donor; }
        public double getTotal() { return total; }
        public int getCount() { return count; }
    }

    private final DonorDao donorDao;
    private final DonationDao donationDao;

    public DonationService(DonorDao donorDao, DonationDao donationDao) {
        this.donorDao = donorDao;
        this.donationDao = donationDao;
    }

    // ===================================================================== donors

    public Donor registerDonor(String name, String phone, String email, String address) {
        Validator.name(name);
        Validator.phone(phone);
        Validator.emailOptional(email);
        Validator.address(address);

        if (donorDao.findByPhone(phone.trim()).isPresent()) {
            throw new IllegalArgumentException("A donor with this phone number is already registered.");
        }
        Donor donor = new Donor(0, name.trim(), phone.trim(), safe(email), safe(address), LocalDate.now());
        return donorDao.add(donor);
    }

    public Donor updateDonor(int id, String name, String phone, String email, String address) {
        Donor donor = donorDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No donor found with ID " + id));

        Validator.name(name);
        Validator.phone(phone);
        Validator.emailOptional(email);
        Validator.address(address);

        Optional<Donor> samePhone = donorDao.findByPhone(phone.trim());
        if (samePhone.isPresent() && samePhone.get().getId() != id) {
            throw new IllegalArgumentException("Another donor already uses this phone number.");
        }
        donor.setName(name.trim());
        donor.setPhone(phone.trim());
        donor.setEmail(safe(email));
        donor.setAddress(safe(address));
        donorDao.update(donor);
        return donor;
    }

    /** A donor who has already made donations cannot be deleted - receipts must stay valid. */
    public void deleteDonor(int id) {
        if (!donorDao.findById(id).isPresent()) {
            throw new IllegalArgumentException("No donor found with ID " + id);
        }
        if (donationDao.existsForDonor(id)) {
            throw new IllegalStateException(
                    "This donor has donation records, so the donor cannot be deleted.");
        }
        donorDao.delete(id);
    }

    public List<Donor> getAllDonors() {
        return donorDao.getAll();
    }

    public Optional<Donor> getDonor(int id) {
        return donorDao.findById(id);
    }

    public Optional<Donor> findDonorByPhone(String phone) {
        return donorDao.findByPhone(phone.trim());
    }

    public List<Donor> searchDonors(String keyword) {
        return donorDao.searchByName(keyword);
    }

    public String donorName(int donorId) {
        return donorDao.findById(donorId).map(Donor::getName).orElse("(unknown)");
    }

    // ================================================================== donations

    public Donation recordDonation(int donorId, double amount, PaymentMode mode,
                                   String purpose, LocalDate date, String remarks) {
        if (!donorDao.findById(donorId).isPresent()) {
            throw new IllegalArgumentException("No donor found with ID " + donorId);
        }
        Validator.amount(amount);
        if (mode == null) {
            throw new IllegalArgumentException("Payment mode is required.");
        }
        if (purpose == null || purpose.trim().isEmpty()) {
            throw new IllegalArgumentException("Purpose is required.");
        }
        LocalDate when = (date == null) ? LocalDate.now() : date;
        if (when.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Donation date cannot be in the future.");
        }
        double rounded = Math.round(amount * 100.0) / 100.0;
        return donationDao.add(new Donation(0, donorId, rounded, when, mode, purpose.trim(), safe(remarks)));
    }

    public List<Donation> getAllDonations() {
        return donationDao.getAll();
    }

    public Optional<Donation> getDonation(int id) {
        return donationDao.findById(id);
    }

    public List<Donation> getDonationsByDonor(int donorId) {
        return donationDao.findByDonorId(donorId);
    }

    /** Donations from "from" to "to", both dates included. */
    public List<Donation> getDonationsBetween(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'From' date must not be after 'To' date.");
        }
        List<Donation> result = new ArrayList<>();
        for (Donation donation : donationDao.getAll()) {
            if (!donation.getDate().isBefore(from) && !donation.getDate().isAfter(to)) {
                result.add(donation);
            }
        }
        return result;
    }

    // =================================================================== reports

    public double totalCollection() {
        return sum(donationDao.getAll());
    }

    public double totalForDonor(int donorId) {
        return sum(donationDao.findByDonorId(donorId));
    }

    public double totalForMonth(YearMonth month) {
        return totalByMonth().getOrDefault(month, 0.0);
    }

    /** The biggest single donation, or null when there are no donations. */
    public Donation highestDonation() {
        Donation best = null;
        for (Donation donation : donationDao.getAll()) {
            if (best == null || donation.getAmount() > best.getAmount()) {
                best = donation;
            }
        }
        return best;
    }

    public Map<String, Double> totalByPurpose() {
        Map<String, Double> result = new TreeMap<>();
        for (Donation donation : donationDao.getAll()) {
            result.merge(donation.getPurpose(), donation.getAmount(), Double::sum);
        }
        return result;
    }

    public Map<PaymentMode, Double> totalByMode() {
        Map<PaymentMode, Double> result = new EnumMap<>(PaymentMode.class);
        for (Donation donation : donationDao.getAll()) {
            result.merge(donation.getMode(), donation.getAmount(), Double::sum);
        }
        return result;
    }

    /** Month-wise totals in calendar order (oldest month first). */
    public Map<YearMonth, Double> totalByMonth() {
        Map<YearMonth, Double> result = new TreeMap<>();
        for (Donation donation : donationDao.getAll()) {
            result.merge(YearMonth.from(donation.getDate()), donation.getAmount(), Double::sum);
        }
        return result;
    }

    /** The donors who gave the most, highest first. */
    public List<DonorSummary> topDonors(int limit) {
        Map<Integer, DonorSummary> byDonor = new HashMap<>();
        for (Donation donation : donationDao.getAll()) {
            DonorSummary summary = byDonor.get(donation.getDonorId());
            if (summary == null) {
                Optional<Donor> donor = donorDao.findById(donation.getDonorId());
                if (!donor.isPresent()) {
                    continue;
                }
                summary = new DonorSummary(donor.get());
                byDonor.put(donation.getDonorId(), summary);
            }
            summary.total += donation.getAmount();
            summary.count++;
        }
        List<DonorSummary> list = new ArrayList<>(byDonor.values());
        list.sort(Comparator.comparingDouble(DonorSummary::getTotal).reversed());
        return list.size() > limit ? new ArrayList<>(list.subList(0, limit)) : list;
    }

    // ==================================================================== export

    /** Writes all donations to a CSV file that opens in Excel. Returns the number of rows written. */
    public int exportDonationsCsv(Path file) {
        List<String> lines = new ArrayList<>();
        lines.add("Receipt No,Date,Donor ID,Donor Name,Amount,Payment Mode,Purpose,Remarks");
        for (Donation d : donationDao.getAll()) {
            lines.add(String.join(",",
                    csv(d.getReceiptNo()),
                    csv(d.getDate().toString()),
                    String.valueOf(d.getDonorId()),
                    csv(donorName(d.getDonorId())),
                    String.format(Locale.ROOT, "%.2f", d.getAmount()),
                    csv(d.getMode().getLabel()),
                    csv(d.getPurpose()),
                    csv(d.getRemarks())));
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + file, e);
        }
        return lines.size() - 1;
    }

    // ================================================================== helpers

    private static double sum(List<Donation> donations) {
        double total = 0;
        for (Donation donation : donations) {
            total += donation.getAmount();
        }
        return Math.round(total * 100.0) / 100.0;
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }

    /** Quotes a CSV field and neutralises spreadsheet formulas such as =1+1 typed in remarks. */
    private static String csv(String value) {
        String text = value == null ? "" : value;
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            text = "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
