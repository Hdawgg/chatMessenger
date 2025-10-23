import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 5555;
    private static ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    
    static class Room {
        String roomId;
        String roomName;
        String password;
        Set<String> members;
        
        public Room(String roomId, String roomName, String password) {
            this.roomId = roomId;
            this.roomName = roomName;
            this.password = password;
            this.members = ConcurrentHashMap.newKeySet();
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Chat Server with Rooms Started ===");
        System.out.println("Listening on port: " + PORT);
        System.out.println("Waiting for clients...\n");
        
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
        private String currentRoom;
        
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
                    sendRoomList();
                    break;
                    
                case CREATE_ROOM:
                    String roomId = message.getRoomId();
                    String roomName = message.getRoomName();
                    String password = message.getPassword();
                    
                    if (rooms.containsKey(roomId)) {
                        sendMessage(new Message(Message.MessageType.NOTIFICATION, 
                            "Server", "Room ID already exists!", null));
                    } else {
                        Room newRoom = new Room(roomId, roomName, password);
                        rooms.put(roomId, newRoom);
                        System.out.println("Room created: " + roomName + " (" + roomId + ")" + 
                            (password != null && !password.isEmpty() ? " with password" : ""));
                        
                        sendMessage(new Message(Message.MessageType.NOTIFICATION, 
                            "Server", "Room '" + roomName + "' created successfully!", roomId));
                        
                        sendRoomList();
                    }
                    break;
                    
                case JOIN_ROOM:
                    roomId = message.getRoomId();
                    Room room = rooms.get(roomId);
                    
                    if (room != null) {
                        String providedPassword = message.getPassword();
                        
                        if (room.password == null || room.password.isEmpty()) {
                            joinRoom(room, roomId);
                        } else if (providedPassword != null && providedPassword.equals(room.password)) {
                            joinRoom(room, roomId);
                        } else {
                            sendMessage(new Message(Message.MessageType.PASSWORD_INCORRECT, 
                                "Server", "Incorrect password!", roomId));
                        }
                    } else {
                        sendMessage(new Message(Message.MessageType.NOTIFICATION, 
                            "Server", "Room does not exist!", null));
                    }
                    break;
                    
                case TEXT:
                    if (currentRoom != null) {
                        System.out.println("[" + currentRoom + "] " + username + ": " + message.getContent());
                        broadcastToRoom(currentRoom, message, username);
                    } else {
                        sendMessage(new Message(Message.MessageType.NOTIFICATION, 
                            "Server", "You must join a room first!", null));
                    }
                    break;
                    
                case LEAVE_ROOM:
                    leaveCurrentRoom();
                    break;
                    
                case ROOM_LIST:
                    sendRoomList();
                    break;
                    
                case DISCONNECT:
                    cleanup();
                    break;
            }
        }
        
        private void joinRoom(Room room, String roomId) throws IOException {
            if (currentRoom != null) {
                leaveCurrentRoom();
            }
            
            currentRoom = roomId;
            room.members.add(username);
            System.out.println(username + " joined room: " + room.roomName);
            
            sendMessage(new Message(Message.MessageType.NOTIFICATION, 
                "Server", "Joined room: " + room.roomName, roomId));
            
            broadcastToRoom(roomId, new Message(Message.MessageType.NOTIFICATION, 
                "Server", username + " joined the room", roomId), username);
            
            broadcastRoomListToAll();
        }
        
        private void leaveCurrentRoom() throws IOException {
            if (currentRoom != null) {
                Room room = rooms.get(currentRoom);
                if (room != null) {
                    room.members.remove(username);
                    broadcastToRoom(currentRoom, new Message(Message.MessageType.NOTIFICATION, 
                        "Server", username + " left the room", currentRoom), username);
                    
                    broadcastRoomListToAll();
                }
                currentRoom = null;
            }
        }
        
        private void sendRoomList() throws IOException {
            StringBuilder roomList = new StringBuilder();
            for (Room room : rooms.values()) {
                roomList.append(room.roomId).append("|")
                       .append(room.roomName).append("|")
                       .append(room.members.size()).append(";");
            }
            sendMessage(new Message(Message.MessageType.ROOM_LIST, 
                "Server", roomList.toString()));
        }
        
        private static void broadcastRoomListToAll() {
            StringBuilder roomList = new StringBuilder();
            for (Room room : rooms.values()) {
                roomList.append(room.roomId).append("|")
                       .append(room.roomName).append("|")
                       .append(room.members.size()).append(";");
            }
            
            Message roomListMsg = new Message(Message.MessageType.ROOM_LIST, 
                "Server", roomList.toString());
            
            for (ClientHandler client : clients.values()) {
                try {
                    client.sendMessage(roomListMsg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        private void broadcastToRoom(String roomId, Message message, String excludeUser) {
            Room room = rooms.get(roomId);
            if (room != null) {
                for (String member : room.members) {
                    if (!member.equals(excludeUser)) {
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
                if (currentRoom != null) {
                    leaveCurrentRoom();
                }
                if (username != null) {
                    clients.remove(username);
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
