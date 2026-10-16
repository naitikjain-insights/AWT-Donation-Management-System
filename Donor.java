package com.awt.donation.model;

import com.awt.donation.util.FileUtil;

import java.time.LocalDate;

/** A person who donates to AWT Welfare Foundation. */
public class Donor {

    private int id;
    private String name;
    private String phone;
    private String email;
    private String address;
    private final LocalDate registeredOn;

    public Donor(int id, String name, String phone, String email, String address, LocalDate registeredOn) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.registeredOn = registeredOn;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDate getRegisteredOn() { return registeredOn; }

    /** Converts this donor into one line of text for the data file. */
    public String toRecord() {
        return String.join(FileUtil.DELIMITER,
                String.valueOf(id),
                FileUtil.clean(name),
                FileUtil.clean(phone),
                FileUtil.clean(email),
                FileUtil.clean(address),
                registeredOn.toString());
    }

    /** Builds a donor from one line of the data file. */
    public static Donor fromRecord(String line) {
        String[] p = FileUtil.split(line);
        if (p.length < 6) {
            throw new IllegalArgumentException("Bad donor record: " + line);
        }
        return new Donor(Integer.parseInt(p[0]), p[1], p[2], p[3], p[4], LocalDate.parse(p[5]));
    }
}
