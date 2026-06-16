package com.accenture.springai_bootcamp_demo.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A conversation aggregate that owns an ordered list of chatMessages.
 */
@Entity
@Table(name = "chats")
@Getter
@Setter
@NoArgsConstructor
public class Chat {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<ChatMessage> chatMessages = new ArrayList<>();

    public static Chat create(String title) {
        Chat chat = new Chat();
        Instant now = Instant.now();
        chat.id = UUID.randomUUID().toString();
        chat.title = title;
        chat.createdAt = now;
        chat.updatedAt = now;
        return chat;
    }

    /**
     * Appends a message, keeping both sides of the relationship in sync.
     */
    public void addMessage(ChatMessage chatMessage) {
        chatMessage.setChat(this);
        this.chatMessages.add(chatMessage);
        this.updatedAt = Instant.now();
    }
}
