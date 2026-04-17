package com.example.auction.domain.auction.scheduler;

import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.auction.result.entity.AuctionResult;
import com.example.auction.domain.auction.result.repository.AuctionResultRepository;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionResultRepository resultRepository;

    /*
     startedAt 이 지난 경매 시작 처리(READY -> ACTIVE)
     10초마다 실행
     */
    @Scheduled(fixedDelayString = "${scheduler.auction.delay}")
    @Transactional
    public void startAuctions() {
        LocalDateTime now = LocalDateTime.now();

        List<Auction> readyAuctions = auctionRepository.findAllByStatusAndStartedAtBefore(
                AuctionStatus.READY, now);

        for (Auction auction : readyAuctions) {
            auction.activate();
            log.info("[경매 시작] auctionId={}", auction.getId());
        }
    }

    // active -> done/nobid 처리
    // 경매결과(auctionResult) 생성 및 저장
    @Scheduled(fixedDelayString = "${scheduler.auction.delay}")
    @Transactional
    public void endAuctions() {

        LocalDateTime now = LocalDateTime.now();
        List<Auction> activeAuctions = auctionRepository.findAllByStatusAndEndedAtBefore(
                AuctionStatus.ACTIVE, now);

        for (Auction auction : activeAuctions) {
            Long auctionId = auction.getId();

            // 최저가 입찰 찾기
            Bid winnerBid = bidRepository.findFirstByAuctionIdOrderByPriceAsc(auctionId).orElse(null);

            if (winnerBid != null) {
                // 낙찰시 경매결과 생성 및 저장
                auction.close();
                AuctionResult auctionResult = AuctionResult.of(
                        winnerBid.getPrice(),
                        auctionId,
                        auction.getUserId(), // 구매자 (경매 생성자)
                        winnerBid.getUserId(), // 판매자 (낙찰 입찰자)
                        winnerBid.getId()
                );
                resultRepository.save(auctionResult);
                log.info("[경매 낙찰] auctionId={}", auctionId);
            } else {
                // 유찰
                auction.noBid();
                log.info("[경매 유찰] auctionId={}", auctionId);
            }
        }
    }
}
