import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private String username;
    private String roomID;
    private BufferedReader in;
    private PrintWriter out;
    private Map<String, List<ClientHandler>> rooms;

    public ClientHandler(Socket socket, Map<String, List<ClientHandler>> rooms) {
        this.socket = socket;
        this.rooms = rooms;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Step 1: Receive username and roomID
            this.username = in.readLine();
            this.roomID = in.readLine();

            rooms.putIfAbsent(roomID, new ArrayList<>());
            rooms.get(roomID).add(this);

            broadcast(roomID, "🔔 " + username + " joined the room.");

            String message;
            while ((message = in.readLine()) != null) {
                broadcast(roomID, "[" + username + "]: " + message);
            }
        } catch (IOException e) {
            System.out.println("❌ Connection lost for " + username);
        } finally {
            leaveRoom();
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void broadcast(String roomID, String message) {
        List<ClientHandler> clients = rooms.get(roomID);
        if (clients != null) {
            for (ClientHandler client : clients) {
                client.out.println(message);
            }
        }
    }

    private void leaveRoom() {
        List<ClientHandler> clients = rooms.get(roomID);
        if (clients != null) {
            clients.remove(this);
            broadcast(roomID, "🚪 " + username + " left the room.");
        }
    }
}
