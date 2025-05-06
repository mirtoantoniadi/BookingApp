import org.json.JSONObject;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Renter {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\nAccommodation Renter Menu");
            System.out.println("1. Search for rooms");
            System.out.println("2. Make a reservation");
            System.out.println("3. Rate a room");
            System.out.println("4. Exit");
            System.out.print("Choose an option: ");
            
            int choice = getValidChoice(scanner);
            
            switch (choice) {
                case 1:
                    scanner.nextLine(); // clear buffer
                    sendSearchCriteriaToMaster(scanner);
                    break;
                case 2:
                    scanner.nextLine(); // clear buffer
                    makeReservation(scanner);
                    break;
                case 3:
                    scanner.nextLine(); // clear buffer
                    rateRoom(scanner);
                    break;
                case 4:
                    System.out.println("Exiting");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid option. Please select again.");
                    break;
            }
        }
    }

    private static void sendSearchCriteriaToMaster(Scanner scanner) {
        try (Socket socket = new Socket("127.0.0.1", 4321);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Enter area of interest (leave blank to skip):");
            String area = scanner.nextLine().trim();
            area = area.isEmpty() ? null : area;

            System.out.println("Enter start date (dd/MM/yyyy, leave blank to skip):");
            String startDateStr = getValidDate(scanner);

            System.out.println("Enter end date (dd/MM/yyyy, leave blank to skip):");
            String endDateStr = getValidDate(scanner);

            System.out.println("Enter number of persons (leave blank to skip):");
            Integer numberOfPersons = getPositiveInt(scanner);

            System.out.println("Enter maximum price (leave blank to skip):");
            Double maxPrice = getNonNegativeDouble(scanner);

            System.out.println("Enter minimum stars (1-5, leave blank to skip):");
            Integer minStars = getStars(scanner);

            JSONObject json = new JSONObject();
            json.put("type", "search");
            json.put("area", area);
            json.put("startDate", startDateStr);
            json.put("endDate", endDateStr);
            json.put("numberOfPersons", numberOfPersons);
            json.put("maxPrice", maxPrice);
            json.put("minStars", minStars);

            out.println(json.toString());
            System.out.println("Search criteria have been sent to the master server.");
        } catch (Exception e) {
            System.out.println("Error while connecting to server: " + e.getMessage());
        }
    }

    private static void makeReservation(Scanner scanner) {
        try (Socket socket = new Socket("127.0.0.1", 4321);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Enter room name to reserve:");
            String roomName = scanner.nextLine().trim();

            System.out.println("Enter start date of reservation (dd/MM/yyyy):");
            String startDateStr = getValidDate(scanner);

            System.out.println("Enter end date of reservation (dd/MM/yyyy):");
            String endDateStr = getValidDate(scanner);

            JSONObject json = new JSONObject();
            json.put("type", "reservation");
            json.put("roomName", roomName);
            json.put("startDate", startDateStr);
            json.put("endDate", endDateStr);

            out.println(json.toString());
            System.out.println("Reservation request has been sent to the master server.");
        } catch (Exception e) {
            System.out.println("Error while connecting to server: " + e.getMessage());
        }
    }

    private static void rateRoom(Scanner scanner) {
        try (Socket socket = new Socket("127.0.0.1", 4321);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Enter room name to rate:");
            String roomName = scanner.nextLine().trim();

            System.out.println("Enter your rating (1-5):");
            Integer rating = getStars(scanner);

            JSONObject json = new JSONObject();
            json.put("type", "rating");
            json.put("roomName", roomName);
            json.put("rating", rating);

            out.println(json.toString());
            System.out.println("Rating has been sent to the master server.");
        } catch (Exception e) {
            System.out.println("Error while connecting to server: " + e.getMessage());
        }
    }

    private static int getValidChoice(Scanner scanner) {
        int choice;
        do {
            while (!scanner.hasNextInt()) {
                System.out.println("Please enter a valid number.");
                scanner.next(); // discard non-integer input
                System.out.print("Choose an option: ");
            }
            choice = scanner.nextInt();
            if (choice < 1 || choice > 4) {
                System.out.println("Please select a valid option from the menu.");
            }
        } while (choice < 1 || choice > 4);
        return choice;
    }

    private static String getValidDate(Scanner scanner) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        dateFormat.setLenient(false);
        String dateInput;
        while (true) {
            dateInput = scanner.nextLine().trim();
            if (dateInput.isEmpty()) return null;
            try {
                dateFormat.parse(dateInput);
                return dateInput;
            } catch (ParseException e) {
                System.out.println("Invalid date format, please enter again (dd/MM/yyyy):");
            }
        }
    }

    private static int getPositiveInt(Scanner scanner) {
        int number;
        do {
            while (!scanner.hasNextInt()) {
                System.out.println("Please enter a valid number.");
                scanner.next(); // discard non-integer input
            }
            number = scanner.nextInt();
            if (number <= 0) {
                System.out.println("Please enter a positive number.");
            }
        } while (number <= 0);
        return number;
    }

    private static int getStars(Scanner scanner) {
        int stars;
        do {
            while (!scanner.hasNextInt()) {
                System.out.println("Please enter a valid number for stars.");
                scanner.next(); // discard non-integer input
            }
            stars = scanner.nextInt();
            if (stars < 1 || stars > 5) {
                System.out.println("Stars must be between 1 and 5.");
            }
        } while (stars < 1 || stars > 5);
        return stars;
    }

    private static int getNonNegativeInt(Scanner scanner) {
        int number;
        do {
            while (!scanner.hasNextInt()) {
                System.out.println("Please enter a valid number.");
                scanner.next(); // discard non-integer input
            }
            number = scanner.nextInt();
            if (number < 0) {
                System.out.println("Please enter a non-negative number.");
            }
        } while (number < 0);
        return number;
    }

    private static Double getNonNegativeDouble(Scanner scanner) {
        double number;
        do {
            while (!scanner.hasNextDouble()) {
                System.out.println("Please enter a valid number.");
                scanner.next(); // discard non-double input
            }
            number = scanner.nextDouble();
            if (number < 0) {
                System.out.println("Please enter a non-negative number.");
            }
        } while (number < 0);
        return number;
    }
}







