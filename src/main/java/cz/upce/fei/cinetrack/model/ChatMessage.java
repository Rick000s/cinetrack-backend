package cz.upce.fei.cinetrack.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String senderName;
    private String text;
    private String createdAt;
}
