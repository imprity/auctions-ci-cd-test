package com.example.auction.domain.chat.scheduler;

import com.example.auction.domain.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatScheduler {

    private final ChatMessageRepository chatMessageRepository;

    // 매일 자정 30일 이전 메시지 자동 삭제
    // 시간 같은 경우는 나중에 비즈니스 로직 협의 이후 변경 예정
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void deleteOldMessages() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        chatMessageRepository.deleteAllByCreatedAtBefore(threshold);
        log.info("[ChatScheduler] 30일 이전 메시지 삭제 완료 | 기준시각: {}", threshold);
    }
}