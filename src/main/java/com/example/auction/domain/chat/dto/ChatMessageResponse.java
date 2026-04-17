package com.example.auction.domain.chat.dto;

import com.example.auction.domain.chat.entity.ChatMessage;
import com.example.auction.domain.chat.entity.MessageRole;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        String content,
        MessageRole role,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getContent(),
                chatMessage.getRole(),
                chatMessage.getCreatedAt()
        );
    }
}