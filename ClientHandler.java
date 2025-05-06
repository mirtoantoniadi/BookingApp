import java.io.*;
import java.net.*;
import java.util.List;
import org.json.JSONObject;

class ClientHandler extends Thread {
    private Socket clientSocket; // Socket to communicate with the client (manager/renter)
    private List<WorkerNode> workerNodes; // List of worker nodes available for processing requests

    // Constructor to initialize the client socket and the list of worker nodes
    public ClientHandler(Socket clientSocket, List<WorkerNode> workerNodes) {
        this.clientSocket = clientSocket;
        this.workerNodes = workerNodes;
    }

    @Override
    public void run() {
        try {
            // Setup input stream to receive data from client
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String inputLine;
            StringBuilder jsonData = new StringBuilder();

            // Read all data sent by the client
            while ((inputLine = in.readLine()) != null) {
                jsonData.append(inputLine);
            }

            // Parse the received data as JSON
            JSONObject jsonObject = new JSONObject(jsonData.toString());
            
            // Check the type of request from the client
            if (jsonObject.has("type") && jsonObject.getString("type").equals("search")) {
                // If it's a search type, broadcast the search criteria to all worker nodes
                MasterServer.broadcastSearchCriteriaWithMapId(jsonData.toString());
            } else {
                // Otherwise, handle as room data, extract room name and forward to the appropriate worker node
                String roomName = extractRoomNameFromJson(jsonData.toString());
                WorkerNode node = selectWorkerNode(roomName);
                if (node != null) {
                    forwardDataToWorker(node, jsonData.toString());
                } else {
                    System.out.println("No valid worker node found to forward data.");
                }
            }

            // Close client socket after handling the request
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port or listening for a connection");
            System.out.println(e.getMessage());
        }
    }

    // Extracts the room name from JSON data
    private String extractRoomNameFromJson(String json) {
        String roomName = "";
        try {
            int start = json.indexOf("\"roomName\": \"") + "\"roomName\": \"".length();
            int end = json.indexOf("\"", start);
            roomName = json.substring(start, end);
        } catch (Exception e) {
            System.out.println("Error extracting room name from JSON: " + e.getMessage());
        }
        return roomName;
    }

    // Selects a worker node based on the hash of the room name to evenly distribute the load
    private WorkerNode selectWorkerNode(String roomName) {
        if (workerNodes.isEmpty()) {
            System.out.println("No worker nodes are available.");
            return null;  // Return null if no worker nodes are available
        }
        int hash = Math.abs(roomName.hashCode()); // Compute hash of the room name
        int index = hash % workerNodes.size(); // Use modulo to find index for load balancing
        return workerNodes.get(index); // Return the selected worker node
    }
    
    // Forwards the data to the selected worker node
    private void forwardDataToWorker(WorkerNode node, String data) {
        try (Socket socket = new Socket(node.host, node.port); 
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(data); 
        } catch (IOException e) {
            System.out.println("Exception in forwarding data to worker: " + e.getMessage());
        }
    }
    
}


