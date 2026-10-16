# AWT Welfare Foundation - Donation Management System

A console-based **Core Java** application that helps AWT Welfare Foundation record donors and
donations, print receipts and see collection reports. Built as a first-year internship project
(Java Development).

- **Language:** Java (works on JDK 8 or higher; tested on JDK 21)
- **Storage:** plain text files in the `data/` folder (no database or external library needed)
- **Interface:** console menus

---

## 1. Features

| Module | What you can do |
|---|---|
| **Admin login** | Secure login (3 attempts), password stored as SHA-256 hash, change password |
| **Donor management** | Add, view all, search (by ID / name / phone), update, delete donors |
| **Donation management** | Record donation, view all, view by donor, filter between two dates |
| **Receipts** | Auto-numbered receipt (`AWT/2026/00007`), amount in words, saved as `receipts/Receipt_00007.txt` |
| **Reports** | Summary, collection by purpose, by payment mode, month-wise, top 5 donors |
| **Export** | All donations to a CSV file (opens in Excel) in `exports/` |

Smart rules built in:

- Phone must be a valid 10-digit Indian mobile number, and the same phone cannot be registered twice.
- Email is optional but checked if given. Amount must be > 0 (max Rs. 10 crore). Donation date cannot be in the future.
- A donor who already has donations **cannot be deleted** (so old receipts always stay valid).
- Amounts are shown in the Indian style: `Rs. 12,34,567.50`.
- Wrong input never crashes the program - it shows a message and asks again.

---

## 2. How to run

You need the **JDK** installed (`javac -version` should work).

**Windows:** double-click `run.bat`

**Linux / macOS / Git Bash:**
```bash
chmod +x run.sh
./run.sh
```

**Manual commands (any OS), from the project folder:**
```bash
mkdir out
javac -d out $(find src -name "*.java")        # Windows cmd: see run.bat
java -cp out com.awt.donation.Main
```

**In an IDE (IntelliJ / Eclipse / VS Code):** open the folder, mark `src` as the source root and
run `com.awt.donation.Main`. Set the working directory to the project folder so that `data/` is found.

> If the IDE console shows the password in plain text, that is normal - a real terminal hides it.

### Default login

| Username | Password |
|---|---|
| `admin` | `admin123` |

Change it from **Main Menu -> Change Password**.

### Sample data

The `data/` folder already contains 6 sample donors and 10 sample donations so you can see the reports
immediately. For a fresh, empty start, delete `data/donors.txt` and `data/donations.txt`.

---

## 3. Project structure

```
AWT-Donation-Management-System/
├── src/com/awt/donation/
│   ├── Main.java                  -> starts the program
│   ├── model/                     -> data classes
│   │   ├── Donor.java
│   │   ├── Donation.java
│   │   └── PaymentMode.java       (enum: Cash, UPI, Card, Cheque, Bank Transfer)
│   ├── dao/                       -> reads/writes the data files
│   │   ├── DonorDao.java
│   │   └── DonationDao.java
│   ├── service/                   -> business logic and reports
│   │   ├── DonationService.java
│   │   └── AuthService.java
│   ├── ui/
│   │   └── ConsoleUI.java         -> all menus and screens
│   └── util/                      -> helpers
│       ├── FileUtil.java, InputUtil.java, Validator.java
│       ├── MoneyUtil.java, DateUtil.java, ReceiptGenerator.java
├── data/                          -> donors.txt, donations.txt, admin.txt (created on first run)
├── receipts/                      -> generated receipts (created when used)
├── exports/                       -> CSV exports (created when used)
├── run.sh / run.bat
└── README.md
```

### Layered design (3-tier idea)

```
ConsoleUI  ->  DonationService  ->  DonorDao / DonationDao  ->  data/*.txt
 (screens)     (rules, reports)        (storage)                 (files)
```

Each layer only talks to the layer below it. Because of this, the text-file storage can later be
replaced by MySQL (JDBC) by changing **only the DAO classes**.

---

## 4. Data file format

Fields are separated by `|`. Lines starting with `#` are comments.

`data/donors.txt`
```
id|name|phone|email|address|registeredOn
1|Ramesh Kumar|9000000001|ramesh.kumar@example.com|12 MG Road, Sample City|2026-08-03
```

`data/donations.txt`
```
id|donorId|amount|date|mode|purpose|remarks
1|1|5000.00|2026-08-05|UPI|Education|School books drive
```

---

## 5. Java concepts used (useful for your viva)

| Concept | Where |
|---|---|
| Classes, objects, encapsulation (private fields + getters/setters) | `Donor`, `Donation` |
| Enum | `PaymentMode` |
| Collections: `List`, `ArrayList`, `Map`, `TreeMap`, `EnumMap`, `HashMap` | DAOs, reports in `DonationService` |
| `Optional` (safe "may be missing" result) | `findById`, `findByPhone` |
| Exception handling: `try/catch`, `IllegalArgumentException` / `IllegalStateException`, own exception class | whole project, `InputEndedException` |
| File handling with `java.nio.file` (`Files`, `Path`) | `FileUtil`, `ReceiptGenerator` |
| Regular expressions | `Validator` (phone, email, name) |
| Date & time API (`LocalDate`, `YearMonth`) | donations, month-wise report |
| Lambdas, method references, `Comparator` | sorting top donors, `Validator::name` |
| Nested / static classes | `DonorSummary`, `InputEndedException` |
| Generics (`<K> printBreakdown`) | `ConsoleUI` |
| Hashing (`MessageDigest` SHA-256) | `AuthService` |
| Packages and layered architecture | whole project |

---

## 6. Sample demo flow (for presentation)

1. Run the program and log in with `admin / admin123`.
2. **Donor Management -> Add new donor** (try a wrong phone number to show validation).
3. **Donation Management -> Record new donation**, say *yes* to print the receipt.
4. Open `receipts/` and show the saved receipt file.
5. **Reports** -> show summary, purpose-wise, month-wise and top donors.
6. **Reports -> Export** and open the CSV in Excel.
7. Try deleting a donor who has donations - the program refuses (data safety rule).

---

## 7. Possible viva questions

**Why did you split the code into model / dao / service / ui?**
Each class has one job. The UI shows screens, the service has the rules, the DAO handles storage.
If storage changes (files -> MySQL), only the DAO changes.

**Why is the password stored as a hash?**
A hash cannot be turned back into the password, so even if someone reads `admin.txt` they do not
see the real password.

**Why can't a donor with donations be deleted?**
Receipts and reports refer to the donor. Deleting would leave donations with no donor.

**What is `Optional` used for?**
A search may find nothing. `Optional` forces the caller to handle "not found" instead of getting a
`NullPointerException`.

**Why a `.tmp` file in `FileUtil.writeLines`?**
Data is first written to a temp file and then moved over the real file, so a crash in the middle of
saving cannot leave a half-written data file.

**How can this project be improved?**
MySQL with JDBC, a Swing/JavaFX screen, PDF receipts, multiple users with roles, or a Spring Boot web version.

---

## 8. Known limitations (honest notes)

- Money is stored as `double` for simplicity. Professional billing software uses `BigDecimal`.
- Passwords use a single SHA-256 hash; real systems use salted slow hashes such as bcrypt.
- Data files are loaded into memory at start, so the program is meant for one admin at a time.
- Receipts are plain text; they do not claim any tax-exemption (80G) details.

---

## 9. Future scope

1. Replace text files with **MySQL + JDBC** (only DAO classes change).
2. GUI using **Java Swing** or **JavaFX**.
3. PDF receipts (iText / Apache PDFBox) and e-mail/SMS receipt to donor.
4. Multiple users (admin / accountant) and activity log.
5. Web version using **Spring Boot**.
