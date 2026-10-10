package lk.ijse.fleet;

import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class Main {
    
    private static final int BOX_WIDTH = 62;
    
    private static final int PROMPT_WIDTH = 24;

    
    private static final boolean ANIMATIONS = true;

   
    private static final boolean SHOW_FLEET_STATUS = true;

    private static final String DEMO_USERNAME = "Hasen";
    private static final String DEMO_PASSWORD = "2006";  
    private static final String DEMO_FULL_NAME = "Hasen";

    private static final double MAX_MONEY = 1_000_000_000.0;
    private static final Pattern DECIMAL = Pattern.compile("\\d+(\\.\\d+)?");

    private final FleetManager fleetManager;     
    private final UserManager userManager;      
    private final Scanner scanner;
    private SystemUser loggedInUser;            
    
    
    public Main() {
        fleetManager = new FleetManager();
        userManager = new UserManager();
        scanner = new Scanner(System.in);

        
        userManager.registerUser(DEMO_USERNAME, DEMO_PASSWORD, DEMO_FULL_NAME);

       
        fleetManager.setRentalEventListener(agreement ->
                System.out.println("[EVENT] New rental created: " + agreement.getAgreementId())
        );

        
        if (SHOW_FLEET_STATUS) {
            Thread statusThread = new Thread(new VehicleStatusMonitor(fleetManager));
            statusThread.setDaemon(true);
            statusThread.start();
        }

        Thread overdueThread = new Thread(new OverdueReminderWorker(fleetManager));
        overdueThread.setDaemon(true);
        overdueThread.start();
    }

    public static void main(String[] args) {
        try {
            new Main().run();
        } catch (NoSuchElementException e) {
            System.out.println();
            System.out.println("Input closed. Goodbye!");
        }
    }

    public void run() {
        while (true) {
            while (loggedInUser == null) {
                showAuthMenu();
            }
            showMainMenu();   
        }
    }

    // ================
    //  AUTH MENU
    // ================

    public void showAuthMenu() {
        clearConsole();
        System.out.println(boxBorder());
        System.out.println(boxCentered("FLEET RENTAL MANAGEMENT SYSTEM (FRMS)"));
        System.out.println(boxBorder());
        System.out.println(boxLeft("[1] Login"));
        System.out.println(boxLeft("[2] Register New User"));
        System.out.println(boxLeft("[0] Exit"));
        System.out.println(boxBorder());

        switch (readChoice()) {
            case 1:
                handleLogin();
                if (loggedInUser == null) {
                    pause();  
                }
                break;
            case 2:
                handleRegister();
                pause();
                break;
            case 0:
                System.out.println("Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid choice. Please select 0, 1 or 2.");
                pause();
        }
    }

    // ===============
    //  LOGIN PAGE 
    // ================

    private void handleLogin() {
        clearConsole();
        drawLoginBanner();

        String username = readNonEmpty(prompt("Username", 12));
        String password = readNonEmpty(prompt("Password", 12));
        System.out.println(boxBorder());
        System.out.println();

        showLoadingBar("Verifying credentials");
        SystemUser user = userManager.login(username, password);
        System.out.println();
        if (user != null) {
            loggedInUser = user;
            slowPrintln("  ACCESS GRANTED. Welcome, " + user.getFullName() + "!", 25);
            sleep(900);
        } else {
            System.out.println("  Invalid username or password.");
        }
    }

   
    private void drawLoginBanner() {
        sweepPrintln(boxBorder(), 6);
        sweepPrintln(boxCentered(""), 6);
        for (String row : blockText("LOGIN", 2)) {      
            sweepPrintln(boxCentered(row), 8);
        }
        sweepPrintln(boxCentered(""), 6);
        typeCentered("FLEET RENTAL MANAGEMENT SYSTEM", 25);
        sweepPrintln(boxBorder(), 6);
    }

    private void handleRegister() {
        clearConsole();
        System.out.println(boxBorder());
        typeCentered("R E G I S T E R   N E W   U S E R", 15);
        System.out.println(boxBorder());

        String username = readNonEmpty(prompt("Choose a username", 20));
        String password = readNonEmpty(prompt("Choose a password", 20));
        String fullName = readNonEmpty(prompt("Full name", 20));
        System.out.println(boxBorder());
        userManager.registerUser(username, password, fullName);  
        System.out.println(boxBorder());
    }

    // ===============
    //  MAIN MENU
    // ================
    public void showMainMenu() {
        clearConsole();
        slowPrintln("Welcome, " + loggedInUser.getFullName() + "! Loading fleet dashboard...", 20);
        sleep(900);

        while (loggedInUser != null) {
            clearConsole();
            printMainMenu();
            switch (readChoice()) {
                case 1:  runPage(this::addVehicle); break;
                case 2:  runPage(this::addCustomer); break;
                case 3:  runPage(this::viewAvailableVehicles); break;
                case 4:  runPage(this::createRental); break;
                case 5:  runPage(this::returnVehicle); break;
                case 6:  runPage(this::viewRevenueReport); break;
                case 7:  runPage(this::viewVehiclesByFuelType); break;
                case 8:  runPage(this::viewCombinedReport); break;
                case 9:  runPage(this::viewAllCustomers); break;
                case 10: runPage(this::viewAllAgreements); break;
                case 0:
                    System.out.println("Logged out. See you soon, " + loggedInUser.getFullName() + "!");
                    loggedInUser = null;
                    pause();
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number from the menu.");
                    pause();
            }
        }
    }

    
    private void runPage(Runnable action) {
        clearConsole();
        action.run();
        pause();
    }

    private void printMainMenu() {
        System.out.println(boxBorder());
        System.out.println(boxCentered("FRMS MAIN MENU | Logged in as: " + loggedInUser.getFullName()));
        System.out.println(boxBorder());
        System.out.println(boxLeft("[1]  Add Vehicle"));
        System.out.println(boxLeft("[2]  Add Customer"));
        System.out.println(boxLeft("[3]  View Available Vehicles"));
        System.out.println(boxLeft("[4]  Create Rental Agreement"));
        System.out.println(boxLeft("[5]  Return Vehicle"));
        System.out.println(boxLeft("[6]  View Revenue Report"));
        System.out.println(boxLeft("[7]  View Vehicles by Fuel Type"));
        System.out.println(boxLeft("[8]  View Combined Fleet & Customer Report"));
        System.out.println(boxLeft("[9]  View All Customers"));
        System.out.println(boxLeft("[10] View All Rental Agreements"));
        System.out.println(boxLeft("[0]  Logout"));
        System.out.println(boxBorder());
    }

    // ==================
    //  MENU ACTIONS 
    // ==================

    private void addVehicle() {
        drawPageHeader("ADD VEHICLE");
        System.out.println(boxLeft("[1] Car   [2] Van   [3] Truck"));
        int type = readInt(prompt("Vehicle type"), 1, 3);
        String reg = readNonEmpty(prompt("Registration number"));
        String brand = readNonEmpty(prompt("Brand"));
        String model = readNonEmpty(prompt("Model"));
        double rate = readDouble(prompt("Daily rate"), 0.01, MAX_MONEY);
        FuelType fuel = readFuelType();

        Vehicle vehicle;
        if (type == 1) {
            vehicle = new Car(reg, brand, model, rate, fuel);
        } else if (type == 2) {
            int capacity = readInt(prompt("Passenger capacity"), 1, 100);
            vehicle = new Van(reg, brand, model, rate, fuel, capacity);
        } else {
            double tons = readDouble(prompt("Cargo capacity (tons)"), 0.1, 1000.0);
            vehicle = new Truck(reg, brand, model, rate, fuel, tons);
        }
        fleetManager.addVehicle(vehicle);
        closeFormWith("[OK] Vehicle added: " + vehicle.getVehicleId());
    }

    private void addCustomer() {
        drawPageHeader("ADD CUSTOMER");
        System.out.println(boxLeft("[1] Individual   [2] Corporate"));
        int type = readInt(prompt("Customer type"), 1, 2);
        String name = readNonEmpty(prompt("Full name / company name"));
        String contact = readNonEmpty(prompt("Contact number"));

        Customer customer;
        if (type == 1) {
            int points = readInt(prompt("Loyalty points"), 0, Integer.MAX_VALUE);
            customer = new IndividualCustomer(name, contact, points);
        } else {
            double limit = readDouble(prompt("Credit limit"), 0.0, MAX_MONEY);
            customer = new CorporateCustomer(name, contact, limit);
        }
        fleetManager.addCustomer(customer);
        closeFormWith("[OK] Customer added: " + customer.getCustomerId());
    }

    private void viewAvailableVehicles() {
        drawPageHeader("AVAILABLE VEHICLES");
        List<Vehicle> available = fleetManager.getAvailableVehicles();
        if (available.isEmpty()) {
            printNote("No vehicles are currently available.");
            return;
        }
        System.out.println();
        printVehicleTable(available);
    }

    private void createRental() {
        drawPageHeader("CREATE RENTAL AGREEMENT");
        String vehicleId = readNonEmpty(prompt("Vehicle ID (VEH-1001)"));
        String customerId = readNonEmpty(prompt("Customer ID (CUS-1001)"));
        int days = readInt(prompt("Rental days"), 1, 3650);
        System.out.println(boxBorder());

        try {
            RentalAgreement agreement = fleetManager.createRentalAgreement(vehicleId, customerId, days);
            if (agreement != null) {
                System.out.println(agreement);
            } else {
                System.out.println(boxBorder());  
            }
        } catch (VehicleNotAvailableException | InsufficientCreditException e) {
            System.out.println("Rental failed: " + e.getMessage());
            System.out.println(boxBorder());
        }
    }

    private void returnVehicle() {
        drawPageHeader("RETURN VEHICLE");
        String agreementId = readNonEmpty(prompt("Agreement ID (AGR-1001)"));
        System.out.println(boxBorder());
        fleetManager.returnVehicle(agreementId);   
        System.out.println(boxBorder());
    }

    private void viewRevenueReport() {
        drawPageHeader("FLEET REVENUE REPORT");
        printNote("Total Revenue: " + money(fleetManager.generateRevenueReport()));
    }

    private void viewVehiclesByFuelType() {
        drawPageHeader("VEHICLES BY FUEL TYPE");
        FuelType type = readFuelType();
        List<Vehicle> list = fleetManager.getVehiclesByFuelType(type);
        if (list.isEmpty()) {
            closeFormWith("No vehicles found with fuel type " + type + ".");
            return;
        }
        System.out.println(boxBorder());
        System.out.println();
        printVehicleTable(list);
    }

    private void viewCombinedReport() {
        drawPageHeader("COMBINED FLEET & CUSTOMER REPORT");
        System.out.println();
        fleetManager.printAllReportables();   
    }

    private void viewAllCustomers() {
        drawPageHeader("ALL CUSTOMERS");
        List<Customer> list = fleetManager.getAllCustomers();
        if (list.isEmpty()) {
            printNote("No customers registered yet.");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (Customer c : list) {
            rows.add(new String[] { c.getCustomerId(), typeName(c), c.getFullName(),
                    c.getContactNumber(), customerDetails(c) });
        }
        System.out.println();
        printTable(new String[] { "ID", "Type", "Name", "Contact", "Details" },
                rows, new boolean[] { false, false, false, false, false });
    }

    private void viewAllAgreements() {
        drawPageHeader("ALL RENTAL AGREEMENTS");
        List<RentalAgreement> list = fleetManager.getAllAgreements();
        if (list.isEmpty()) {
            printNote("No rental agreements yet.");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (RentalAgreement a : list) {
            rows.add(new String[] { a.getAgreementId(),
                    a.getVehicle().getVehicleId() + " (" + a.getVehicle().getBrand() + " "
                            + a.getVehicle().getModel() + ")",
                    a.getCustomer().getCustomerId() + " (" + a.getCustomer().getFullName() + ")",
                    String.valueOf(a.getRentalDays()), money(a.getTotalCost()),
                    a.isReturned() ? "RETURNED" : "ACTIVE" });
        }
        System.out.println();
        printTable(new String[] { "Agreement", "Vehicle", "Customer", "Days", "Total Cost", "Status" },
                rows, new boolean[] { false, false, false, true, true, false });
    }

    // ===================
    //  OUTPUT HELPERS 
    // ===================

    
    private static boolean isRealTerminal() {
        Console console = System.console();
        if (console == null) {
            return false;
        }
        try {
            return (boolean) Console.class.getMethod("isTerminal").invoke(console);
        } catch (ReflectiveOperationException e) {
            return true;  
        }
    }

   
    private static void clearConsole() {
        try {
            if (isRealTerminal()) {
                if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
                    new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                } else {
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                }
                return;
            }
        } catch (IOException e) {
           
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.print("\n".repeat(40));
        System.out.flush();
    }

    private void pause() {
        System.out.print(System.lineSeparator() + "Press ENTER to continue...");
        scanner.nextLine();
    }

    
    private static void sleep(int millis) {
        if (!ANIMATIONS) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

   
    private static void slowPrintln(String text, int millisPerChar) {
        for (int i = 0; i < text.length(); i++) {
            System.out.print(text.charAt(i));
            System.out.flush();
            sleep(millisPerChar);
        }
        System.out.println();
    }

   
    private static void sweepPrintln(String text, int millisPerChunk) {
        final int chunk = 8;
        for (int i = 0; i < text.length(); i += chunk) {
            System.out.print(text.substring(i, Math.min(text.length(), i + chunk)));
            System.out.flush();
            sleep(millisPerChunk);
        }
        System.out.println();
    }

   
    private static void typeCentered(String text, int millisPerChar) {
        String t = fit(text);
        int left = (BOX_WIDTH - t.length()) / 2;
        System.out.print("|" + " ".repeat(left));
        for (int i = 0; i < t.length(); i++) {
            System.out.print(t.charAt(i));
            System.out.flush();
            sleep(millisPerChar);
        }
        System.out.println(" ".repeat(BOX_WIDTH - left - t.length()) + "|");
    }

   
    private static void showLoadingBar(String label) {
        final int total = 30;
        System.out.print("  " + label + " [");
        for (int i = 0; i < total; i++) {
            System.out.print("#");
            System.out.flush();
            sleep(35);
        }
        System.out.println("] 100%");
    }

   

    private static String boxBorder() {
        return "+" + "-".repeat(BOX_WIDTH) + "+";
    }

    private static String boxCentered(String text) {
        String t = fit(text);
        int left = (BOX_WIDTH - t.length()) / 2;
        return "|" + " ".repeat(left) + t + " ".repeat(BOX_WIDTH - left - t.length()) + "|";
    }

    private static String boxLeft(String text) {
        String t = fit("  " + text);
        return "|" + t + " ".repeat(BOX_WIDTH - t.length()) + "|";
    }

    private static String fit(String text) {
        return text.length() > BOX_WIDTH ? text.substring(0, BOX_WIDTH) : text;
    }

    
    private static void drawPageHeader(String title) {
        System.out.println(boxBorder());
        typeCentered(title, 8);
        System.out.println(boxBorder());
    }

    
    private static String prompt(String label) {
        return prompt(label, PROMPT_WIDTH);
    }

    private static String prompt(String label, int width) {
        return String.format("| %-" + width + "s : ", label);
    }

   
    private static void printBoxLines(String... messages) {
        for (String message : messages) {
            for (String line : wrap(message, BOX_WIDTH - 3)) {
                System.out.println(boxLeft(line));
            }
        }
    }

    
    private static void closeFormWith(String... messages) {
        System.out.println(boxBorder());
        printBoxLines(messages);
        System.out.println(boxBorder());
    }

    
    private static void printNote(String... messages) {
        printBoxLines(messages);
        System.out.println(boxBorder());
    }

    
    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            while (word.length() > width) {   
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (current.length() > 0 && current.length() + 1 + word.length() > width) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
        }
        if (current.length() > 0 || lines.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

  
    
    private static String[] glyph(char c) {
        switch (c) {
            case 'L': return new String[] { "#    ", "#    ", "#    ", "#    ", "#####" };
            case 'O': return new String[] { " ### ", "#   #", "#   #", "#   #", " ### " };
            case 'G': return new String[] { " ####", "#    ", "# ###", "#   #", " ### " };
            case 'I': return new String[] { "#####", "  #  ", "  #  ", "  #  ", "#####" };
            case 'N': return new String[] { "#   #", "##  #", "# # #", "#  ##", "#   #" };
            default:  throw new IllegalArgumentException("No glyph for '" + c + "'");
        }
    }

   
    private static String[] blockText(String word, int scale) {
        String[] rows = new String[5 * scale];
        for (int r = 0; r < 5; r++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < word.length(); i++) {
                if (i > 0) {
                    sb.append("  ");
                }
                for (char c : glyph(word.charAt(i))[r].toCharArray()) {
                    sb.append(String.valueOf(c).repeat(scale));
                }
            }
            for (int s = 0; s < scale; s++) {
                rows[r * scale + s] = sb.toString();
            }
        }
        return rows;
    }

   

    private static String money(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

   
    private static String typeName(Object o) {
        return o.getClass().getSimpleName().replace("Customer", "");
    }

    private static String vehicleDetails(Vehicle v) {
        if (v instanceof Van) {
            return ((Van) v).getPassengerCapacity() + " seats";
        }
        if (v instanceof Truck) {
            return ((Truck) v).getCargoCapacityTons() + " t cargo";
        }
        return "-";
    }

    private static String customerDetails(Customer c) {
        if (c instanceof IndividualCustomer) {
            return "Loyalty points: " + ((IndividualCustomer) c).getLoyaltyPoints();
        }
        if (c instanceof CorporateCustomer) {
            CorporateCustomer corporate = (CorporateCustomer) c;
            return String.format(Locale.US, "Credit limit: %,.2f | Outstanding: %,.2f",
                    corporate.getCreditLimit(), corporate.getOutstandingBalance());
        }
        return "-";
    }

    private void printVehicleTable(List<Vehicle> vehicles) {
        List<String[]> rows = new ArrayList<>();
        for (Vehicle v : vehicles) {
            rows.add(new String[] { v.getVehicleId(), typeName(v), v.getRegistrationNumber(),
                    v.getBrand() + " " + v.getModel(), money(v.getDailyRate()),
                    v.getFuelType().toString(), vehicleDetails(v), v.getStatus().toString() });
        }
        printTable(new String[] { "ID", "Type", "Reg No", "Vehicle", "Rate/Day", "Fuel", "Details", "Status" },
                rows, new boolean[] { false, false, false, false, true, false, false, false });
    }

    
    private static void printTable(String[] headers, List<String[]> rows, boolean[] rightAligned) {
        int cols = headers.length;
        int[] width = new int[cols];
        for (int i = 0; i < cols; i++) {
            width[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < cols; i++) {
                width[i] = Math.max(width[i], row[i].length());
            }
        }

        StringBuilder separator = new StringBuilder("+");
        for (int w : width) {
            separator.append("-".repeat(w + 2)).append('+');
        }

        System.out.println(separator);
        System.out.println(tableRow(headers, width, new boolean[cols]));
        System.out.println(separator);
        for (String[] row : rows) {
            System.out.println(tableRow(row, width, rightAligned));
        }
        System.out.println(separator);
    }

    private static String tableRow(String[] cells, int[] width, boolean[] rightAligned) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < width.length; i++) {
            String pattern = rightAligned[i] ? " %" + width[i] + "s |" : " %-" + width[i] + "s |";
            sb.append(String.format(pattern, cells[i]));
        }
        return sb.toString();
    }

    // ================
    //  INPUT HELPERS 
    // ================

    private static void inputError(String message) {
        System.out.println("|   ! " + message);
    }

    
    private int readChoice() {
        System.out.print("Enter choice > ");
        String line = scanner.nextLine().trim();
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String readNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = scanner.nextLine().trim();
            if (!s.isEmpty()) {
                return s;
            }
            inputError("Input cannot be empty. Try again.");
        }
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String s = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(s);
                if (value >= min && value <= max) {
                    return value;
                }
                inputError("Value must be between " + min + " and "
                        + (max == Integer.MAX_VALUE ? "a large number" : String.valueOf(max)) + ".");
            } catch (NumberFormatException e) {
                inputError("Please enter a valid whole number.");
            }
        }
    }

    
    private double readDouble(String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            String s = scanner.nextLine().trim();
            if (!DECIMAL.matcher(s).matches()) {
                inputError("Please enter a positive number such as 1500 or 1500.50.");
                continue;
            }
            double value = Double.parseDouble(s);
            if (value >= min && value <= max) {
                return value;
            }
            inputError(String.format(Locale.US, "Value must be between %,.2f and %,.2f.", min, max));
        }
    }

    private FuelType readFuelType() {
        FuelType[] types = FuelType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.println(boxLeft("[" + (i + 1) + "] " + types[i]));
        }
        int pick = readInt(prompt("Fuel type"), 1, types.length);
        return types[pick - 1];
    }
}

// ======================= FuelType =======================

enum FuelType {
    PETROL(1.00),
    DIESEL(0.90),
    ELECTRIC(0.75),
    HYBRID(0.85);

    private final double costMultiplier;

    FuelType(double costMultiplier) {
        this.costMultiplier = costMultiplier;
    }

    public double getCostMultiplier() {
        return costMultiplier;
    }
}

// ======================= RentalStatus =======================
enum RentalStatus {
    AVAILABLE,
    RENTED
}

// ======================= SystemUser =======================
class SystemUser {
    private String username;
    private String password;   
    private String fullName;

    public SystemUser(String username, String password, String fullName) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    @Override
    public String toString() {
        
        return "SystemUser{username='" + username + "', fullName='" + fullName + "'}";
    }
}

// ======================= UserManager =======================
class UserManager {
    private final List<SystemUser> users = new ArrayList<>();

    
    public SystemUser registerUser(String username, String password, String fullName) {
        if (findByUsername(username) != null) {
            System.out.println("Username already taken. Please choose another one.");
            return null;
        }
        SystemUser user = new SystemUser(username, password, fullName);
        users.add(user);
        System.out.println("User '" + username + "' registered successfully.");
        return user;
    }

   
    public SystemUser login(String username, String password) {
        for (SystemUser user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    
    public SystemUser findByUsername(String username) {
        for (SystemUser user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }
}

// ======================= Reportable =======================

interface Reportable {
    String getSummary();
    String getReportCategory();
}

// ======================= RentalEventListener =======================
@FunctionalInterface
interface RentalEventListener {
    void onRentalCreated(RentalAgreement agreement);
}

// ======================= VehicleNotAvailableException =======================

class VehicleNotAvailableException extends Exception {
    private static final long serialVersionUID = 1L;

    public VehicleNotAvailableException(String message) {
        super(message);
    }
}


class InsufficientCreditException extends Exception {
    private static final long serialVersionUID = 1L;

    public InsufficientCreditException(String message) {
        super(message);
    }
}

// ======================= Vehicle =======================
abstract class Vehicle implements Reportable {
    private static int counter = 1000;

    private final String vehicleId;
    private String registrationNumber;
    private String brand;
    private String model;
    private double dailyRate;
    private FuelType fuelType;
    private RentalStatus status;

    public Vehicle(String registrationNumber, String brand, String model,
                   double dailyRate, FuelType fuelType) {
        this.vehicleId = generateId();
        this.registrationNumber = registrationNumber;
        this.brand = brand;
        this.model = model;
        this.dailyRate = dailyRate;
        this.fuelType = fuelType;
        this.status = RentalStatus.AVAILABLE;
    }

    private static synchronized String generateId() {
        counter++;
        return "VEH-" + counter;
    }

   
    public String getVehicleId() { return vehicleId; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public double getDailyRate() { return dailyRate; }
    public void setDailyRate(double dailyRate) { this.dailyRate = dailyRate; }

    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType; }

    public RentalStatus getStatus() { return status; }
    public void setStatus(RentalStatus status) { this.status = status; }

    public abstract double calculateRentalCost(int rentalDays);

    @Override
    public String getSummary() {
        return vehicleId + " " + brand + " " + model + " " + status;
    }

    @Override
    public String getReportCategory() {
        return "VEHICLE";
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "%s | Reg: %s | %s %s | Rate: %,.2f/day | Fuel: %s | Status: %s",
                vehicleId, registrationNumber, brand, model, dailyRate, fuelType, status);
    }
}

// ======================= Car =======================
class Car extends Vehicle {

    public Car(String registrationNumber, String brand, String model,
               double dailyRate, FuelType fuelType) {
        super(registrationNumber, brand, model, dailyRate, fuelType);
    }

    @Override
    public double calculateRentalCost(int rentalDays) {
        return getDailyRate() * rentalDays * getFuelType().getCostMultiplier();
    }
}

// ======================= Van =======================
class Van extends Vehicle {
    private int passengerCapacity;

    public Van(String registrationNumber, String brand, String model,
               double dailyRate, FuelType fuelType, int passengerCapacity) {
        super(registrationNumber, brand, model, dailyRate, fuelType);
        this.passengerCapacity = passengerCapacity;
    }

    public int getPassengerCapacity() { return passengerCapacity; }
    public void setPassengerCapacity(int passengerCapacity) { this.passengerCapacity = passengerCapacity; }

    @Override
    public double calculateRentalCost(int rentalDays) {
       
        return (getDailyRate() * rentalDays * getFuelType().getCostMultiplier())
                + (getDailyRate() * 0.20);
    }

    @Override
    public String toString() {
        return super.toString() + " | Passengers: " + passengerCapacity;
    }
    }

// ======================= Truck =======================
class Truck extends Vehicle {
    private double cargoCapacityTons;

    public Truck(String registrationNumber, String brand, String model,
                 double dailyRate, FuelType fuelType, double cargoCapacityTons) {
        super(registrationNumber, brand, model, dailyRate, fuelType);
        this.cargoCapacityTons = cargoCapacityTons;
    }

    public double getCargoCapacityTons() { return cargoCapacityTons; }
    public void setCargoCapacityTons(double cargoCapacityTons) { this.cargoCapacityTons = cargoCapacityTons; }

    @Override
    public double calculateRentalCost(int rentalDays) {
        
        return (getDailyRate() * rentalDays * getFuelType().getCostMultiplier())
                + (cargoCapacityTons * 10.00 * rentalDays);
    }

    @Override
    public String toString() {
        return super.toString() + " | Cargo: " + cargoCapacityTons + "t";
    }
}

// ======================= Customer =======================
abstract class Customer implements Reportable {
    private static int counter = 1000;

    private final String customerId;
    private String fullName;
    private String contactNumber;

    public Customer(String fullName, String contactNumber) {
        this.customerId = generateId();
        this.fullName = fullName;
        this.contactNumber = contactNumber;
    }

    private static synchronized String generateId() {
        counter++;
        return "CUS-" + counter;
    }

    
    public String getCustomerId() { return customerId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    
    public abstract double applyDiscount(double subtotal);

    @Override
    public String getSummary() {
        return customerId + " " + fullName;
    }

    @Override
    public String getReportCategory() {
        return "CUSTOMER";
    }

    @Override
    public String toString() {
        return customerId + " | " + fullName + " | Contact: " + contactNumber;
    }
}

// ======================= IndividualCustomer =======================
class IndividualCustomer extends Customer {
    private int loyaltyPoints;

    public IndividualCustomer(String fullName, String contactNumber, int loyaltyPoints) {
        super(fullName, contactNumber);
        this.loyaltyPoints = loyaltyPoints;
    }

    public int getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(int loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }

    @Override
    public double applyDiscount(double subtotal) {
        double discountRate;
        if (loyaltyPoints >= 300) {
            discountRate = 0.10;
        } else if (loyaltyPoints >= 100) {
            discountRate = 0.05;
        } else {
            discountRate = 0.00;
        }
        return subtotal * (1 - discountRate);
    }

    @Override
    public String toString() {
        return super.toString() + " | Individual | Loyalty points: " + loyaltyPoints;
    }
}

// ======================= CorporateCustomer =======================
class CorporateCustomer extends Customer {
    private double creditLimit;
    private double outstandingBalance;

    public CorporateCustomer(String fullName, String contactNumber, double creditLimit) {
        super(fullName, contactNumber);
        this.creditLimit = creditLimit;
        this.outstandingBalance = 0;
    }

    public double getCreditLimit() { return creditLimit; }
    public void setCreditLimit(double creditLimit) { this.creditLimit = creditLimit; }

    public double getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(double outstandingBalance) { this.outstandingBalance = outstandingBalance; }

    @Override
    public double applyDiscount(double subtotal) {
        return subtotal * 0.85;   
    }

    public boolean hasSufficientCredit(double cost) {
        return (outstandingBalance + cost) <= creditLimit;
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "%s | Corporate | Credit limit: %,.2f | Outstanding: %,.2f",
                super.toString(), creditLimit, outstandingBalance);
    }
}

// ======================= RentalAgreement =======================
class RentalAgreement {
    private static int counter = 1000;

    private final String agreementId;
    private final Vehicle vehicle;       
    private final Customer customer;    
    private final int rentalDays;
    private final double totalCost;
    private int daysElapsed;
    private boolean returned;            

    public RentalAgreement(Vehicle vehicle, Customer customer, int rentalDays) {
        this.vehicle = vehicle;
        this.customer = customer;
        this.rentalDays = rentalDays;
        double subtotal = vehicle.calculateRentalCost(rentalDays);   
        this.totalCost = customer.applyDiscount(subtotal);           
        this.agreementId = generateId();
        this.daysElapsed = 0;
    }

    private static synchronized String generateId() {
        counter++;
        return "AGR-" + counter;
    }

    public String getAgreementId() { return agreementId; }
    public Vehicle getVehicle() { return vehicle; }
    public Customer getCustomer() { return customer; }
    public int getRentalDays() { return rentalDays; }
    public double getTotalCost() { return totalCost; }

    public int getDaysElapsed() { return daysElapsed; }
    public void setDaysElapsed(int daysElapsed) { this.daysElapsed = daysElapsed; }

    public boolean isReturned() { return returned; }
    public void markReturned() { this.returned = true; }

    @Override
    public String toString() {
        String line = "=".repeat(64);
        String vehicleType = vehicle.getClass().getSimpleName();                           
        String customerType = customer.getClass().getSimpleName().replace("Customer", ""); 
        StringBuilder sb = new StringBuilder();
        sb.append(line).append(System.lineSeparator());
        sb.append("RENTAL AGREEMENT RECEIPT").append(System.lineSeparator());
        sb.append(line).append(System.lineSeparator());
        sb.append("Agreement ID : ").append(agreementId).append(System.lineSeparator());
        sb.append("Vehicle      : ").append(vehicle.getVehicleId()).append(" - ")
          .append(vehicle.getBrand()).append(" ").append(vehicle.getModel())
          .append(" (").append(vehicleType).append(")").append(System.lineSeparator());
        sb.append("Customer     : ").append(customer.getCustomerId()).append(" - ")
          .append(customer.getFullName()).append(" (").append(customerType).append(")")
          .append(System.lineSeparator());
        sb.append("Rental Days  : ").append(rentalDays).append(System.lineSeparator());
        sb.append("Total Cost   : ").append(String.format(Locale.US, "%,.2f", totalCost))
          .append(System.lineSeparator());
        sb.append(line);
        return sb.toString();
    }
}

// ======================= FleetManager =======================
class FleetManager {
    private final List<Vehicle> vehicles = new ArrayList<>();            
    private final List<Customer> customers = new ArrayList<>();          
    private final List<RentalAgreement> agreements = new ArrayList<>();  
    private RentalEventListener rentalEventListener;                     

   
    public synchronized void addVehicle(Vehicle v) {
        vehicles.add(v);
    }

    public synchronized void addCustomer(Customer c) {
        customers.add(c);
    }

    public synchronized Vehicle findVehicleById(String id) {
        for (Vehicle v : vehicles) {
            if (v.getVehicleId().equalsIgnoreCase(id)) {
                return v;
            }
        }
        return null;
    }

    public synchronized Customer findCustomerById(String id) {
        for (Customer c : customers) {
            if (c.getCustomerId().equalsIgnoreCase(id)) {
                return c;
            }
        }
        return null;
    }

    public synchronized void setRentalEventListener(RentalEventListener listener) {
        this.rentalEventListener = listener;
    }

   
    public synchronized List<Vehicle> getAvailableVehicles() {
        return vehicles.stream()
                .filter(v -> v.getStatus() == RentalStatus.AVAILABLE)
                .collect(Collectors.toList());
    }

    public synchronized List<Customer> getAllCustomers() {
        List<Customer> result = new ArrayList<>();
        for (Customer c : customers) {
            result.add(c);
        }
        return result;
    }

    public synchronized List<RentalAgreement> getAllAgreements() {
        List<RentalAgreement> result = new ArrayList<>();
        for (RentalAgreement a : agreements) {
            result.add(a);
        }
        return result;
    }

    public synchronized RentalAgreement createRentalAgreement(String vehicleId, String customerId, int rentalDays)
            throws VehicleNotAvailableException, InsufficientCreditException {

        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle == null) {
            System.out.println("No vehicle found with ID: " + vehicleId);
            return null;
        }

        if (vehicle.getStatus() != RentalStatus.AVAILABLE) {
            throw new VehicleNotAvailableException(
                    "Vehicle " + vehicle.getVehicleId() + " is not available (status: " + vehicle.getStatus() + ").");
        }

        Customer customer = findCustomerById(customerId);
        if (customer == null) {
            System.out.println("No customer found with ID: " + customerId);
            return null;
        }

        double subtotal = vehicle.calculateRentalCost(rentalDays);
        double total = customer.applyDiscount(subtotal);

        if (customer instanceof CorporateCustomer) {
            CorporateCustomer corporate = (CorporateCustomer) customer;
            if (!corporate.hasSufficientCredit(total)) {
                throw new InsufficientCreditException(String.format(Locale.US,
                        "Insufficient credit for %s. Cost: %,.2f, Credit limit: %,.2f, Outstanding: %,.2f.",
                        corporate.getFullName(), total, corporate.getCreditLimit(),
                        corporate.getOutstandingBalance()));
            }
            
            corporate.setOutstandingBalance(corporate.getOutstandingBalance() + total);
        }

        RentalAgreement agreement = new RentalAgreement(vehicle, customer, rentalDays);
        vehicle.setStatus(RentalStatus.RENTED);
        agreements.add(agreement);

        if (rentalEventListener != null) {
            rentalEventListener.onRentalCreated(agreement);   
        }
        return agreement;
    }

    public synchronized void returnVehicle(String agreementId) {
        for (RentalAgreement a : agreements) {
            if (a.getAgreementId().equalsIgnoreCase(agreementId)) {
                if (a.isReturned()) {
                    System.out.println("Agreement " + a.getAgreementId() + " has already been returned.");
                    return;
                }
                a.markReturned();
                a.getVehicle().setStatus(RentalStatus.AVAILABLE);
                if (a.getCustomer() instanceof CorporateCustomer) {
                    
                    CorporateCustomer corporate = (CorporateCustomer) a.getCustomer();
                    corporate.setOutstandingBalance(
                            Math.max(0, corporate.getOutstandingBalance() - a.getTotalCost()));
                }
                System.out.println("Vehicle " + a.getVehicle().getVehicleId()
                        + " returned successfully (Agreement " + a.getAgreementId() + ").");
                return;
            }
        }
        System.out.println("No rental agreement found with ID: " + agreementId);
    }

   

    public synchronized FleetSnapshot generateSnapshot() {
        return new FleetSnapshot();
    }

    public synchronized void printFleetStatus() {
        generateSnapshot().printSummary();
    }

    public synchronized void checkOverdueAgreements() {
        for (RentalAgreement a : agreements) {
            if (a.isReturned()) {
                continue;   
            }
            a.setDaysElapsed(a.getDaysElapsed() + 1);
            if (a.getDaysElapsed() > a.getRentalDays()) {
                System.out.println("[OVERDUE ALERT] Agreement " + a.getAgreementId()
                        + " has exceeded its " + a.getRentalDays() + "-day rental period.");
            }
        }
    }

    // ---------------- Mission 4 ----------------

    public synchronized List<Vehicle> getVehiclesByFuelType(FuelType type) {
        return vehicles.stream()
                .filter(v -> v.getFuelType() == type)
                .collect(Collectors.toList());
    }

    public synchronized List<Customer> getCustomersSortedByName() {
        return customers.stream()
                .sorted(Comparator.comparing(c -> c.getFullName()))
                .collect(Collectors.toList());
    }

    public synchronized double generateRevenueReport() {
        return agreements.stream()
                .mapToDouble(a -> a.getTotalCost())
                .sum();
    }

    public synchronized void printAllReportables() {
        List<Reportable> reportables = new ArrayList<>();
        reportables.addAll(vehicles);
        reportables.addAll(customers);
        if (reportables.isEmpty()) {
            System.out.println("Nothing to report yet.");
            return;
        }
        for (Reportable item : reportables) {
            System.out.println(item.getReportCategory() + ": " + item.getSummary());
        }
    }

    // ---------------- Inner class ----------------

    public class FleetSnapshot {
        private int availableCount;
        private int rentedCount;
        private double totalRevenue;

        public FleetSnapshot() {
            for (Vehicle v : vehicles) {
                if (v.getStatus() == RentalStatus.AVAILABLE) {
                    availableCount++;
                } else if (v.getStatus() == RentalStatus.RENTED) {
                    rentedCount++;
                }
            }
            for (RentalAgreement a : agreements) {
                totalRevenue += a.getTotalCost();
            }
        }

        public int getAvailableCount() { return availableCount; }
        public int getRentedCount() { return rentedCount; }
        public double getTotalRevenue() { return totalRevenue; }

        public void printSummary() {
            System.out.println(String.format(Locale.US,
                    "[FLEET STATUS] Available: %d  Rented: %d  Total Revenue So Far: %,.2f",
                    availableCount, rentedCount, totalRevenue));
        }
    }
}

// ======================= VehicleStatusMonitor =======================
class VehicleStatusMonitor implements Runnable {
    private final FleetManager fleetManager;

    public VehicleStatusMonitor(FleetManager fleetManager) {
        this.fleetManager = fleetManager;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(5000);
                fleetManager.printFleetStatus();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;   
            }
        }
    }
}

// ======================= OverdueReminderWorker =======================
class OverdueReminderWorker implements Runnable {
    private final FleetManager fleetManager;

    public OverdueReminderWorker(FleetManager fleetManager) {
        this.fleetManager = fleetManager;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(5000);
                fleetManager.checkOverdueAgreements();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}