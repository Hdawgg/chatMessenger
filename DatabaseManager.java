
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:chat.db";
    private Connection conn;
    
    public DatabaseManager() {
        try {
            conn = DriverManager.getConnection(DB_URL);
            createTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void createTables() {
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL
            )
        """;
        
        String createRoomsTable = """
            CREATE TABLE IF NOT EXISTS rooms (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                room_id TEXT UNIQUE NOT NULL,
                room_name TEXT NOT NULL,
                created_by TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String createMessagesTable = """
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                room_id TEXT NOT NULL,
                username TEXT NOT NULL,
                message TEXT NOT NULL,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(room_id) REFERENCES rooms(room_id)
            )
        """;
        
        String createUserRoomsTable = """
            CREATE TABLE IF NOT EXISTS user_rooms (
                user_id INTEGER,
                room_id TEXT,
                joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY(user_id, room_id),
                FOREIGN KEY(user_id) REFERENCES users(id),
                FOREIGN KEY(room_id) REFERENCES rooms(room_id)
            )
        """;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createRoomsTable);
            stmt.execute(createMessagesTable);
            stmt.execute(createUserRoomsTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
    
    public boolean authenticateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }
    
    public int getUserId(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public boolean createRoom(String roomId, String roomName, String createdBy) {
        String sql = "INSERT INTO rooms (room_id, room_name, created_by) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, roomId);
            pstmt.setString(2, roomName);
            pstmt.setString(3, createdBy);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
    
    public boolean joinRoom(int userId, String roomId) {
        String sql = "INSERT OR IGNORE INTO user_rooms (user_id, room_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, roomId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
    
    public List<String> getUserRooms(int userId) {
        List<String> rooms = new ArrayList<>();
        String sql = """
            SELECT r.room_id, r.room_name FROM rooms r
            JOIN user_rooms ur ON r.room_id = ur.room_id
            WHERE ur.user_id = ?
            ORDER BY ur.joined_at DESC
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                rooms.add(rs.getString("room_id") + "|" + rs.getString("room_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }
    
    public void saveMessage(String roomId, String username, String message) {
        String sql = "INSERT INTO messages (room_id, username, message) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, roomId);
            pstmt.setString(2, username);
            pstmt.setString(3, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public List<String> getRoomMessages(String roomId) {
        List<String> messages = new ArrayList<>();
        String sql = "SELECT username, message, timestamp FROM messages WHERE room_id = ? ORDER BY timestamp ASC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, roomId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String msg = String.format("[%s] %s: %s", 
                    rs.getString("timestamp"), 
                    rs.getString("username"), 
                    rs.getString("message"));
                messages.add(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }
    
    public boolean roomExists(String roomId) {
        String sql = "SELECT COUNT(*) FROM rooms WHERE room_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, roomId);
            ResultSet rs = pstmt.executeQuery();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }
    
    public void close() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
