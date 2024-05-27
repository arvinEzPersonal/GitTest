package com.myAuctionApp;

import com.myAuctionApp.model.Auction;
import com.myAuctionApp.model.Bid;
import com.myAuctionApp.model.User;
import com.myAuctionApp.service.*;
import com.myAuctionApp.task.AuctionClosingTask;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainBid {
    private static AuctionService auctionService = new AuctionService();
    private static BidService bidService = new BidService();
    private static AdminService adminService = new AdminService();
    private static RegistrationService registrationService = new RegistrationService();
    private static LoginService loginService = new LoginService();
    private static UserService userService = new UserService();
    private static User loggedInUser = null;

    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AuctionClosingTask task = new AuctionClosingTask();
        scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.MINUTES);

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Create Auction");
            System.out.println("4. View My Auctions");
            System.out.println("5. View All Active Auctions");
            System.out.println("6. Place Bid");
            System.out.println("7. Admin Dashboard");
            System.out.println("8. Exit");

            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            try {
                switch (choice) {
                    case 1:
                        register(scanner);
                        break;
                    case 2:
                        login(scanner);
                        break;
                    case 3:
                        if (isLoggedIn()) {
                            createAuction(scanner);
                        } else {
                            System.out.println("You must be logged in to create an auction.");
                        }
                        break;
                    case 4:
                        if (isLoggedIn()) {
                            viewMyAuctions();
                        } else {
                            System.out.println("You must be logged in to view your auctions.");
                        }
                        break;
                    case 5:
                        viewAllActiveAuctions();
                        break;
                    case 6:
                        if (isLoggedIn()) {
                            placeBid(scanner);
                        } else {
                            System.out.println("You must be logged in to place a bid.");
                        }
                        break;
                    case 7:
                        if (isAdmin()) {
                            adminDashboard(scanner);
                        } else {
                            System.out.println("You must be an admin to access the admin dashboard.");
                        }
                        break;
                    case 8:
                        System.exit(0);
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
            }
        }
    }

    private static void register(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        System.out.print("Enter email: ");
        String email = scanner.nextLine();

        if (!isValidEmail(email)) {
            System.out.println("Invalid email format.");
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);

        try {
            registrationService.register(user);
            System.out.println("Registration successful!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to register.");
        }
    }

    private static void login(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        try {
            boolean success = loginService.login(username, password);
            if (success) {
                loggedInUser = loginService.getUserByUsername(username);
                System.out.println("Login successful!");
            } else {
                System.out.println("Invalid credentials.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to login.");
        }
    }

    private static void createAuction(Scanner scanner) {
        System.out.print("Enter item name: ");
        String itemName = scanner.nextLine();
        System.out.print("Enter description: ");
        String description = scanner.nextLine();
        System.out.print("Enter starting bid: ");
        double startingBid;
        try {
            startingBid = Double.parseDouble(scanner.nextLine());
            if (startingBid <= 0) {
                System.out.println("Starting bid must be a positive number.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid starting bid.");
            return;
        }
        System.out.print("Enter end time (YYYY-MM-DDTHH:MM:SS) [e.g., 2024-05-21T15:30:00]: ");
        String endTimeStr = scanner.nextLine();
        ZonedDateTime endTime;
        try {
            endTime = ZonedDateTime.parse(endTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault()));
        } catch (Exception e) {
            System.out.println("Invalid date format.");
            return;
        }

        Auction auction = new Auction();
        auction.setItemName(itemName);
        auction.setDescription(description);
        auction.setStartingBid(startingBid);
        auction.setHighestBid(startingBid); // Initialize highest bid with starting bid
        auction.setEndTime(endTime);
        auction.setAuctioneerId(loggedInUser.getId());
        auction.setStatus("active"); // Ensure the status is set to "active"

        try {
            auctionService.createAuction(auction);
            System.out.println("Auction created successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to create auction.");
        }
    }

    private static void viewMyAuctions() {
        try {
            List<Auction> auctions = auctionService.getAuctionsByAuctioneer(loggedInUser.getId());
            if (auctions.isEmpty()) {
                System.out.println("No auctions found.");
            } else {
                for (Auction auction : auctions) {
                	String leftAlignFormat = "| %-10d | %-20s | %-30s | %-12.2f | %-12.2f | %-25s | %-8s |%n";

                    System.out.format("+------------+----------------------+--------------------------------+--------------+--------------+---------------------------+----------+%n");
                    System.out.format("| Auction ID | Item Name            | Description                    | Start Bid    | Highest Bid  | End Time                  | Status   |%n");
                    System.out.format("+------------+----------------------+--------------------------------+--------------+--------------+---------------------------+----------+%n");

                    for (Auction auction1 : auctions) {
                        System.out.format(leftAlignFormat,
                                auction1.getId(),
                                auction1.getItemName(),
                                auction1.getDescription(),
                                auction1.getStartingBid(),
                                auction1.getHighestBid(),
                                auction1.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                auction1.isActive() ? "Active" : "Closed");
                    }
                    System.out.format("+------------+----------------------+--------------------------------+--------------+--------------+---------------------------+----------+%n");

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to retrieve auctions.");
        }
    }

    private static void viewAllActiveAuctions() {
        try {
            List<Auction> auctions = auctionService.getAllActiveAuctions();
            if (auctions.isEmpty()) {
                System.out.println("No active auctions found.");
            } else {
                for (Auction auction : auctions) {
                	String leftAlignFormat = "| %-10d | %-20s | %-30s | %-12.2f | %-12.2f | %-25s | %-8s |%n";

                    System.out.format("+------------+----------------------+--------------------------------+--------------+--------------+---------------------------+----------+%n");
                    System.out.format("| Auction ID | Item Name            | Description                    | Start Bid    | Highest Bid  | End Time                  | Status   |%n");
                    System.out.format("+------------+----------------------+--------------------------------+--------------+--------------+---------------------------+----------+%n");

                    System.out.format(leftAlignFormat,
                            auction.getId(),
                            auction.getItemName(),
                            auction.getDescription(),
                            auction.getStartingBid(),
                            auction.getHighestBid(),
                            auction.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            auction.isActive() ? "Active" : "Closed");

                    System.out.format("+------------+----------------------+--------------------------------+--------------+--------------+---------------------------+----------+%n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to retrieve active auctions.");
        }
    }

    private static void placeBid(Scanner scanner) {
        System.out.print("Enter auction ID: ");
        int auctionId;
        try {
            auctionId = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid auction ID.");
            return;
        }

        Auction auction;
        try {
            auction = auctionService.getAuctionById(auctionId);
            if (auction == null) {
                System.out.println("Auction not found.");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to retrieve auction.");
            return;
        }

        System.out.println("The current highest bid is " + auction.getHighestBid());
        System.out.print("Enter bid amount: ");
        double bidAmount;
        try {
            bidAmount = Double.parseDouble(scanner.nextLine());
            if (bidAmount <= 0) {
                System.out.println("Bid amount must be a positive number.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid bid amount.");
            return;
        }

        Bid bid = new Bid();
        bid.setAuctionId(auctionId);
        bid.setBidderId(loggedInUser.getId());
        bid.setBidAmount(bidAmount);

        try {
            bidService.placeBid(bid, auction);
            System.out.println("Bid placed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to place bid.");
        }
    }

    private static void adminDashboard(Scanner scanner) {
        while (true) {
            System.out.println("Admin Dashboard:");
            System.out.println("1. View All Users in My Auctions");
            System.out.println("2. View My Auctions");
            System.out.println("3. Delete User");
            System.out.println("4. Delete Auction");
            System.out.println("5. Back to Main Menu");

            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            try {
                switch (choice) {
                    case 1:
                        viewAllUsers();
                        break;
                    case 2:
                        viewMyAuctions();
                        break;
                    case 3:
                        deleteUser(scanner);
                        break;
                    case 4:
                        deleteAuction(scanner);
                        break;
                    case 5:
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
            }
        }
    }

    private static void viewAllUsers() {
        try {
            List<User> users = adminService.getUsersByAuctioneer(loggedInUser.getId());
            if (users.isEmpty()) {
                System.out.println("No users found.");
            } else {
                for (User user : users) {
                    System.out.println("User ID: " + user.getId());
                    System.out.println("Username: " + user.getUsername());
                    System.out.println("Email: " + user.getEmail());
                    System.out.println();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to retrieve users.");
        }
    }

    private static void deleteUser(Scanner scanner) {
        System.out.print("Enter user ID to delete: ");
        int userId;
        try {
            userId = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid user ID.");
            return;
        }

        try {
            adminService.deleteUser(userId);
            System.out.println("User deleted successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to delete user.");
        }
    }

    private static void deleteAuction(Scanner scanner) {
        System.out.print("Enter auction ID to delete: ");
        int auctionId;
        try {
            auctionId = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid auction ID.");
            return;
        }

        try {
            adminService.deleteAuction(auctionId);
            System.out.println("Auction deleted successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to delete auction.");
        }
    }

    private static boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    private static boolean isLoggedIn() {
        return loggedInUser != null;
    }

    private static boolean isAdmin() {
        return loggedInUser != null && loggedInUser.isAdmin();
    }
}
