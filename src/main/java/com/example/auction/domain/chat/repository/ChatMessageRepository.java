package com.example.auction.domain.chat.repository;

import com.example.auction.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageRepositoryCustom {

    // 채팅방 삭제 시 메시지 bulk 하드딜리트
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ChatMessage m WHERE m.roomId = :roomId")
    void deleteAllByRoomId(@Param("roomId") Long roomId);

    // 스케줄러 - 30일 이전 메시지 bulk 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ChatMessage m WHERE m.createdAt < :dateTime")
    void deleteAllByCreatedAtBefore(@Param("dateTime") LocalDateTime dateTime);
}