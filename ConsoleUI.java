package com.awt.donation.ui;

import com.awt.donation.model.Donation;
import com.awt.donation.model.Donor;
import com.awt.donation.model.PaymentMode;
import com.awt.donation.service.AuthService;
import com.awt.donation.service.DonationService;
import com.awt.donation.service.DonationService.DonorSummary;
import com.awt.donation.util.DateUtil;
import com.awt.donation.util.FileUtil;
import com.awt.donation.util.InputUtil;
import com.awt.donation.util.MoneyUtil;
import com.awt.donation.util.ReceiptGenerator;
import com.awt.donation.util.Validator;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** The text menus the admin uses. All real work is delegated to DonationService. */
public class ConsoleUI {

    private final InputUtil in;
    private final AuthService auth;
    private final DonationService service;

    public ConsoleUI(InputUtil in, AuthService auth, DonationService service) {
        this.in = in;
        this.auth = auth;
        this.service = service;
    }

    // ================================================================== start

    public void start() {
        printBanner();
        if (!login()) {
            return;
        }
        mainMenu();
        System.out.println("\nThank you for using the AWT Donation Management System. Goodbye!");
    }

    private void printBanner() {
        System.out.println(repeat('=', 62));
        System.out.println("            AWT WELFARE FOUNDATION");
        System.out.println("          DONATION MANAGEMENT SYSTEM");
        System.out.println(repeat('=', 62));
    }

    private boolean login() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            String user = in.readLine("\nUsername: ");
            String password = in.readPassword("Password: ");
            if (auth.login(user, password)) {
                System.out.println("\nLogin successful. Welcome, " + user + "!");
                return true;
            }
            System.out.println("  ! Wrong username or password. Attempts left: " + (3 - attempt));
        }
        System.out.println("\nToo many failed attempts. Program closed.");
        return false;
    }

    /** Runs one menu action and shows a friendly message instead of crashing on errors. */
    private void safely(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("\n  [!] " + e.getMessage());
        } catch (UncheckedIOException e) {
            System.out.println("\n  [!] File problem: " + e.getMessage());
        }
    }

    // ================================================================== menus

    private void mainMenu() {
        while (true) {
            System.out.println("\n=============== MAIN MENU ===============");
            System.out.println(" 1. Donor Management");
            System.out.println(" 2. Donation Management");
            System.out.println(" 3. Reports");
            System.out.println(" 4. Change Password");
            System.out.println(" 0. Exit");
            switch (in.readInt("Enter choice: ", 0, 4)) {
                case 1: donorMenu(); break;
                case 2: donationMenu(); break;
                case 3: reportMenu(); break;
                case 4: safely(this::changePassword); break;
                default: return;
            }
        }
    }

    private void donorMenu() {
        while (true) {
            System.out.println("\n----------- DONOR MANAGEMENT -----------");
            System.out.println(" 1. Add new donor");
            System.out.println(" 2. View all donors");
            System.out.println(" 3. Search donor");
            System.out.println(" 4. Update donor");
            System.out.println(" 5. Delete donor");
            System.out.println(" 0. Back");
            switch (in.readInt("Enter choice: ", 0, 5)) {
                case 1: safely(this::addDonor); break;
                case 2: safely(() -> printDonors(service.getAllDonors())); break;
                case 3: safely(this::searchDonor); break;
                case 4: safely(this::updateDonor); break;
                case 5: safely(this::deleteDonor); break;
                default: return;
            }
        }
    }

    private void donationMenu() {
        while (true) {
            System.out.println("\n---------- DONATION MANAGEMENT ----------");
            System.out.println(" 1. Record new donation");
            System.out.println(" 2. View all donations");
            System.out.println(" 3. View donations of a donor");
            System.out.println(" 4. Donations between two dates");
            System.out.println(" 5. Print / save receipt");
            System.out.println(" 0. Back");
            switch (in.readInt("Enter choice: ", 0, 5)) {
                case 1: safely(this::addDonation); break;
                case 2: safely(() -> printDonations(service.getAllDonations())); break;
                case 3: safely(this::donationsOfDonor); break;
                case 4: safely(this::donationsBetweenDates); break;
                case 5: safely(this::receiptForDonation); break;
                default: return;
            }
        }
    }

    private void reportMenu() {
        while (true) {
            System.out.println("\n---------------- REPORTS ----------------");
            System.out.println(" 1. Collection summary");
            System.out.println(" 2. Collection by purpose");
            System.out.println(" 3. Collection by payment mode");
            System.out.println(" 4. Month-wise collection");
            System.out.println(" 5. Top 5 donors");
            System.out.println(" 6. Export all donations to CSV (Excel)");
            System.out.println(" 0. Back");
            switch (in.readInt("Enter choice: ", 0, 6)) {
                case 1: safely(this::printSummary); break;
                case 2: safely(() -> printBreakdown("Collection by Purpose", service.totalByPurpose())); break;
                case 3: safely(() -> printBreakdown("Collection by Payment Mode", service.totalByMode())); break;
                case 4: safely(this::printMonthWise); break;
                case 5: safely(this::printTopDonors); break;
                case 6: safely(this::exportCsv); break;
                default: return;
            }
        }
    }

    // ================================================================== donors

    private void addDonor() {
        header("Add New Donor");
        String name = in.readValidated("Name               : ", Validator::name);
        String phone = in.readValidated("Phone (10 digits)  : ", Validator::phone);
        String email = in.readOptionalValidated("Email (optional)   : ", Validator::emailOptional);
        String address = in.readOptionalValidated("Address (optional) : ", Validator::address);

        Donor donor = service.registerDonor(name, phone, email, address);
        System.out.println("\nDonor added successfully. Donor ID = " + donor.getId());
    }

    private void searchDonor() {
        header("Search Donor");
        System.out.println(" 1. By donor ID");
        System.out.println(" 2. By name");
        System.out.println(" 3. By phone number");
        int choice = in.readInt("Search by: ", 1, 3);

        List<Donor> result = new ArrayList<>();
        if (choice == 1) {
            Optional<Donor> donor = service.getDonor(in.readInt("Donor ID: ", 1, Integer.MAX_VALUE));
            if (donor.isPresent()) {
                result.add(donor.get());
            }
        } else if (choice == 2) {
            result.addAll(service.searchDonors(in.readLine("Name contains: ")));
        } else {
            Optional<Donor> donor = service.findDonorByPhone(in.readLine("Phone number: "));
            if (donor.isPresent()) {
                result.add(donor.get());
            }
        }
        printDonors(result);

        if (result.size() == 1) {
            Donor donor = result.get(0);
            int count = service.getDonationsByDonor(donor.getId()).size();
            System.out.println("Address : " + dash(donor.getAddress()));
            System.out.println("Donated : " + MoneyUtil.inr(service.totalForDonor(donor.getId()))
                    + " in " + count + " donation(s)");
        }
    }

    private void updateDonor() {
        header("Update Donor");
        int id = in.readInt("Donor ID to update: ", 1, Integer.MAX_VALUE);
        Donor donor = service.getDonor(id)
                .orElseThrow(() -> new IllegalArgumentException("No donor found with ID " + id));

        System.out.println("Press Enter to keep the current value.");
        String name = in.readOptionalValidated("Name [" + donor.getName() + "]: ", Validator::name);
        String phone = in.readOptionalValidated("Phone [" + donor.getPhone() + "]: ", Validator::phone);
        String email = in.readOptionalValidated("Email [" + dash(donor.getEmail()) + "]: ", Validator::emailOptional);
        String address = in.readOptionalValidated("Address [" + dash(donor.getAddress()) + "]: ", Validator::address);

        service.updateDonor(id,
                name.isEmpty() ? donor.getName() : name,
                phone.isEmpty() ? donor.getPhone() : phone,
                email.isEmpty() ? donor.getEmail() : email,
                address.isEmpty() ? donor.getAddress() : address);
        System.out.println("\nDonor updated successfully.");
    }

    private void deleteDonor() {
        header("Delete Donor");
        int id = in.readInt("Donor ID to delete: ", 1, Integer.MAX_VALUE);
        Donor donor = service.getDonor(id)
                .orElseThrow(() -> new IllegalArgumentException("No donor found with ID " + id));
        printDonors(java.util.Collections.singletonList(donor));

        if (in.confirm("Really delete this donor? (y/n): ")) {
            service.deleteDonor(id);
            System.out.println("\nDonor deleted.");
        } else {
            System.out.println("\nCancelled. Nothing was deleted.");
        }
    }

    private void printDonors(List<Donor> donors) {
        if (donors.isEmpty()) {
            System.out.println("\nNo donors found.");
            return;
        }
        String format = "%-5s %-24s %-12s %-26s %-10s%n";
        System.out.println();
        System.out.printf(format, "ID", "Name", "Phone", "Email", "Joined");
        System.out.println(repeat('-', 81));
        for (Donor d : donors) {
            System.out.printf(format, d.getId(), cut(d.getName(), 24), d.getPhone(),
                    cut(dash(d.getEmail()), 26), DateUtil.show(d.getRegisteredOn()));
        }
        System.out.println(repeat('-', 81));
        System.out.println(donors.size() + " donor(s)");
    }

    // ============================================================== donations

    private void addDonation() {
        header("Record New Donation");
        Donor donor = pickDonor();
        if (donor == null) {
            return;
        }
        System.out.println("Donor: " + donor.getName() + " (ID " + donor.getId() + ")");

        double amount = in.readAmount("Amount (Rs.)  : ");
        PaymentMode mode = pickMode();
        String purpose = pickPurpose();
        LocalDate date = in.readDate("Date (dd-MM-yyyy, Enter = today): ", LocalDate.now());
        String remarks = in.readLine("Remarks (optional): ");

        Donation donation = service.recordDonation(donor.getId(), amount, mode, purpose, date, remarks);
        System.out.println("\nDonation recorded. Receipt No: " + donation.getReceiptNo()
                + "  (Donation ID " + donation.getId() + ")");

        if (in.confirm("Print receipt now? (y/n): ")) {
            printReceipt(donation);
        }
    }

    private void donationsOfDonor() {
        header("Donations of a Donor");
        Donor donor = pickDonor();
        if (donor == null) {
            return;
        }
        System.out.println("Donor: " + donor.getName() + " (ID " + donor.getId() + ")");
        printDonations(service.getDonationsByDonor(donor.getId()));
    }

    private void donationsBetweenDates() {
        header("Donations Between Two Dates");
        LocalDate from = in.readDate("From date (dd-MM-yyyy): ", null);
        LocalDate to = in.readDate("To date   (dd-MM-yyyy): ", null);
        printDonations(service.getDonationsBetween(from, to));
    }

    private void receiptForDonation() {
        header("Print / Save Receipt");
        int id = in.readInt("Donation ID (see 'View all donations'): ", 1, Integer.MAX_VALUE);
        Donation donation = service.getDonation(id)
                .orElseThrow(() -> new IllegalArgumentException("No donation found with ID " + id));
        printReceipt(donation);
    }

    private void printReceipt(Donation donation) {
        Donor donor = service.getDonor(donation.getDonorId())
                .orElseThrow(() -> new IllegalStateException("Donor record of this donation is missing."));
        String text = ReceiptGenerator.build(donation, donor);
        System.out.println();
        System.out.println(text);
        Path saved = ReceiptGenerator.save(donation, text);
        System.out.println("Receipt saved to: " + saved.toAbsolutePath());
    }

    private void printDonations(List<Donation> donations) {
        if (donations.isEmpty()) {
            System.out.println("\nNo donations found.");
            return;
        }
        String format = "%-5s %-10s %-20s %16s %-13s %-17s%n";
        double total = 0;
        System.out.println();
        System.out.printf(format, "ID", "Date", "Donor", "Amount", "Mode", "Purpose");
        System.out.println(repeat('-', 86));
        for (Donation d : donations) {
            System.out.printf(format, d.getId(), DateUtil.show(d.getDate()),
                    cut(service.donorName(d.getDonorId()), 20), MoneyUtil.inr(d.getAmount()),
                    d.getMode().getLabel(), cut(d.getPurpose(), 17));
            total += d.getAmount();
        }
        System.out.println(repeat('-', 86));
        System.out.println(donations.size() + " donation(s), total " + MoneyUtil.inr(total));
    }

    /** Asks for a donor ID, or lets the user search by name first. Returns null if cancelled. */
    private Donor pickDonor() {
        System.out.println("Enter the Donor ID, or 0 to search by name.");
        int id = in.readInt("Donor ID: ", 0, Integer.MAX_VALUE);
        if (id == 0) {
            List<Donor> found = service.searchDonors(in.readLine("Name contains: "));
            printDonors(found);
            if (found.isEmpty()) {
                return null;
            }
            id = in.readInt("Donor ID (0 to cancel): ", 0, Integer.MAX_VALUE);
            if (id == 0) {
                return null;
            }
        }
        Optional<Donor> donor = service.getDonor(id);
        if (!donor.isPresent()) {
            System.out.println("\n  [!] No donor found with ID " + id);
            return null;
        }
        return donor.get();
    }

    private PaymentMode pickMode() {
        PaymentMode[] modes = PaymentMode.values();
        System.out.println("Payment mode:");
        for (int i = 0; i < modes.length; i++) {
            System.out.println("  " + (i + 1) + ". " + modes[i].getLabel());
        }
        return modes[in.readInt("Choose mode: ", 1, modes.length) - 1];
    }

    private String pickPurpose() {
        String[] purposes = DonationService.PURPOSES;
        System.out.println("Purpose:");
        for (int i = 0; i < purposes.length; i++) {
            System.out.println("  " + (i + 1) + ". " + purposes[i]);
        }
        return purposes[in.readInt("Choose purpose: ", 1, purposes.length) - 1];
    }

    // ================================================================= reports

    private void printSummary() {
        header("Collection Summary");
        List<Donation> all = service.getAllDonations();
        if (all.isEmpty()) {
            System.out.println("No donations recorded yet.");
            return;
        }
        double total = service.totalCollection();
        Donation highest = service.highestDonation();

        System.out.println("Registered donors   : " + service.getAllDonors().size());
        System.out.println("Total donations     : " + all.size());
        System.out.println("Total collection    : " + MoneyUtil.inr(total));
        System.out.println("Average donation    : " + MoneyUtil.inr(total / all.size()));
        System.out.println("Highest donation    : " + MoneyUtil.inr(highest.getAmount())
                + " by " + service.donorName(highest.getDonorId())
                + " on " + DateUtil.show(highest.getDate()));
        System.out.println("This month so far   : " + MoneyUtil.inr(service.totalForMonth(YearMonth.now())));
    }

    private void printMonthWise() {
        Map<String, Double> labelled = new LinkedHashMap<>();
        for (Map.Entry<YearMonth, Double> e : service.totalByMonth().entrySet()) {
            YearMonth month = e.getKey();
            labelled.put(month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    + " " + month.getYear(), e.getValue());
        }
        printBreakdown("Month-wise Collection", labelled);
    }

    private void printTopDonors() {
        header("Top 5 Donors");
        List<DonorSummary> top = service.topDonors(5);
        if (top.isEmpty()) {
            System.out.println("No donations recorded yet.");
            return;
        }
        String format = "%-5s %-26s %10s %18s%n";
        System.out.printf(format, "Rank", "Donor", "Donations", "Total");
        System.out.println(repeat('-', 62));
        int rank = 1;
        for (DonorSummary s : top) {
            System.out.printf(format, rank++, cut(s.getDonor().getName(), 26), s.getCount(),
                    MoneyUtil.inr(s.getTotal()));
        }
    }

    /** Prints a table with percentage and a small text bar (20 characters = 100%). */
    private <K> void printBreakdown(String title, Map<K, Double> data) {
        header(title);
        if (data.isEmpty()) {
            System.out.println("No donations recorded yet.");
            return;
        }
        double total = 0;
        for (double value : data.values()) {
            total += value;
        }
        for (Map.Entry<K, Double> e : data.entrySet()) {
            double percent = e.getValue() * 100.0 / total;
            System.out.printf("%-20s %18s %6.1f%%  %s%n", e.getKey(), MoneyUtil.inr(e.getValue()),
                    percent, repeat('#', (int) Math.round(percent / 5)));
        }
        System.out.println(repeat('-', 62));
        System.out.printf("%-20s %18s%n", "TOTAL", MoneyUtil.inr(total));
    }

    private void exportCsv() {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path file = FileUtil.outputDir("exports").resolve("donations_" + stamp + ".csv");
        int rows = service.exportDonationsCsv(file);
        System.out.println("\nExported " + rows + " donation(s) to:\n" + file.toAbsolutePath());
    }

    // ================================================================ settings

    private void changePassword() {
        header("Change Password");
        String oldPassword = in.readPassword("Current password : ");
        String newPassword = in.readPassword("New password     : ");
        String again = in.readPassword("Repeat new password: ");
        if (!newPassword.equals(again)) {
            throw new IllegalArgumentException("The two new passwords do not match.");
        }
        auth.changePassword(oldPassword, newPassword);
        System.out.println("\nPassword changed successfully.");
    }

    // ================================================================ helpers

    private void header(String title) {
        System.out.println("\n=== " + title + " ===");
    }

    private static String dash(String text) {
        return text == null || text.isEmpty() ? "-" : text;
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 2) + "..";
    }

    private static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
