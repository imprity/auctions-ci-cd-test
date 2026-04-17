package com.example.auction.domain.bid.service;


import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionProductCategory;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.bid.dto.request.BidRequest;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.repository.BidRepository;
import com.example.auction.common.config.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** *
 * 입찰 동시성 테스트 - Before(v1)과 After(v2) 비교, 통합 테스트로 진행
 *
 * <테스트 시나리오>
 * 50명이 동시에 같은 경매에 80,000원으로 입찰했을 경우
 * v1(락 없음): 중복 최저가 발생 가능
 * v2(분산락): 1건만 최저가로 저장
 */

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class BidConcurrencyTest {

    @Autowired
    private BidCommandFacade bidCommandFacade;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @AfterEach
    void tearDown() {
        bidRepository.deleteAll();
        auctionRepository.deleteAll();
    }

    private Auction createActiveAuction(Long userId) {
        Auction auction = Auction.of(
                userId,
                "동시성 테스트 경매",
                BigDecimal.valueOf(200_000),
                "테스트 상품",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                AuctionProductCategory.ELECTRONICS
        );
        auction.activate();
        return auctionRepository.save(auction);
    }

    @Test
    @DisplayName("v1 -50명 동시 입찰 시 중복 최저가 발생")
    void concurrency_v1_noLock() throws InterruptedException {
        // given
        int threadCount = 50;
        Auction auction = createActiveAuction(99L);
        Long auctionId = auction.getId();
        BigDecimal bidPrice = BigDecimal.valueOf(80_000);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 50명 동시 입찰
        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;
            executor.submit(() -> {
                try {
                    CustomUserDetails userDetails = new CustomUserDetails(userId, "USER");
                    BidRequest request = new BidRequest(bidPrice, null);
                    bidCommandFacade.placeBid(userDetails, auctionId, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // then
        List<Bid> bids = bidRepository.findAll().stream()
                .filter(b -> b.getAuctionId().equals(auctionId))
                .toList();

        long minPriceCount = bids.stream()
                .filter(b -> b.getPrice().compareTo(bidPrice) == 0)
                .count();

        log.info("====================================");
        log.info("[v1 - 락 없음] 동시 입찰 {}명", threadCount);
        log.info("성공: {}건, 실패: {}건", successCount.get(), failCount.get());
        log.info("총 입찰 저장: {}건", bids.size());
        log.info("80,000원 중복 저장: {}건 (1건이 정상)", minPriceCount);
        log.info("====================================");

        // v1은 중복이 발생할 수 있음을 확인 (1건 초과 가능)
        assertThat(bids.size()).isGreaterThan(1);
    }

    @Test
    @DisplayName("v2(분산락) 50명 동시 입찰 시 정확히 1건만 저장")
    void concurrency_v2_withLock() throws InterruptedException {
        // given
        int threadCount = 50;
        Auction auction = createActiveAuction(99L);
        Long auctionId = auction.getId();
        BigDecimal bidPrice = BigDecimal.valueOf(80_000);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 50명 동시 입찰
        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;
            executor.submit(() -> {
                try {
                    CustomUserDetails userDetails = new CustomUserDetails(userId, "USER");
                    BidRequest request = new BidRequest(bidPrice, null);
                    bidCommandFacade.placeBidDis(userDetails, auctionId, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // then
        List<Bid> bids = bidRepository.findAll().stream()
                .filter(b -> b.getAuctionId().equals(auctionId))
                .toList();

        long minPriceCount = bids.stream()
                .filter(b -> b.getPrice().compareTo(bidPrice) == 0)
                .count();

        log.info("====================================");
        log.info("[v2 - 분산락] 동시 입찰 {}명", threadCount);
        log.info("성공: {}건, 실패: {}건", successCount.get(), failCount.get());
        log.info("총 입찰 저장: {}건", bids.size());
        log.info("80,000원 저장: {}건 (1건이 정상)", minPriceCount);
        log.info("====================================");

        // v2는 정확히 1건만 저장되어야 함
        assertThat(minPriceCount).isEqualTo(1);
    }
}
