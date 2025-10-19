

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatFrame extends JFrame {
    private String username;
    private String currentRoomId;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private DatabaseManager db;
    
    // UI Components
    private DefaultListModel<String> roomListModel;
    private JList<String> roomList;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JLabel currentRoomLabel;
    
    public ChatFrame(String username) {
        this.username = username;
        this.db = new DatabaseManager();
        initComponents();
        connectToServer();
    }
    
    private void initComponents() {
        setTitle("Chat Messenger - " + username);
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Main layout
        setLayout(new BorderLayout());
        
        // Sidebar (Left Panel)
        JPanel sidebarPanel = createSidebarPanel();
        add(sidebarPanel, BorderLayout.WEST);
        
        // Chat Panel (Center)
        JPanel chatPanel = createChatPanel();
        add(chatPanel, BorderLayout.CENTER);
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
    }
    
    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(250, 600));
        sidebar.setBackground(new Color(43, 43, 43));
        sidebar.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(43, 43, 43));
        headerPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        JLabel roomsLabel = new JLabel("Rooms");
        roomsLabel.setFont(new Font("Arial", Font.BOLD, 18));
        roomsLabel.setForeground(Color.WHITE);
        headerPanel.add(roomsLabel, BorderLayout.WEST);
        
        // Add room button
        JButton addRoomButton = new JButton("+");
        addRoomButton.setFont(new Font("Arial", Font.BOLD, 20));
        addRoomButton.setBackground(new Color(63, 81, 181));
        addRoomButton.setForeground(Color.WHITE);
        addRoomButton.setFocusPainted(false);
        addRoomButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addRoomButton.setPreferredSize(new Dimension(45, 35));
        addRoomButton.addActionListener(e -> showAddRoomDialog());
        headerPanel.add(addRoomButton, BorderLayout.EAST);
        
        sidebar.add(headerPanel, BorderLayout.NORTH);
        
        // Room list
        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);
        roomList.setFont(new Font("Arial", Font.PLAIN, 14));
        roomList.setBackground(new Color(60, 60, 60));
        roomList.setForeground(Color.WHITE);
        roomList.setSelectionBackground(new Color(63, 81, 181));
        roomList.setSelectionForeground(Color.WHITE);
        roomList.setBorder(new EmptyBorder(5, 5, 5, 5));
        roomList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                joinSelectedRoom();
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(roomList);
        scrollPane.setBorder(null);
        sidebar.add(scrollPane, BorderLayout.CENTER);
        
        loadUserRooms();
        
        return sidebar;
    }
    
    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(Color.WHITE);
        
        // Chat header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(63, 81, 181));
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        currentRoomLabel = new JLabel("Select a room to start chatting");
        currentRoomLabel.setFont(new Font("Arial", Font.BOLD, 16));
        currentRoomLabel.setForeground(Color.WHITE);
        headerPanel.add(currentRoomLabel, BorderLayout.WEST);
        
        chatPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setMargin(new Insets(10, 10, 10, 10));
        
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        
        // Message input panel
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(8, 10, 8, 10)
        ));
        messageField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
        
        sendButton = new JButton("Send");
        sendButton.setFont(new Font("Arial", Font.BOLD, 14));
        sendButton.setBackground(new Color(63, 81, 181));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.setPreferredSize(new Dimension(100, 40));
        sendButton.addActionListener(e -> sendMessage());
        
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        
        return chatPanel;
    }
    
    private void connectToServer() {
        try {
            socket = new Socket("localhost", 5555);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            // Send connection message
            Message connectMsg = new Message(Message.MessageType.CONNECT, username, "");
            out.writeObject(connectMsg);
            out.flush();
            
            // Start message listener thread
            new Thread(new MessageListener()).start();
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Could not connect to server. Please ensure the server is running.", 
                "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    private void loadUserRooms() {
        int userId = db.getUserId(username);
        java.util.List<String> rooms = db.getUserRooms(userId);
        roomListModel.clear();
        for (String room : rooms) {
            String[] parts = room.split("\\|");
            roomListModel.addElement(parts[1] + " (" + parts[0] + ")");
        }
    }
    
    private void showAddRoomDialog() {
        String[] options = {"Create Room", "Join Room", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, 
            "What would you like to do?", 
            "Room Options",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null, options, options[2]);
        
        if (choice == 0) {
            createRoom();
        } else if (choice == 1) {
            joinRoom();
        }
    }
    
    private void createRoom() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        JTextField roomIdField = new JTextField();
        JTextField roomNameField = new JTextField();
        
        panel.add(new JLabel("Room ID:"));
        panel.add(roomIdField);
        panel.add(new JLabel("Room Name:"));
        panel.add(roomNameField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Create New Room", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String roomId = roomIdField.getText().trim();
            String roomName = roomNameField.getText().trim();
            
            if (!roomId.isEmpty() && !roomName.isEmpty()) {
                try {
                    Message msg = new Message(Message.MessageType.CREATE_ROOM, username, "");
                    msg.setRoomId(roomId);
                    msg.setRoomName(roomName);
                    out.writeObject(msg);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please fill in all fields", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void joinRoom() {
        String roomId = JOptionPane.showInputDialog(this, 
            "Enter Room ID to join:", "Join Room", JOptionPane.PLAIN_MESSAGE);
        
        if (roomId != null && !roomId.trim().isEmpty()) {
            try {
                Message msg = new Message(Message.MessageType.JOIN_ROOM, username, "", roomId.trim());
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void joinSelectedRoom() {
        String selected = roomList.getSelectedValue();
        if (selected != null) {
            String roomId = selected.substring(selected.lastIndexOf("(") + 1, selected.lastIndexOf(")"));
            String roomName = selected.substring(0, selected.lastIndexOf("(")).trim();
            
            currentRoomId = roomId;
            currentRoomLabel.setText(roomName);
            chatArea.setText("");
            
            // Request room history
            try {
                Message msg = new Message(Message.MessageType.JOIN_ROOM, username, "", roomId);
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void sendMessage() {
    String messageText = messageField.getText().trim();
    
    if (!messageText.isEmpty() && currentRoomId != null) {
        try {
            Message msg = new Message(Message.MessageType.MESSAGE, username, messageText, currentRoomId);
            out.writeObject(msg);
            out.flush();
            messageField.setText("");  // Just clear the field
        } catch (IOException e) {
            e.printStackTrace();
        }
    } else if (currentRoomId == null) {
        JOptionPane.showMessageDialog(this, "Please select a room first", 
            "No Room Selected", JOptionPane.WARNING_MESSAGE);
    }
}

    
    private void disconnect() {
        try {
            if (out != null) {
                Message msg = new Message(Message.MessageType.DISCONNECT, username, "");
                out.writeObject(msg);
                out.flush();
            }
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    class MessageListener implements Runnable {
        public void run() {
            try {
                while (true) {
                    Message message = (Message) in.readObject();
                    handleIncomingMessage(message);
                }
            } catch (Exception e) {
                System.out.println("Disconnected from server");
            }
        }
    }
    
    private void handleIncomingMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            switch (message.getType()) {
                case MESSAGE:
                    if (message.getRoomId().equals(currentRoomId)) {
                        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
                        chatArea.append(String.format("[%s] %s: %s\n", 
                            timestamp, message.getSender(), message.getContent()));
                    }
                    break;
                    
                case NOTIFICATION:
                    if (message.getRoomId() != null && message.getRoomId().equals(currentRoomId)) {
                        chatArea.append("*** " + message.getContent() + " ***\n");
                    }
                    loadUserRooms();
                    break;
                    
                case ROOM_HISTORY:
                    if (message.getRoomId().equals(currentRoomId)) {
                        chatArea.setText(message.getContent() + "\n");
                    }
                    break;
            }
        });
    }
}

