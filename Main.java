package com.awt.donation;

import com.awt.donation.dao.DonationDao;
import com.awt.donation.dao.DonorDao;
import com.awt.donation.service.AuthService;
import com.awt.donation.service.DonationService;
import com.awt.donation.ui.ConsoleUI;
import com.awt.donation.util.InputUtil;

import java.util.Scanner;

/**
 * AWT Welfare Foundation - Donation Management System.
 * Run this class to start the program.
 */
public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in, "UTF-8");
        InputUtil input = new InputUtil(scanner);

        DonorDao donorDao = new DonorDao();
        DonationDao donationDao = new DonationDao();
        DonationService service = new DonationService(donorDao, donationDao);
        AuthService auth = new AuthService();

        ConsoleUI ui = new ConsoleUI(input, auth, service);
        try {
            ui.start();
        } catch (InputUtil.InputEndedException e) {
            System.out.println("\n\nInput closed. Exiting.");
        }
    }
}
