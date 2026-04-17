package com.example.auction.domain.chat.entity;

import com.example.auction.common.entity.CreatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends CreatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;

    @Column(nullable = false)
    private Long roomId;

    public static ChatMessage of(Long roomId, String content, MessageRole role) {
        ChatMessage message = new ChatMessage();
        message.roomId = roomId;
        message.content = content;
        message.role = role;
        return message;
    }
}