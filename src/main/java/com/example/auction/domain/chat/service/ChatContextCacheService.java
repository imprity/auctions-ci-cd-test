package com.example.auction.domain.chat.service;

import com.example.auction.domain.chat.dto.ChatMessageCacheDto;
import com.example.auction.domain.chat.repository.ChatMessageRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatContextCacheService {

    private static final String KEY_PREFIX = "chat:context:";
    private static final int MAX_MESSAGES = 20;      // AI 컨텍스트로 넘길 최대 메시지 수
    private static final Duration TTL = Duration.ofHours(24); // 마지막 갱신 후 24시간 유지

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatMessageRepository chatMessageRepository;

    // AI 호출 전 컨텍스트 로드 — 캐시 히트 시 Redis, 미스 시 DB 조회 후 캐싱
    // Redis 장애 시 예외를 삼키고 DB 폴백 — Redis 문제가 채팅을 막지 않도록
    public List<ChatMessageCacheDto> getContext(Long roomId) {
        String key = KEY_PREFIX + roomId;

        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                List<ChatMessageCacheDto> cached = deserialize(key, json);
                if (cached != null) {
                    return cached;
                }
                // deserialize 실패(손상) — DB 재조회로 폴백
            }
        } catch (Exception e) {
            log.warn("[ChatContextCacheService] Redis 조회 실패, DB 폴백 key={}", key, e);
        }

        // 캐시 미스 or Redis 장애 — DB에서 최근 N개 조회 후 캐시 적재 (첫 메시지 or Redis 만료 후)
        List<ChatMessageCacheDto> messages = chatMessageRepository.findRecentByRoomId(roomId, MAX_MESSAGES)
                .stream()
                .map(m -> new ChatMessageCacheDto(m.getRole().name(), m.getContent()))
                .toList();

        if (!messages.isEmpty()) {
            save(key, messages);
        }

        return messages;
    }

    // AI 응답 완료 후 유저 메시지 + AI 응답을 캐시에 추가 — doFinally에서 호출
    // Redis 장애 시 무시 — 다음 턴 getContext()의 DB 폴백으로 복구됨
    // TODO: GET→수정→SET 방식이라 동시 요청 시 마지막 write가 앞선 turn을 덮어쓸 수 있음 (docs/known-issues.md SSE-2)
    public void appendMessages(Long roomId, String userContent, String assistantContent) {
        String key = KEY_PREFIX + roomId;

        try {
            String json = stringRedisTemplate.opsForValue().get(key);

            List<ChatMessageCacheDto> parsed = (json != null) ? deserialize(key, json) : null;
            List<ChatMessageCacheDto> messages = new ArrayList<>(parsed != null ? parsed : List.of());
            messages.add(new ChatMessageCacheDto("USER", userContent));
            messages.add(new ChatMessageCacheDto("ASSISTANT", assistantContent));

            // MAX_MESSAGES 초과 시 오래된 것부터 제거 (슬라이딩 윈도우)
            if (messages.size() > MAX_MESSAGES) {
                messages = messages.subList(messages.size() - MAX_MESSAGES, messages.size());
            }

            save(key, messages);
        } catch (Exception e) {
            log.warn("[ChatContextCacheService] 캐시 추가 실패, 다음 턴 DB 폴백으로 복구 key={}", key, e);
        }
    }

    // 채팅방 삭제 시 컨텍스트 캐시 제거
    // Redis 장애 시 무시 — 채팅방 삭제 자체는 성공해야 함
    public void evict(Long roomId) {
        try {
            stringRedisTemplate.delete(KEY_PREFIX + roomId);
        } catch (Exception e) {
            log.warn("[ChatContextCacheService] 캐시 evict 실패 roomId={}", roomId, e);
        }
    }

    // JSON 직렬화 후 Redis에 저장 + TTL 갱신 — best-effort, 모든 예외 흡수
    private void save(String key, List<ChatMessageCacheDto> messages) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(messages), TTL);
        } catch (Exception e) {
            log.warn("[ChatContextCacheService] 캐시 저장 실패 key={}", key, e);
        }
    }

    // JSON 역직렬화 — 파싱 실패 시 null 반환 (호출부에서 손상된 키 삭제 후 DB 재조회)
    private List<ChatMessageCacheDto> deserialize(String key, String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JacksonException e) {
            log.warn("[ChatContextCacheService] 캐시 역직렬화 실패, 손상된 키 제거 후 DB 재조회 key={}", key, e);
            stringRedisTemplate.delete(key);
            return null;
        }
    }
}