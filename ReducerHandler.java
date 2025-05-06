import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ReducerHandler extends Thread {
    private Socket clientSocket;
    private Map<Integer, List<String>> resultMap;

    public ReducerHandler(Socket clientSocket, Map<Integer, List<String>> resultMap) {
        this.clientSocket = clientSocket;
        this.resultMap = resultMap;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String result;
            while ((result = in.readLine()) != null) {
                JSONObject json = new JSONObject(result);
                int mapId = json.getInt("mapId");
                JSONArray dataArray = json.getJSONArray("data");
                List<String> dataEntries = new ArrayList<>();
                for (int i = 0; i < dataArray.length(); i++) {
                    dataEntries.add(dataArray.getJSONObject(i).toString());
                }
                resultMap.put(mapId, dataEntries);
                System.out.println("Received results for mapId " + mapId + ": " + dataEntries);
                sendResultToMaster(mapId, dataEntries); // Send results to the master server
            }
        } catch (IOException e) {
            System.out.println("Error on connection: " + e.getMessage());
        } catch (JSONException e) {
            System.out.println("JSON parsing error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    private void sendResultToMaster(int mapId, List<String> dataEntries) {
        try (Socket masterSocket = new Socket("127.0.0.1", 4322); // Master server address and result port
             PrintWriter out = new PrintWriter(masterSocket.getOutputStream(), true)) {
            JSONObject resultJson = new JSONObject();
            resultJson.put("mapId", mapId);
            resultJson.put("data", new JSONArray(dataEntries));
            out.println(resultJson.toString());
        } catch (IOException e) {
            System.out.println("Error sending results to master: " + e.getMessage());
        }
    }
}

