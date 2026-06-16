package com.accenture.springai_bootcamp_demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single utterance within a {@link Chat}.
 */
@Entity
@Table(name = "chatMessages")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, length = 8000)
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static ChatMessage of(Role role, String content) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.role = role;
        chatMessage.content = content;
        chatMessage.createdAt = Instant.now();
        return chatMessage;
    }
}
