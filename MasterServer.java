import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class WorkerNode {
    String host;
    int port;

    WorkerNode(String host, int port) {
        this.host = host;
        this.port = port;
    }
}

public class MasterServer {
    private static final String CONFIG_FILE = "workers.config";
    static List<WorkerNode> workerNodes = new ArrayList<>();
    private static AtomicInteger mapIdCounter = new AtomicInteger(0);

    public static void main(String[] args) {
        int port = 4321;
        loadWorkerNodes(CONFIG_FILE);

        Thread resultListener = new Thread(() -> listenForResults(4322)); // New thread to listen for results
        resultListener.start();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                ClientHandler clientHandler = new ClientHandler(socket, workerNodes);
                clientHandler.start();
            }
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static void loadWorkerNodes(String configPath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(configPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    String host = parts[1];
                    int port = Integer.parseInt(parts[2]);
                    workerNodes.add(new WorkerNode(host, port));
                    System.out.println("Loaded worker node: " + parts[0] + " at " + host + ":" + port);
                } else {
                    System.out.println("Skipping invalid config line: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to read configuration file: " + e.getMessage());
        }
    }

    public static void broadcastSearchCriteriaWithMapId(String data) {
        int mapId = mapIdCounter.incrementAndGet();
        for (WorkerNode node : workerNodes) {
            try (Socket socket = new Socket(node.host, node.port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                out.println(mapId + ":" + data);
            } catch (IOException e) {
                System.out.println("Error broadcasting to worker at " + node.host + ":" + node.port + ": " + e.getMessage());
            }
        }
    }

    private static void listenForResults(int resultPort) {
        try (ServerSocket resultSocket = new ServerSocket(resultPort)) {
            System.out.println("Listening for results on port " + resultPort);
            while (true) {
                Socket socket = resultSocket.accept();
                new Thread(() -> handleResult(socket)).start();
            }
        } catch (IOException e) {
            System.out.println("Error listening for results: " + e.getMessage());
        }
    }

    private static void handleResult(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            StringBuilder resultData = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                resultData.append(line);
            }
            System.out.println("Received result: " + resultData.toString());
            // Process the result as needed
        } catch (IOException e) {
            System.out.println("Error handling result: " + e.getMessage());
        }
    }
}





