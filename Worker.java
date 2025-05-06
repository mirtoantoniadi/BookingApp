import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class Worker {

    private static Map<String, GuestHouse> guestHouses = new HashMap<>();
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Worker <port number> <json directory>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String jsonDirectory = args[1];

        String currentWorkerHost = "127.0.0.1"; // Replace with actual host if needed
        int currentWorkerPort = port;

        // Load worker nodes
        MasterServer.loadWorkerNodes("workers.config");

        // Ensure worker nodes are loaded
        if (MasterServer.workerNodes.isEmpty()) {
            System.out.println("No worker nodes are loaded. Exiting.");
            return;
        }

        // Load and process JSON files based on hash function
        WorkerHandler.loadAndProcessJsonFiles(jsonDirectory, dateFormat, guestHouses, currentWorkerHost, currentWorkerPort);

        try {
            Socket reducerSocket = new Socket("127.0.0.1", 5555);
            System.out.println("Connected to Reducer at 127.0.0.1:5555");

            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Worker listening on port " + port);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected");

                    WorkerHandler workerHandler = new WorkerHandler(clientSocket, guestHouses, dateFormat, reducerSocket);
                    workerHandler.start();
                }
            } catch (IOException e) {
                System.out.println("Could not listen on port " + port);
                System.out.println(e.getMessage());
            }
        } catch (IOException e) {
            System.out.println("Could not connect to Reducer at 127.0.0.1:5555");
            e.printStackTrace();
        }
    }
}












