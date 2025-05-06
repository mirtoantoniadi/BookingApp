import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Reducer {
    private static Map<Integer, List<String>> resultMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = 5555;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Reducer listening on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                ReducerHandler reducerHandler = new ReducerHandler(socket, resultMap);
                reducerHandler.start();
            }
        } catch (IOException e) {
            System.out.println("Reducer error: " + e.getMessage());
        }
    }
}



