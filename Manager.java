import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Manager {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\nAccommodation Management Menu");
            System.out.println("1. Adding accommodation and available dates for rent");
            System.out.println("2. Show reservations for accommodation");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            
            int choice = getValidChoice(scanner);
            
            switch (choice) {
                case 1:
                    scanner.nextLine(); // clear buffer
                    System.out.println("Enter room name:");
                    String roomName = scanner.nextLine();

                    System.out.println("Enter number of persons:");
                    int noOfPersons = getPositiveInt(scanner);

                    scanner.nextLine(); // clear buffer
                    System.out.println("Enter area:");
                    String area = scanner.nextLine();

                    System.out.println("Enter stars (1-5):");
                    int stars = getStars(scanner);

                    System.out.println("Enter number of reviews:");
                    int noOfReviews = getNonNegativeInt(scanner);

                    scanner.nextLine(); // clear buffer
                    System.out.println("Enter room image path:");
                    String roomImage = scanner.nextLine();

                    System.out.println("Enter price per night:");
                    int pricePerNight = getPositiveInt(scanner);

                    scanner.nextLine(); // clear buffer
                    System.out.println("Enter available dates (dd/MM/yyyy-dd/MM/yyyy, separate multiple ranges with ;):");
                    String availableDates = getValidDates(scanner);

                    // Create JSON strings
                    String jsonString = String.format("{\"roomName\": \"%s\", \"noOfPersons\": %d, \"area\": \"%s\", \"stars\": %d, \"noOfReviews\": %d, \"roomImage\": \"%s\", \"pricePerNight\": %d}",
                                                      roomName, noOfPersons, area, stars, noOfReviews, roomImage, pricePerNight);
                    
                    String datesJsonString = String.format("{\"roomName\": \"%s\", \"availableDates\": \"%s\"}",
                                                            roomName, availableDates);
                    
                    // Save and send to master
                    saveAndSendJson(roomName, jsonString, datesJsonString);
                    break;
                case 2:
                    // Display reservations
                    break;
                case 3:
                    System.out.println("Exiting");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid option. Please select again.");
                    break;
            }
        }
    }

    // Choise tou xristi apo 1-3 
    private static int getValidChoice(Scanner scanner) {
        int choice;
        do {
            while (!scanner.hasNextInt()) {
                System.out.println("Please enter a valid number.");
                scanner.next(); // discard non-integer input
                System.out.print("Choose an option: ");
            }
            choice = scanner.nextInt();
            if (choice < 1 || choice > 3) {
                System.out.println("Please select a valid option from the menu.");
            }
        } while (choice < 1 || choice > 3);
        return choice;
    }
    
    // Arithmos atomwn megaliteros tou 0 
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

    // Asteria apo 1-5 
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

    // Reviews >= 0 
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

    private static String getValidDates(Scanner scanner) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        dateFormat.setLenient(false);
        String input;
        boolean valid;

        do {
            valid = true;
            input = scanner.nextLine();
            String[] dateRanges = input.split(";");
            
            for (String range : dateRanges) {
                String[] dates = range.split("-");
                if (dates.length != 2) {
                    valid = false;
                    break;
                }
                
                try {
                    Date startDate = dateFormat.parse(dates[0].trim());
                    Date endDate = dateFormat.parse(dates[1].trim());
                    if (startDate.after(endDate)) {
                        valid = false;
                        break;
                    }
                } catch (ParseException e) {
                    valid = false;
                    break;
                }
            }
            
            if (!valid) {
                System.out.println("Invalid date format or logical date error. Please enter again (dd/MM/yyyy-dd/MM/yyyy, separate multiple ranges with ;):");
            }
        } while (!valid);

        return input;
    }

    private static void saveAndSendJson(String roomName, String jsonString, String datesJsonString) {
        try {
            String roomInfoFilename = roomName + "_roomInfo.json";
            String datesInfoFilename = roomName + "_datesInfo.json";

            // Save JSON Strings files to folder 
            writeToFile(roomInfoFilename, jsonString);
            writeToFile(datesInfoFilename, datesJsonString);

            System.out.println("JSON files saved successfully.");

            // Send json to master 
            sendJsonToServer("127.0.0.1", 4321, jsonString, datesJsonString);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private static void writeToFile(String filename, String content) throws IOException {
        // Define the directory path for JSON files
        File directory = new File("JSON");
        
        // Create the directory if it does not exist
        if (!directory.exists()) {
            directory.mkdir();
        }
        
        // Create the file object within the directory
        File file = new File(directory, filename);
        
        // Write content to the file.
        FileWriter writer = new FileWriter(file);
        writer.write(content);
        writer.close();
    }

    private static void sendJsonToServer(String serverAddress, int port, String json, String datesJsonString) throws IOException {
        // Dimiourgei mia sindesi ipodoxis me ton server xrisimopoiontas to IP address kai to port 
        Socket socket = new Socket(serverAddress, port);
        OutputStream output = socket.getOutputStream();
        PrintWriter writer = new PrintWriter(output, true);
        
        // Use a unique delimiter to separate the two JSON strings
        String combinedJson = json + "<<<DELIMITER>>>" + datesJsonString;
        
        writer.println(combinedJson);
        socket.close();
    }
}




