package com.example.auction.domain.chat.service;

import com.example.auction.domain.chat.dto.ChatMessageCacheDto;
import com.example.auction.domain.chat.entity.ChatMessage;
import com.example.auction.domain.chat.entity.MessageRole;
import com.example.auction.domain.chat.repository.ChatMessageRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatContextCacheServiceTest {

    @InjectMocks
    private ChatContextCacheService chatContextCacheService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private static final Long ROOM_ID = 1L;
    private static final String KEY = "chat:context:1";


    // ========================
    // getContext
    // ========================

    @Test
    @DisplayName("getContext - 캐시 히트: Redis에 데이터 있으면 역직렬화해서 반환")
    void getContext_cacheHit() throws Exception {
        // given
        List<ChatMessageCacheDto> cached = List.of(
                new ChatMessageCacheDto("USER", "안녕"),
                new ChatMessageCacheDto("ASSISTANT", "안녕하세요")
        );
        String json = objectMapper.writeValueAsString(cached);

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(KEY)).willReturn(json);

        // when
        List<ChatMessageCacheDto> result = chatContextCacheService.getContext(ROOM_ID);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).role()).isEqualTo("USER");
        assertThat(result.get(0).content()).isEqualTo("안녕");
        assertThat(result.get(1).role()).isEqualTo("ASSISTANT");
        assertThat(result.get(1).content()).isEqualTo("안녕하세요");
    }

    @Test
    @DisplayName("getContext - 캐시 미스, DB에 데이터 있음: DB 조회 후 Redis에 저장 후 반환")
    void getContext_cacheMiss_dbHasData() throws Exception {
        // given
        ChatMessage msg1 = ChatMessage.of(ROOM_ID, "안녕", MessageRole.USER);
        ChatMessage msg2 = ChatMessage.of(ROOM_ID, "안녕하세요", MessageRole.ASSISTANT);

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(KEY)).willReturn(null);
        given(chatMessageRepository.findRecentByRoomId(ROOM_ID, 20)).willReturn(List.of(msg1, msg2));

        // when
        List<ChatMessageCacheDto> result = chatContextCacheService.getContext(ROOM_ID);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).role()).isEqualTo("USER");
        assertThat(result.get(1).role()).isEqualTo("ASSISTANT");

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(KEY), jsonCaptor.capture(), eq(Duration.ofHours(24)));

        List<ChatMessageCacheDto> saved = objectMapper.readValue(jsonCaptor.getValue(), new TypeReference<>() {});
        assertThat(saved).hasSize(2);
    }

    @Test
    @DisplayName("getContext - 캐시 미스, DB도 비어있음: 빈 리스트 반환 + Redis 저장 안 함")
    void getContext_cacheMiss_dbEmpty() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(KEY)).willReturn(null);
        given(chatMessageRepository.findRecentByRoomId(ROOM_ID, 20)).willReturn(List.of());

        // when
        List<ChatMessageCacheDto> result = chatContextCacheService.getContext(ROOM_ID);

        // then
        assertThat(result).isEmpty();
        verify(valueOperations, never()).set(anyString(), anyString(), any());
    }


    // ========================
    // appendMessages
    // ========================

    @Test
    @DisplayName("appendMessages - 기존 캐시 있음: 기존 메시지 유지 + 새 메시지 2개 추가")
    void appendMessages_withExistingCache() throws Exception {
        // given
        List<ChatMessageCacheDto> existing = List.of(
                new ChatMessageCacheDto("USER", "이전 질문"),
                new ChatMessageCacheDto("ASSISTANT", "이전 답변")
        );
        String existingJson = objectMapper.writeValueAsString(existing); // given() 밖에서 먼저 직렬화

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(KEY)).willReturn(existingJson);

        // when
        chatContextCacheService.appendMessages(ROOM_ID, "새 질문", "새 답변");

        // then — 기존 2개 + 신규 2개 = 4개, 순서 확인
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(KEY), jsonCaptor.capture(), eq(Duration.ofHours(24)));

        List<ChatMessageCacheDto> saved = objectMapper.readValue(jsonCaptor.getValue(), new TypeReference<>() {});
        assertThat(saved).hasSize(4);
        assertThat(saved.get(2).role()).isEqualTo("USER");
        assertThat(saved.get(2).content()).isEqualTo("새 질문");
        assertThat(saved.get(3).role()).isEqualTo("ASSISTANT");
        assertThat(saved.get(3).content()).isEqualTo("새 답변");
    }

    @Test
    @DisplayName("appendMessages - 기존 캐시 없음: 빈 리스트에서 시작해 새 메시지 2개 저장")
    void appendMessages_noExistingCache() throws Exception {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(KEY)).willReturn(null);

        // when
        chatContextCacheService.appendMessages(ROOM_ID, "질문", "답변");

        // then
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(KEY), jsonCaptor.capture(), eq(Duration.ofHours(24)));

        List<ChatMessageCacheDto> saved = objectMapper.readValue(jsonCaptor.getValue(), new TypeReference<>() {});
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).role()).isEqualTo("USER");
        assertThat(saved.get(1).role()).isEqualTo("ASSISTANT");
    }

    @Test
    @DisplayName("appendMessages - MAX_MESSAGES(20) 초과 시 슬라이딩 윈도우: 오래된 메시지 제거")
    void appendMessages_slidingWindow() throws Exception {
        // given — 19개 기존 메시지 + 2개 추가 = 21개 → 20개로 트림
        List<ChatMessageCacheDto> existing = IntStream.range(0, 19)
                .mapToObj(i -> new ChatMessageCacheDto(i % 2 == 0 ? "USER" : "ASSISTANT", "메시지" + i))
                .toList();
        String existingJson = objectMapper.writeValueAsString(existing); // given() 밖에서 먼저 직렬화

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(KEY)).willReturn(existingJson);

        // when
        chatContextCacheService.appendMessages(ROOM_ID, "새 질문", "새 답변");

        // then — 20개 유지, 가장 오래된 "메시지0" 제거, 마지막이 새 메시지
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(KEY), jsonCaptor.capture(), eq(Duration.ofHours(24)));

        List<ChatMessageCacheDto> saved = objectMapper.readValue(jsonCaptor.getValue(), new TypeReference<>() {});
        assertThat(saved).hasSize(20);
        assertThat(saved.get(0).content()).isEqualTo("메시지1"); // 메시지0 제거됨
        assertThat(saved.get(18).content()).isEqualTo("새 질문");
        assertThat(saved.get(19).content()).isEqualTo("새 답변");
    }


    // ========================
    // evict
    // ========================

    @Test
    @DisplayName("evict - 채팅방 삭제 시 Redis 키 제거")
    void evict_deletesRedisKey() {
        // when
        chatContextCacheService.evict(ROOM_ID);

        // then
        verify(stringRedisTemplate).delete(KEY);
    }
}