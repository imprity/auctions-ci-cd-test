package com.example.auction.domain.chat.dto;

import java.util.List;

public record ChatMessageListResponse(
        List<ChatMessageResponse> messages,
        Long nextCursor  // 다음 페이지 커서 (null이면 마지막 페이지)
) {
    public static ChatMessageListResponse of(List<ChatMessageResponse> messages, int size) {
        // 요청한 size만큼 왔으면 다음 페이지 존재 — 가장 오래된 메시지 id를 커서로 (id < cursor 조건으로 더 오래된 메시지 조회)
        boolean hasNext = messages.size() == size;
        Long nextCursor = hasNext ? messages.get(0).id() : null;
        return new ChatMessageListResponse(messages, nextCursor);
    }
}