package com.example.auction.domain.chat.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.chat.dto.ChatMessageListResponse;
import com.example.auction.domain.chat.dto.ChatMessageResponse;
import com.example.auction.domain.chat.dto.ChatRoomResponse;
import com.example.auction.domain.chat.entity.ChatMessage;
import com.example.auction.domain.chat.entity.ChatRoom;
import com.example.auction.domain.chat.entity.MessageRole;
import com.example.auction.domain.chat.exception.ChatErrorEnum;
import com.example.auction.domain.chat.repository.ChatMessageRepository;
import com.example.auction.domain.chat.repository.ChatRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatContextCacheService chatContextCacheService;

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 10L;


    // ========================
    // createRoom
    // ========================

    @Test
    @DisplayName("채팅방 생성 성공")
    void createRoom_success() {
        // given
        ChatRoom chatRoom = ChatRoom.from(USER_ID);
        ReflectionTestUtils.setField(chatRoom, "id", ROOM_ID);

        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(chatRoom);

        // when
        ChatRoomResponse response = chatService.createRoom(USER_ID);

        // then
        assertThat(response.id()).isEqualTo(ROOM_ID);
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }


    // ========================
    // getRooms
    // ========================

    @Test
    @DisplayName("채팅방 목록 조회 성공")
    void getRooms_success() {
        // given
        ChatRoom room1 = ChatRoom.from(USER_ID);
        ReflectionTestUtils.setField(room1, "id", 1L);
        ChatRoom room2 = ChatRoom.from(USER_ID);
        ReflectionTestUtils.setField(room2, "id", 2L);

        given(chatRoomRepository.findAllByUserIdOrderByCreatedAtDesc(USER_ID)).willReturn(List.of(room1, room2));

        // when
        List<ChatRoomResponse> result = chatService.getRooms(USER_ID);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);
    }

    @Test
    @DisplayName("채팅방 목록 조회 성공 - 채팅방 없음")
    void getRooms_empty() {
        // given
        given(chatRoomRepository.findAllByUserIdOrderByCreatedAtDesc(USER_ID)).willReturn(List.of());

        // when
        List<ChatRoomResponse> result = chatService.getRooms(USER_ID);

        // then
        assertThat(result).isEmpty();
    }


    // ========================
    // deleteRoom
    // ========================

    @Test
    @DisplayName("채팅방 삭제 성공 - 메시지 + Redis 캐시 함께 제거")
    void deleteRoom_success() {
        // given
        ChatRoom chatRoom = ChatRoom.from(USER_ID);
        ReflectionTestUtils.setField(chatRoom, "id", ROOM_ID);

        given(chatRoomRepository.existsById(ROOM_ID)).willReturn(true);
        given(chatRoomRepository.findByIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(chatRoom));

        // when
        chatService.deleteRoom(ROOM_ID, USER_ID);

        // then
        verify(chatMessageRepository).deleteAllByRoomId(ROOM_ID);
        verify(chatRoomRepository).deleteById(ROOM_ID);
        verify(chatContextCacheService).evict(ROOM_ID);
    }

    @Test
    @DisplayName("채팅방 삭제 실패 - 채팅방 없음")
    void deleteRoom_fail_roomNotFound() {
        // given
        given(chatRoomRepository.existsById(ROOM_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> chatService.deleteRoom(ROOM_ID, USER_ID))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(ChatErrorEnum.CHAT_ROOM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("채팅방 삭제 실패 - 본인 채팅방 아님")
    void deleteRoom_fail_forbidden() {
        // given
        given(chatRoomRepository.existsById(ROOM_ID)).willReturn(true);
        given(chatRoomRepository.findByIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.deleteRoom(ROOM_ID, USER_ID))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(ChatErrorEnum.CHAT_ROOM_FORBIDDEN.getMessage());
    }


    // ========================
    // getMessages
    // ========================

    @Test
    @DisplayName("메시지 목록 조회 성공 - cursor 없음 (최신부터)")
    void getMessages_success_noCursor() {
        // given
        ChatRoom chatRoom = ChatRoom.from(USER_ID);
        ReflectionTestUtils.setField(chatRoom, "id", ROOM_ID);

        ChatMessage msg1 = ChatMessage.of(ROOM_ID, "안녕", MessageRole.USER);
        ReflectionTestUtils.setField(msg1, "id", 1L);
        ChatMessage msg2 = ChatMessage.of(ROOM_ID, "안녕하세요", MessageRole.ASSISTANT);
        ReflectionTestUtils.setField(msg2, "id", 2L);

        given(chatRoomRepository.existsById(ROOM_ID)).willReturn(true);
        given(chatRoomRepository.findByIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.findByCursor(ROOM_ID, null, 20)).willReturn(List.of(msg1, msg2));

        // when
        ChatMessageListResponse result = chatService.getMessages(ROOM_ID, USER_ID, null, 20);

        // then
        assertThat(result.messages()).hasSize(2);
        assertThat(result.messages().get(0).content()).isEqualTo("안녕");
        assertThat(result.messages().get(1).role()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(result.nextCursor()).isNull(); // 2개 < size(20) → 다음 페이지 없음
    }

    @Test
    @DisplayName("메시지 목록 조회 성공 - cursor 있음 + hasNext")
    void getMessages_success_withCursorAndHasNext() {
        // given
        ChatRoom chatRoom = ChatRoom.from(USER_ID);
        ReflectionTestUtils.setField(chatRoom, "id", ROOM_ID);

        List<ChatMessage> messages = new java.util.ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            ChatMessage m = ChatMessage.of(ROOM_ID, "메시지" + i, MessageRole.USER);
            ReflectionTestUtils.setField(m, "id", (long) i);
            messages.add(m);
        }

        given(chatRoomRepository.existsById(ROOM_ID)).willReturn(true);
        given(chatRoomRepository.findByIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.findByCursor(ROOM_ID, 100L, 20)).willReturn(messages);

        // when
        ChatMessageListResponse result = chatService.getMessages(ROOM_ID, USER_ID, 100L, 20);

        // then
        assertThat(result.messages()).hasSize(20);
        assertThat(result.nextCursor()).isEqualTo(1L); // 첫 번째 메시지 id (가장 오래된 것)
    }

    @Test
    @DisplayName("메시지 목록 조회 실패 - 채팅방 없음")
    void getMessages_fail_roomNotFound() {
        // given
        given(chatRoomRepository.existsById(ROOM_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> chatService.getMessages(ROOM_ID, USER_ID, null, 20))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(ChatErrorEnum.CHAT_ROOM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("메시지 목록 조회 실패 - 본인 채팅방 아님")
    void getMessages_fail_forbidden() {
        // given
        given(chatRoomRepository.existsById(ROOM_ID)).willReturn(true);
        given(chatRoomRepository.findByIdAndUserId(ROOM_ID, USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.getMessages(ROOM_ID, USER_ID, null, 20))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(ChatErrorEnum.CHAT_ROOM_FORBIDDEN.getMessage());
    }
}