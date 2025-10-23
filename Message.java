import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        CONNECT, DISCONNECT, TEXT, JOIN_ROOM, CREATE_ROOM, 
        LEAVE_ROOM, ROOM_LIST, NOTIFICATION, ROOM_USERS, PASSWORD_INCORRECT
    }
    
    private MessageType type;
    private String sender;
    private String content;
    private String roomId;
    private String roomName;
    private String password;
    
    public Message(MessageType type, String sender, String content, String roomId) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.roomId = roomId;
    }
    
    public Message(MessageType type, String sender, String content) {
        this(type, sender, content, null);
    }
    
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
