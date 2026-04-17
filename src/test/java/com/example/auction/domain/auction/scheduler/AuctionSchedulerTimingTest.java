package com.example.auction.domain.auction.scheduler;

import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionProductCategory;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.repository.AuctionRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스케줄러 타이밍 측정 통합 테스트
 *
 * 실행 전 필수: docker compose -f docker-compose-test.yml up -d
 * -f 해야 docker-compose.yml이 아니라 test.yml을 읽음
 *
 * 측정 항목:
 * - 경매 종료 예정 시간 vs 실제 스케줄러가 처리한 시간 간의 차이
 * - 평균, 최소, 최대 지연 시간
 */

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class AuctionSchedulerTimingTest {

    @Autowired
    private AuctionRepository auctionRepository;

    // 테스트 후 정리
    @AfterEach
    void tearDown() {
        auctionRepository.deleteAll();
    }

    @Test
    @DisplayName("경매 시작 처리 지연 시간 측정")
    void measureActivationDelay() throws InterruptedException {
        int auctionCount = 5;
        List<Long> auctionIds = new ArrayList<>();
        List<LocalDateTime> scheduledStartTimes = new ArrayList<>();

        for (int i = 0; i < auctionCount; i++) {
            // 다양한 시작 시간
            LocalDateTime startedAt = LocalDateTime.now().plusSeconds(3 + i);
            Auction auction = Auction.of(
                    1L,
                    "시작 테스트 경매 " + i,
                    BigDecimal.valueOf(100_000),
                    "테스트 상품 " + i,
                    startedAt,
                    startedAt.plusHours(1),
                    AuctionProductCategory.ELECTRONICS
            );
            auctionIds.add(auctionRepository.save(auction).getId());
            scheduledStartTimes.add(startedAt);
        }

        // when - 스케줄러가 처리할 때까지 대기 (최대 30초)
        LocalDateTime checkStart = LocalDateTime.now();
        int maxWaitSeconds = 30;
        List<Long> delayMsList = new ArrayList<>();

        while (Duration.between(checkStart, LocalDateTime.now()).getSeconds() < maxWaitSeconds) {
            // 스케줄러가 처리했는지 확인하고 안했으면 1초 대기
            TimeUnit.SECONDS.sleep(1);

            //몇개가 active로 바뀌었는지 확인
            long activeCount = auctionRepository.findAll().stream()
                    .filter(a -> auctionIds.contains(a.getId()))
                    .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
                    .count();

            // 다 바뀌었으면 완료 시간 재고, 각 경매별 소요시간 확인
            if (activeCount == auctionCount) {
                LocalDateTime processedAt = LocalDateTime.now();
                for (LocalDateTime scheduledStart : scheduledStartTimes) {
                    long delay = Duration.between(scheduledStart, processedAt).toMillis();
                    delayMsList.add(delay);
                }
                break;
            }
        }

        // then
        assertThat(delayMsList).hasSize(auctionCount);

        // 통계 계산 LongSummaryStatistics
        LongSummaryStatistics stats = delayMsList.stream()
                .mapToLong(Long::longValue)
                .summaryStatistics();

        log.info("====================================");
        log.info("[READY -> ACTIVE 타이밍 측정] 경매 {}개", auctionCount);
        log.info("평균 지연: {}ms ({}초)", (long) stats.getAverage(), (long) stats.getAverage() / 1000); // 밀리초->초 변환 1000
        log.info("최소 지연: {}ms ({}초)", stats.getMin(), stats.getMin() / 1000);
        log.info("최대 지연: {}ms ({}초)", stats.getMax(), stats.getMax() / 1000);
        log.info("스케줄러 주기(10초) 대비 최대 오차: {}ms", stats.getMax() - 10_000); // 10000이 10초
        log.info("====================================");

        // 최대 지연이 15초인지도 확인
        assertThat(stats.getMax()).isLessThan(15_000);
    }

    @Test
    @DisplayName("경매 종료 처리 지연 시간 측정")
    void measureClosingDelay() throws InterruptedException {
        // given: 10개생성
        int auctionCount = 10;
        List<Auction> auctions = new ArrayList<>();
        List<LocalDateTime> scheduledEndTimes = new ArrayList<>();

        for (int i = 0; i < auctionCount; i++) {
            LocalDateTime endedAt = LocalDateTime.now().plusSeconds(3 + i);
            Auction auction = Auction.of(
                    1L,
                    "종료 테스트 경매 " + i,
                    BigDecimal.valueOf(100_000),
                    "테스트 상품 " + i,
                    LocalDateTime.now().minusHours(1), // 이미 시작됨
                    endedAt,
                    AuctionProductCategory.ELECTRONICS
            );
            auction.activate();
            auctions.add(auctionRepository.save(auction));
            scheduledEndTimes.add(endedAt);
        }

        List<Long> auctionIds = auctions.stream().map(Auction::getId).toList();

        // when - 스케줄러가 처리할 때까지 대기 (최대 60초)
        LocalDateTime checkStart = LocalDateTime.now();
        int maxWaitSeconds = 60;
        // 각 경매별 처리된 시간을 기록
        List<Long> delayMsList = new ArrayList<>();
        List<Long> processedIds = new ArrayList<>();

        while (Duration.between(checkStart, LocalDateTime.now()).getSeconds() < maxWaitSeconds) {
            TimeUnit.SECONDS.sleep(1);
            LocalDateTime now = LocalDateTime.now();

            // 루프에서 새로 처리된 경매만 지연 계산
            List<Auction> allAuctions = auctionRepository.findAll();
            for (int i = 0; i < auctionIds.size(); i++) {
                Long auctionId = auctionIds.get(i);
                if (processedIds.contains(auctionId)) continue;

                // 새로 처리된 경매이고 Done 또는 nobid인 경우를 찾아서 delay 계산
                boolean isDone = allAuctions.stream()
                        .filter(a -> a.getId().equals(auctionId))
                        .anyMatch(a -> a.getStatus() == AuctionStatus.DONE
                                || a.getStatus() == AuctionStatus.NO_BID);

                if (isDone) {
                    long delay = Duration.between(scheduledEndTimes.get(i), now).toMillis();
                    delayMsList.add(delay);
                    processedIds.add(auctionId);
                }
            }

            if (processedIds.size() == auctionCount) break;
        }

        // then
        assertThat(delayMsList).hasSize(auctionCount);

        LongSummaryStatistics stats = delayMsList.stream()
                .mapToLong(Long::longValue)
                .summaryStatistics();

        log.info("====================================");
        log.info("[ACTIVE -> DONE/NO_BID 타이밍 측정] 경매 {}개", auctionCount);
        log.info("평균 지연: {}ms ({}초)", (long) stats.getAverage(), (long) stats.getAverage() / 1000);
        log.info("최소 지연: {}ms ({}초)", stats.getMin(), stats.getMin() / 1000);
        log.info("최대 지연: {}ms ({}초)", stats.getMax(), stats.getMax() / 1000);
        log.info("스케줄러 주기(10초) 대비 최대 오차: {}ms", stats.getMax() - 10_000);
        log.info("====================================");

        assertThat(stats.getMax()).isLessThan(15_000);
    }
}