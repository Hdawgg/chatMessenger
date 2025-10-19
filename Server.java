import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 5000;
    // Map: roomID -> list of clients
    private static Map<String, List<ClientHandler>> rooms = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("🌐 Server started on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("✅ New client connected: " + socket.getInetAddress());
                new Thread(new ClientHandler(socket, rooms)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
