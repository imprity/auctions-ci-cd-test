package com.example.auction.domain.chat.repository;

import com.example.auction.domain.chat.entity.ChatMessage;
import com.example.auction.domain.chat.entity.QChatMessage;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QChatMessage chatMessage = QChatMessage.chatMessage;

    // 커서 기반 페이징 - cursor가 null이면 최신부터, 있으면 해당 id 이전 메시지 조회 (ASC 반환)
    @Override
    public List<ChatMessage> findByCursor(Long roomId, Long cursor, int size) {
        List<ChatMessage> messages = queryFactory
                .selectFrom(chatMessage)
                .where(
                        chatMessage.roomId.eq(roomId),
                        cursor != null ? chatMessage.id.lt(cursor) : null
                )
                .orderBy(chatMessage.id.desc())
                .limit(size)
                .fetch();
        Collections.reverse(messages);
        return messages;
    }

    // Redis 캐싱용 - AI 컨텍스트에 넘길 최근 N개 (시간순 정렬로 반환)
    @Override
    public List<ChatMessage> findRecentByRoomId(Long roomId, int limit) {
        List<ChatMessage> messages = queryFactory
                .selectFrom(chatMessage)
                .where(chatMessage.roomId.eq(roomId))
                .orderBy(chatMessage.id.desc())
                .limit(limit)
                .fetch();
        Collections.reverse(messages);
        return messages;
    }
}