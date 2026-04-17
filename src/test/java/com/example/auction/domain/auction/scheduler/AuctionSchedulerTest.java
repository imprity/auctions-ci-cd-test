package com.example.auction.domain.auction.scheduler;

import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionProductCategory;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.auction.result.repository.AuctionResultRepository;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.example.auction.domain.bid.repository.BidRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuctionSchedulerTest {

    @InjectMocks
    private AuctionScheduler auctionScheduler;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionResultRepository resultRepository;

    private Auction makeAuction(AuctionStatus status, LocalDateTime startedAt, LocalDateTime endedAt) {
        return Auction.of(
                1L,
                "테스트 경매",
                BigDecimal.valueOf(100_000),
                "테스트 상품",
                startedAt,
                endedAt,
                AuctionProductCategory.ELECTRONICS
        );
    }

    @Test
    @DisplayName("startedAt이 지난 READY 경매는 ACTIVE로 바뀐다")
    void readyAuction_Active() {
        // given
        // 과거 시간으로 시작일 세팅 및 테스트용객체 생성
        LocalDateTime past = LocalDateTime.now().minusMinutes(1);
        // 시작시간: 현재로부터 1분전, 끝나는시간: 현재로부터 59분후(시작시간으로부터 1시간후)
        Auction auction = makeAuction(AuctionStatus.READY, past, past.plusHours(1));

        given(auctionRepository.findAllByStatusAndStartedAtBefore(
                eq(AuctionStatus.READY), any(LocalDateTime.class)))
                .willReturn(List.of(auction));

        // when
        auctionScheduler.startAuctions();

        // then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
    }

    @Test
    @DisplayName("전환할 READY 경매가 없으면 아무것도 안 한다")
    void noReadyAuction_doesNothing() {
        // given
        given(auctionRepository.findAllByStatusAndStartedAtBefore(
                eq(AuctionStatus.READY), any(LocalDateTime.class)))
                .willReturn(List.of());

        // when & then (예외 없이 정상 종료)
        auctionScheduler.startAuctions();
    }

    @Test
    @DisplayName("입찰이 있으면 DONE(낙찰)으로 바뀐다")
    void activeAuctionWithBid_Done() {
        // given
        LocalDateTime past = LocalDateTime.now().minusMinutes(1);
        Auction auction = makeAuction(AuctionStatus.READY, past.minusHours(1), past);
        // ACTIVE 상태로 세팅
        auction.activate();

        given(auctionRepository.findAllByStatusAndEndedAtBefore(
                eq(AuctionStatus.ACTIVE), any(LocalDateTime.class)))
                .willReturn(List.of(auction));
        Bid winnerBid = Bid.of(
                null, BigDecimal.valueOf(50_000), auction.getId(), 2L, BidAuctionStatus.ACTIVE);

        given(bidRepository.findFirstByAuctionIdOrderByPriceAsc(any()))
                .willReturn(Optional.of(winnerBid));

        // when
        auctionScheduler.endAuctions();

        // then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.DONE);
    }

    @Test
    @DisplayName("입찰이 없으면 NO_BID(유찰)로 바뀐다")
    void activeAuctionWithoutBid_NoBid() {
        // given
        LocalDateTime past = LocalDateTime.now().minusMinutes(1);
        Auction auction = makeAuction(AuctionStatus.READY, past.minusHours(1), past);
        auction.activate();

        given(auctionRepository.findAllByStatusAndEndedAtBefore(
                eq(AuctionStatus.ACTIVE), any(LocalDateTime.class)))
                .willReturn(List.of(auction));
        given(bidRepository.findFirstByAuctionIdOrderByPriceAsc(any()))
                .willReturn(Optional.empty());

        // when
        auctionScheduler.endAuctions();

        // then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.NO_BID);
    }

    @Test
    @DisplayName("종료할 ACTIVE 경매가 없으면 아무것도 안 한다")
    void noActiveAuction_doesNothing() {
        // given
        given(auctionRepository.findAllByStatusAndEndedAtBefore(
                eq(AuctionStatus.ACTIVE), any(LocalDateTime.class)))
                .willReturn(List.of());

        // when & then (예외 없이 정상 종료)
        auctionScheduler.endAuctions();
    }

}
