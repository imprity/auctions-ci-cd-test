package com.example.auction.domain.chat.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.chat.dto.ChatMessageListResponse;
import com.example.auction.domain.chat.dto.ChatMessageResponse;
import com.example.auction.domain.chat.dto.ChatRoomResponse;
import com.example.auction.domain.chat.entity.ChatRoom;
import com.example.auction.domain.chat.exception.ChatErrorEnum;
import com.example.auction.domain.chat.repository.ChatMessageRepository;
import com.example.auction.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatContextCacheService chatContextCacheService;

    // 채팅방 생성
    @Transactional
    public ChatRoomResponse createRoom(Long userId) {
        ChatRoom chatRoom = ChatRoom.from(userId);
        return ChatRoomResponse.from(chatRoomRepository.save(chatRoom));
    }

    // 내 채팅방 목록 조회
    public List<ChatRoomResponse> getRooms(Long userId) {
        return chatRoomRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ChatRoomResponse::from)
                .toList();
    }

    // 채팅방 삭제 (소유자 검증 + 메시지 cascade 하드딜리트 + Redis 캐시 evict)
    @Transactional
    public void deleteRoom(Long roomId, Long userId) {
        validateRoomOwner(roomId, userId);
        chatMessageRepository.deleteAllByRoomId(roomId);
        chatRoomRepository.deleteById(roomId);
        chatContextCacheService.evict(roomId); // 채팅방 삭제 시 컨텍스트 캐시도 함께 제거
    }

    // 메시지 목록 조회 (커서 기반 페이징)
    public ChatMessageListResponse getMessages(Long roomId, Long userId, Long cursor, int size) {
        validateRoomOwner(roomId, userId);
        List<ChatMessageResponse> messages = chatMessageRepository.findByCursor(roomId, cursor, size)
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
        return ChatMessageListResponse.of(messages, size);
    }

    // 채팅방 존재 여부 + 소유자 검증
    private void validateRoomOwner(Long roomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ServiceErrorException(ChatErrorEnum.CHAT_ROOM_NOT_FOUND));
        if (!chatRoom.getUserId().equals(userId)) {
            throw new ServiceErrorException(ChatErrorEnum.CHAT_ROOM_FORBIDDEN);
        }
    }
}