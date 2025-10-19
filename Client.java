import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class Client {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JTextField usernameField, roomField, ipField;
    private JButton connectButton, sendButton;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread readerThread;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client().createLoginUI());
    }

    // --------------------------- LOGIN SCREEN ---------------------------
    private void createLoginUI() {
        frame = new JFrame("LAN Messenger - Join Room");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(350, 250);
        frame.setLayout(new GridLayout(5, 2, 10, 10));

        JLabel userLabel = new JLabel("Username:");
        JLabel roomLabel = new JLabel("Room ID:");
        JLabel ipLabel = new JLabel("Server IP:");

        usernameField = new JTextField();
        roomField = new JTextField();
        ipField = new JTextField("127.0.0.1"); // default localhost
        connectButton = new JButton("Connect");

        frame.add(userLabel);
        frame.add(usernameField);
        frame.add(roomLabel);
        frame.add(roomField);
        frame.add(ipLabel);
        frame.add(ipField);
        frame.add(new JLabel());
        frame.add(connectButton);

        connectButton.addActionListener(e -> connectToServer());

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // --------------------------- CHAT SCREEN ---------------------------
    private void createChatUI(String username, String roomID, String serverIP) {
        frame.dispose();
        frame = new JFrame("LAN Messenger - Room: " + roomID);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);
        frame.setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        sendButton = new JButton("Send");

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Action listener for sending messages
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        frame.setVisible(true);

        // Start listening for incoming messages
        readerThread = new Thread(this::readMessages);
        readerThread.start();
    }

    // --------------------------- NETWORK LOGIC ---------------------------
    private void connectToServer() {
        String username = usernameField.getText().trim();
        String roomID = roomField.getText().trim();
        String serverIP = ipField.getText().trim();

        if (username.isEmpty() || roomID.isEmpty() || serverIP.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please fill all fields.");
            return;
        }

        try {
            socket = new Socket(serverIP, 5000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Send username and roomID to server
            out.println(username);
            out.println(roomID);

            createChatUI(username, roomID, serverIP);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "❌ Could not connect to server.");
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            messageField.setText("");
        }
    }

    private void readMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                chatArea.append(message + "\n");
            }
        } catch (IOException e) {
            chatArea.append("❌ Disconnected from server.\n");
        }
    }
}
