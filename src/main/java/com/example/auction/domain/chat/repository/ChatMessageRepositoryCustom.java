package com.example.auction.domain.chat.repository;

import com.example.auction.domain.chat.entity.ChatMessage;

import java.util.List;

public interface ChatMessageRepositoryCustom {

    // 커서 기반 페이징 - cursor(마지막 메시지 id)보다 이전 메시지 조회
    List<ChatMessage> findByCursor(Long roomId, Long cursor, int size);

    // Redis 캐싱용 - AI 컨텍스트에 넘길 최근 N개
    List<ChatMessage> findRecentByRoomId(Long roomId, int limit);
}
