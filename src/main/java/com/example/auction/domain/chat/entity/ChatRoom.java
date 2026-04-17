package com.example.auction.domain.chat.entity;

import com.example.auction.common.entity.CreatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chat_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends CreatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String title;

    @Column(nullable = false)
    private Long userId;

    public static ChatRoom from(Long userId) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.userId = userId;
        return chatRoom;
    }

    public void updateTitle(String title) {
        this.title = title;
    }
}