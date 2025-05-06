import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WorkerHandler extends Thread {
    private Socket clientSocket;
    private static Map<String, GuestHouse> guestHouses;
    private SimpleDateFormat dateFormat;
    private Socket reducerSocket;
    private PrintWriter reducerWriter;

    public WorkerHandler(Socket clientSocket, Map<String, GuestHouse> guestHouses, SimpleDateFormat dateFormat, Socket reducerSocket) throws IOException {
        this.clientSocket = clientSocket;
        WorkerHandler.guestHouses = guestHouses;
        this.dateFormat = dateFormat;
        this.reducerSocket = reducerSocket;
        this.reducerWriter = new PrintWriter(reducerSocket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String inputLine;
            StringBuilder jsonData = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                jsonData.append(inputLine);
            }

            System.out.println("Received JSON: " + jsonData.toString());
            processJson(jsonData.toString());
        } catch (IOException e) {
            System.out.println("Exception caught when listening for a connection");
            System.out.println(e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Exception caught when closing the client socket");
                System.out.println(e.getMessage());
            }
        }
    }

    public static void loadAndProcessJsonFiles(String directoryPath, SimpleDateFormat dateFormat, Map<String, GuestHouse> guestHouses, String currentWorkerHost, int currentWorkerPort) {
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("Directory does not exist or is not a directory: " + directoryPath);
            return;
        }

        File[] jsonFiles = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            System.out.println("No JSON files found in directory: " + directoryPath);
            return;
        }

        // Process room information files first
        for (File jsonFile : jsonFiles) {
            if (jsonFile.getName().contains("_roomInfo.json")) {
                try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
                    StringBuilder jsonData = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonData.append(line);
                    }
                    JSONObject jsonObject = new JSONObject(jsonData.toString());
                    String roomName = jsonObject.getString("roomName");

                    if (shouldProcess(roomName, currentWorkerHost, currentWorkerPort)) {
                        System.out.println("Processing file: " + jsonFile.getName());
                        processJsonData(jsonData.toString(), dateFormat, guestHouses);
                    }
                } catch (IOException e) {
                    System.out.println("Error reading JSON file: " + jsonFile.getName());
                    e.printStackTrace();
                }
            }
        }

        // Process date information files second
        for (File jsonFile : jsonFiles) {
            if (jsonFile.getName().contains("_datesInfo.json")) {
                try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
                    StringBuilder jsonData = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonData.append(line);
                    }
                    JSONObject jsonObject = new JSONObject(jsonData.toString());
                    String roomName = jsonObject.getString("roomName");

                    if (shouldProcess(roomName, currentWorkerHost, currentWorkerPort)) {
                        System.out.println("Processing file: " + jsonFile.getName());
                        processJsonData(jsonData.toString(), dateFormat, guestHouses);
                    }
                } catch (IOException e) {
                    System.out.println("Error reading JSON file: " + jsonFile.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean shouldProcess(String roomName, String currentWorkerHost, int currentWorkerPort) {
        if (MasterServer.workerNodes.isEmpty()) {
            return false;
        }

        int hash = Math.abs(roomName.hashCode());
        int workerIndex = hash % MasterServer.workerNodes.size();
        WorkerNode assignedWorker = MasterServer.workerNodes.get(workerIndex);

        return assignedWorker.host.equals(currentWorkerHost) && assignedWorker.port == currentWorkerPort;
    }

    private static void processJsonData(String jsonData, SimpleDateFormat dateFormat, Map<String, GuestHouse> guestHouses) {
        if (jsonData.contains("<<<DELIMITER>>>")) {
            String[] parts = jsonData.split("<<<DELIMITER>>>");
            if (parts.length == 2) {
                System.out.println("Processing room data");
                processAndStoreData(parts[0], guestHouses);
                System.out.println("Processing date data");
                processAndStoreDateData(parts[1], dateFormat, guestHouses);
            } else {
                System.out.println("Received data does not properly split into two parts.");
            }
        } else {
            JSONObject jsonObject = new JSONObject(jsonData);
            if (jsonObject.has("noOfPersons")) {
                processAndStoreData(jsonData, guestHouses);
            }
            if (jsonObject.has("availableDates")) {
                processAndStoreDateData(jsonData, dateFormat, guestHouses);
            }
        }
    }

    private static void processAndStoreData(String jsonData, Map<String, GuestHouse> guestHouses) {
        JSONObject jsonObject = new JSONObject(jsonData);
        String roomName = jsonObject.getString("roomName");
        int noOfPersons = jsonObject.getInt("noOfPersons");
        String area = jsonObject.getString("area");
        int stars = jsonObject.getInt("stars");
        int noOfReviews = jsonObject.getInt("noOfReviews");
        String roomImage = jsonObject.getString("roomImage");
        int pricePerNight = jsonObject.getInt("pricePerNight");
        ArrayList<Date> availableDates = new ArrayList<>();

        GuestHouse guestHouse = new GuestHouse(roomName, area, noOfPersons, stars, noOfReviews, roomImage, availableDates, pricePerNight);
        guestHouses.put(roomName, guestHouse);

        System.out.println("Information for " + roomName + " stored or updated successfully.");
    }

    private static void processAndStoreDateData(String jsonData, SimpleDateFormat dateFormat, Map<String, GuestHouse> guestHouses) {
        JSONObject jsonObject = new JSONObject(jsonData);
        String roomName = jsonObject.getString("roomName");
        String availableDatesString = jsonObject.getString("availableDates");
        ArrayList<Date> availableDates = new ArrayList<>();

        String[] dateRanges = availableDatesString.split(";");
        for (String range : dateRanges) {
            String[] dates = range.split("-");
            try {
                Date startDate = dateFormat.parse(dates[0].trim());
                Date endDate = dateFormat.parse(dates[1].trim());

                Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);
                while (!cal.getTime().after(endDate)) {
                    availableDates.add(cal.getTime());
                    cal.add(Calendar.DATE, 1);
                }
            } catch (ParseException e) {
                System.out.println("Error parsing date range: " + e.getMessage());
            }
        }

        GuestHouse guestHouse = guestHouses.get(roomName);
        if (guestHouse != null) {
            guestHouse.setAvailableDates(availableDates);
            System.out.println("Available dates for " + roomName + " updated successfully.");
        } else {
            System.out.println("No GuestHouse found with name: " + roomName + " to update available dates.");
        }
    }

    private void processJson(String jsonData) {
        int mapId = -1; // Default or invalid mapId
        int idx = jsonData.indexOf(':');

        if (idx > -1 && Character.isDigit(jsonData.charAt(0))) {
            try {
                mapId = Integer.parseInt(jsonData.substring(0, idx));
                jsonData = jsonData.substring(idx + 1);
            } catch (NumberFormatException e) {
                System.out.println("Error parsing mapId: " + e.getMessage());
            }
        }

        if (jsonData.contains("<<<DELIMITER>>>")) {
            String[] parts = jsonData.split("<<<DELIMITER>>>");
            if (parts.length == 2) {
                System.out.println("Processing room data...");
                processAndStoreData(parts[0], guestHouses);
                System.out.println("Processing date data...");
                processAndStoreDateData(parts[1], dateFormat, guestHouses);
            } else {
                System.out.println("Received data does not properly split into two parts.");
            }
        } else {
            JSONObject jsonObject = new JSONObject(jsonData);
            if (jsonObject.has("type") && jsonObject.getString("type").equals("search")) {
                handleSearchRequest(jsonObject, mapId);
            } else {
                if (jsonObject.has("noOfPersons")) {
                    processAndStoreData(jsonData, guestHouses);
                }
                if (jsonObject.has("availableDates")) {
                    processAndStoreDateData(jsonData, dateFormat, guestHouses);
                }
            }
        }
    }

    private void handleSearchRequest(JSONObject searchCriteria, int mapId) {
        String area = searchCriteria.optString("area", "");
        Date startDate = parseDate(searchCriteria.optString("startDate", ""));
        Date endDate = parseDate(searchCriteria.optString("endDate", ""));
        int numberOfPersons = searchCriteria.optInt("numberOfPersons", 0);
        double maxPrice = searchCriteria.optDouble("maxPrice", Double.MAX_VALUE);
        int minStars = searchCriteria.optInt("minStars", 0);

        JSONArray results = new JSONArray();
        for (GuestHouse guestHouse : guestHouses.values()) {
            if (guestHouse.getArea().equalsIgnoreCase(area) &&
                guestHouse.getNoOfPersons() >= numberOfPersons &&
                guestHouse.getStars() >= minStars &&
                guestHouse.getPricePerNight() <= maxPrice &&
                checkAvailability(guestHouse, startDate, endDate)) {
                results.put(guestHouseToJSON(guestHouse));
            }
        }

        JSONObject resultJson = new JSONObject();
        resultJson.put("mapId", mapId);
        resultJson.put("data", results);

        try {
            reducerWriter.println(resultJson.toString());
        } catch (Exception e) {
            System.out.println("Error sending results to reducer: " + e.getMessage());
        }
    }

    private boolean checkAvailability(GuestHouse guestHouse, Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        HashSet<String> availableDateStrings = new HashSet<>();
        for (Date date : guestHouse.getAvailableDates()) {
            availableDateStrings.add(sdf.format(date));
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        while (!cal.getTime().after(endDate)) {
            if (!availableDateStrings.contains(sdf.format(cal.getTime()))) {
                System.out.println("Unavailable on: " + sdf.format(cal.getTime()));
                return false;
            }
            cal.add(Calendar.DATE, 1);
        }
        return true;
    }

    private Date parseDate(String dateString) {
        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            System.out.println("Error parsing date: " + e.getMessage());
            return null;
        }
    }

    private JSONObject guestHouseToJSON(GuestHouse guestHouse) {
        JSONObject json = new JSONObject();
        json.put("roomName", guestHouse.getRoomName());
        json.put("area", guestHouse.getArea());
        json.put("noOfPersons", guestHouse.getNoOfPersons());
        json.put("stars", guestHouse.getStars());
        json.put("noOfReviews", guestHouse.getNoOfReviews());
        json.put("roomImage", guestHouse.getRoomImage());
        json.put("pricePerNight", guestHouse.getPricePerNight());
        json.put("availableDates", new JSONArray(guestHouse.getAvailableDates().stream()
                                                .map(date -> new SimpleDateFormat("dd/MM/yyyy").format(date))
                                                .collect(Collectors.toList())));
        return json;
    }
}











