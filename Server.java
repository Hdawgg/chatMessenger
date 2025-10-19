

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 5555;
    private static ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Set<String>> roomClients = new ConcurrentHashMap<>();
    private static DatabaseManager db;
    
    public static void main(String[] args) {
        db = new DatabaseManager();
        System.out.println("Chat Server started on port " + PORT);
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String username;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                
                while (true) {
                    Message message = (Message) in.readObject();
                    handleMessage(message);
                }
            } catch (Exception e) {
                System.out.println("Client disconnected: " + username);
            } finally {
                cleanup();
            }
        }
        
        private void handleMessage(Message message) throws IOException {
            switch (message.getType()) {
                case CONNECT:
                    username = message.getSender();
                    clients.put(username, this);
                    System.out.println(username + " connected");
                    break;
                    
                case CREATE_ROOM:
                    String roomId = message.getRoomId();
                    String roomName = message.getRoomName();
                    if (db.createRoom(roomId, roomName, username)) {
                        int userId = db.getUserId(username);
                        db.joinRoom(userId, roomId);
                        roomClients.putIfAbsent(roomId, ConcurrentHashMap.newKeySet());
                        roomClients.get(roomId).add(username);
                        sendMessage(new Message(Message.MessageType.NOTIFICATION, "Server", 
                            "Room created successfully", roomId));
                    } else {
                        sendMessage(new Message(Message.MessageType.NOTIFICATION, "Server", 
                            "Room ID already exists", null));
                    }
                    break;
                    
                case JOIN_ROOM:
                    roomId = message.getRoomId();
                    if (db.roomExists(roomId)) {
                        int userId = db.getUserId(username);
                        db.joinRoom(userId, roomId);
                        roomClients.putIfAbsent(roomId, ConcurrentHashMap.newKeySet());
                        roomClients.get(roomId).add(username);
                        
                        // Send room history
                        List<String> history = db.getRoomMessages(roomId);
                        Message historyMsg = new Message(Message.MessageType.ROOM_HISTORY, 
                            "Server", String.join("\n", history), roomId);
                        sendMessage(historyMsg);
                        
                        // Notify room members
                        broadcastToRoom(roomId, new Message(Message.MessageType.NOTIFICATION, 
                            "Server", username + " joined the room", roomId), username);
                    } else {
                        sendMessage(new Message(Message.MessageType.NOTIFICATION, "Server", 
                            "Room does not exist", null));
                    }
                    break;
                    
                case MESSAGE:
                    roomId = message.getRoomId();
                    db.saveMessage(roomId, username, message.getContent());
                    broadcastToRoom(roomId, message, username);  // ← Exclude sender
                    break;

                    
                case ROOM_LIST:
                    int userId = db.getUserId(username);
                    List<String> rooms = db.getUserRooms(userId);
                    Message roomListMsg = new Message(Message.MessageType.ROOM_LIST, 
                        "Server", String.join(";", rooms));
                    sendMessage(roomListMsg);
                    break;
                    
                case DISCONNECT:
                    cleanup();
                    break;
            }
        }
        
        private void broadcastToRoom(String roomId, Message message, String exclude) {
            Set<String> roomMembers = roomClients.get(roomId);
            if (roomMembers != null) {
                for (String member : roomMembers) {
                    if (!member.equals(exclude)) {
                        ClientHandler handler = clients.get(member);
                        if (handler != null) {
                            try {
                                handler.sendMessage(message);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        
        private void sendMessage(Message message) throws IOException {
            out.writeObject(message);
            out.flush();
        }
        
        private void cleanup() {
            try {
                if (username != null) {
                    clients.remove(username);
                    // Remove from all rooms
                    for (Set<String> roomMembers : roomClients.values()) {
                        roomMembers.remove(username);
                    }
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

