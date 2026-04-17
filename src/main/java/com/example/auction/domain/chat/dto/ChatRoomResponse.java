package com.example.auction.domain.chat.dto;

import com.example.auction.domain.chat.entity.ChatRoom;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long id,
        String title,
        LocalDateTime createdAt
) {
    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getTitle(),
                chatRoom.getCreatedAt()
        );
    }
}
