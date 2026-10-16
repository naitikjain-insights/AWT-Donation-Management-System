package com.awt.donation.model;

import com.awt.donation.util.FileUtil;

import java.time.LocalDate;
import java.util.Locale;

/** One donation made by a donor. */
public class Donation {

    private int id;
    private final int donorId;
    private final double amount;
    private final LocalDate date;
    private final PaymentMode mode;
    private final String purpose;
    private final String remarks;

    public Donation(int id, int donorId, double amount, LocalDate date,
                    PaymentMode mode, String purpose, String remarks) {
        this.id = id;
        this.donorId = donorId;
        this.amount = amount;
        this.date = date;
        this.mode = mode;
        this.purpose = purpose;
        this.remarks = remarks;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDonorId() { return donorId; }
    public double getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public PaymentMode getMode() { return mode; }
    public String getPurpose() { return purpose; }
    public String getRemarks() { return remarks; }

    /** Receipt number such as AWT/2026/00007 (year of donation + donation id). */
    public String getReceiptNo() {
        return String.format(Locale.ROOT, "AWT/%d/%05d", date.getYear(), id);
    }

    public String toRecord() {
        return String.join(FileUtil.DELIMITER,
                String.valueOf(id),
                String.valueOf(donorId),
                String.format(Locale.ROOT, "%.2f", amount),
                date.toString(),
                mode.name(),
                FileUtil.clean(purpose),
                FileUtil.clean(remarks));
    }

    public static Donation fromRecord(String line) {
        String[] p = FileUtil.split(line);
        if (p.length < 7) {
            throw new IllegalArgumentException("Bad donation record: " + line);
        }
        return new Donation(
                Integer.parseInt(p[0]),
                Integer.parseInt(p[1]),
                Double.parseDouble(p[2]),
                LocalDate.parse(p[3]),
                PaymentMode.valueOf(p[4]),
                p[5],
                p[6]);
    }
}
