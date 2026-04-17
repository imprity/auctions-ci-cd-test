package com.example.auction.domain.chat.repository;

import com.example.auction.common.config.JpaConfig;
import com.example.auction.common.config.QuerydslConfig;
import com.example.auction.domain.chat.entity.ChatMessage;
import com.example.auction.domain.chat.entity.MessageRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QuerydslConfig.class, JpaConfig.class})
class ChatMessageRepositoryImplTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private static final Long ROOM_ID = 1L;

    @BeforeEach
    void setUp() {
        // 순서대로 저장 — id는 자동 증가이므로 저장 순서가 곧 시간 순서
        chatMessageRepository.save(ChatMessage.of(ROOM_ID, "질문1", MessageRole.USER));
        chatMessageRepository.save(ChatMessage.of(ROOM_ID, "답변1", MessageRole.ASSISTANT));
        chatMessageRepository.save(ChatMessage.of(ROOM_ID, "질문2", MessageRole.USER));
        chatMessageRepository.save(ChatMessage.of(ROOM_ID, "답변2", MessageRole.ASSISTANT));
        chatMessageRepository.save(ChatMessage.of(ROOM_ID, "질문3", MessageRole.USER));
    }


    // ========================
    // findByCursor
    // ========================

    @Test
    @DisplayName("findByCursor - cursor 없음: 최신 N개 ASC 반환")
    void findByCursor_noCursor() {
        // when
        List<ChatMessage> result = chatMessageRepository.findByCursor(ROOM_ID, null, 3);

        // then — DESC limit 3(질문3,답변2,질문2) → reverse → ASC(질문2,답변2,질문3)
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getContent()).isEqualTo("질문2");
        assertThat(result.get(1).getContent()).isEqualTo("답변2");
        assertThat(result.get(2).getContent()).isEqualTo("질문3");
    }

    @Test
    @DisplayName("findByCursor - cursor 있음: cursor id 이전 메시지 ASC 반환")
    void findByCursor_withCursor() {
        // given — 전체 메시지 id 목록 확인
        List<ChatMessage> all = chatMessageRepository.findAll();
        Long thirdId = all.get(2).getId(); // 3번째 메시지 id (cursor로 사용)

        // when — thirdId 이전 메시지 2개
        List<ChatMessage> result = chatMessageRepository.findByCursor(ROOM_ID, thirdId, 2);

        // then — thirdId보다 이전 메시지 중 최신 2개, ASC 정렬
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(m -> m.getId() < thirdId);
        // ASC 정렬 확인
        assertThat(result.get(0).getId()).isLessThan(result.get(1).getId());
    }

    @Test
    @DisplayName("findByCursor - 해당 채팅방 메시지 없음: 빈 리스트 반환")
    void findByCursor_emptyRoom() {
        // when
        List<ChatMessage> result = chatMessageRepository.findByCursor(999L, null, 20);

        // then
        assertThat(result).isEmpty();
    }


    // ========================
    // findRecentByRoomId
    // ========================

    @Test
    @DisplayName("findRecentByRoomId - limit 이하 메시지: 전체 시간순 반환")
    void findRecentByRoomId_lessThanLimit() {
        // when
        List<ChatMessage> result = chatMessageRepository.findRecentByRoomId(ROOM_ID, 10);

        // then
        assertThat(result).hasSize(5);
        // ASC 정렬 확인
        assertThat(result.get(0).getId()).isLessThan(result.get(4).getId());
        assertThat(result.get(0).getContent()).isEqualTo("질문1");
        assertThat(result.get(4).getContent()).isEqualTo("질문3");
    }

    @Test
    @DisplayName("findRecentByRoomId - limit 초과: 최근 N개만 시간순 반환")
    void findRecentByRoomId_exceedsLimit() {
        // when
        List<ChatMessage> result = chatMessageRepository.findRecentByRoomId(ROOM_ID, 3);

        // then — 최근 3개, ASC 정렬
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isLessThan(result.get(2).getId());
        assertThat(result.get(2).getContent()).isEqualTo("질문3"); // 가장 최신
    }

    @Test
    @DisplayName("findRecentByRoomId - 해당 채팅방 메시지 없음: 빈 리스트 반환")
    void findRecentByRoomId_emptyRoom() {
        // when
        List<ChatMessage> result = chatMessageRepository.findRecentByRoomId(999L, 20);

        // then
        assertThat(result).isEmpty();
    }
}