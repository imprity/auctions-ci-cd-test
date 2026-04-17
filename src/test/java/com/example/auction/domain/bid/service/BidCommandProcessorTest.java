package com.example.auction.domain.bid.service;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionProductCategory;
import com.example.auction.domain.auction.exception.AuctionErrorEnum;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.bid.dto.request.BidRequest;
import com.example.auction.domain.bid.dto.response.BidResponse;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.example.auction.domain.bid.exceptions.BidErrorEnum;
import com.example.auction.domain.bid.repository.BidRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BidCommandProcessorTest {

    @InjectMocks
    private BidCommandProcessor processor;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionRepository auctionRepository;

    private CustomUserDetails userDetails;
    private Long auctionId;
    private Auction activeAuction;

    @BeforeEach
    void setUp() {
        userDetails = new CustomUserDetails(1L, "USER");  // 입찰자(판매자) userId = 1
        auctionId = 10L;

        // 기본 경매 생성
        activeAuction = Auction.of(
                99L,
                "테스트 경매",
                BigDecimal.valueOf(200_000),
                "테스트 상품",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                AuctionProductCategory.ELECTRONICS
        );
        activeAuction.activate();
    }

    // ========================
    // 입찰 생성 성공 케이스
    // ========================
    @Test
    @DisplayName("첫 입찰 - 현재 입찰 없을 때 max_price 이하면 성공")
    void firstBid_success() {
        // given
        BidRequest request = new BidRequest(BigDecimal.valueOf(150_000), null);
        Bid savedBid = Bid.of(null, BigDecimal.valueOf(150_000), auctionId, userDetails.getUserId(), BidAuctionStatus.ACTIVE);

        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(activeAuction));
        given(bidRepository.findMinPriceByAuctionId(auctionId)).willReturn(Optional.empty());
        given(bidRepository.save(any(Bid.class))).willReturn(savedBid);

        // when
        BidResponse response = processor.placeBid(userDetails, auctionId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPrice()).isEqualTo(BigDecimal.valueOf(150_000));
        assertThat(response.getAuctionId()).isEqualTo(auctionId);
    }

    @Test
    @DisplayName("최대 가격과 정확히 같은 금액으로 입찰 시 성공")
    void bidEqualsMaxPrice_success() {

        // given
        BidRequest request = new BidRequest(BigDecimal.valueOf(150_000), null);
        Bid savedBid = Bid.of(null, BigDecimal.valueOf(150_000), auctionId, userDetails.getUserId(), BidAuctionStatus.ACTIVE);

        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(activeAuction));
        given(bidRepository.findMinPriceByAuctionId(auctionId)).willReturn(Optional.empty());
        given(bidRepository.save(any(Bid.class))).willReturn(savedBid);

        // when
        BidResponse response = processor.placeBid(userDetails, auctionId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPrice()).isEqualTo(BigDecimal.valueOf(150_000));
        assertThat(response.getAuctionId()).isEqualTo(auctionId);


    }

    @Test
    @DisplayName("현재 최저가보다 낮은 가격으로 입찰 성공")
    void bidLowerThanCurrentMin_success() {
        // given
        BigDecimal currentMinPrice = BigDecimal.valueOf(150000);
        BigDecimal newBidPrice = BigDecimal.valueOf(100000);  // 현재 최저가보다 낮음
        BidRequest request = new BidRequest(newBidPrice, null);
        Bid savedBid = Bid.of(null, newBidPrice, auctionId, userDetails.getUserId(), BidAuctionStatus.ACTIVE);

        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(activeAuction));
        given(bidRepository.findMinPriceByAuctionId(auctionId)).willReturn(Optional.of(currentMinPrice));
        given(bidRepository.save(any(Bid.class))).willReturn(savedBid);

        // when
        BidResponse response = processor.placeBid(userDetails, auctionId, request);

        // then
        assertThat(response.getPrice()).isEqualTo(newBidPrice);
    }


    // ========================
    // 입찰 생성 실패 케이스
    // ========================

    @Test
    @DisplayName("현재 최저가와 같은 가격으로 입찰 시 실패")
    void bidSameAsCurrentMin_fail() {
        // given
        BigDecimal currentMinPrice = BigDecimal.valueOf(150_000);
        BidRequest request = new BidRequest(currentMinPrice, null);

        given(bidRepository.findMinPriceByAuctionId(auctionId)).willReturn(Optional.of(currentMinPrice));
        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(activeAuction));

        // when & then
        assertThatThrownBy(() -> processor.placeBid(userDetails, auctionId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BidErrorEnum.BID_PRICE_NOT_LOWER.getMessage());
    }

    @Test
    @DisplayName("현재 최저가보다 높은 가격으로 입찰 시 실패")
    void bidHigherThanCurrentMin_fail() {
        // given
        BigDecimal currentMinPrice = BigDecimal.valueOf(150000);
        BidRequest request = new BidRequest(BigDecimal.valueOf(200_000), null);  // 최저가보다 높음

        given(bidRepository.findMinPriceByAuctionId(auctionId)).willReturn(Optional.of(currentMinPrice));
        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(activeAuction));

        // when & then
        assertThatThrownBy(() -> processor.placeBid(userDetails, auctionId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BidErrorEnum.BID_PRICE_NOT_LOWER.getMessage());
    }

    @Test
    @DisplayName("READY 상태 경매에 입찰 시 실패")
    void auction_ready_bid_fail() {
        // given
        BidRequest request = new BidRequest(BigDecimal.valueOf(150_000), null);
        Auction readyAuction = Auction.of(
                99L, "테스트", BigDecimal.valueOf(200_000), "상품",
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2),
                AuctionProductCategory.ELECTRONICS
        );
        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(readyAuction));

        // when & then
        assertThatThrownBy(() -> processor.placeBid(userDetails, auctionId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuctionErrorEnum.AUCTION_INVALID_STATUS.getMessage());
    }

    @Test
    @DisplayName("DONE 상태 경매에 입찰 시 실패")
    void auction_done_bid_fail() {
        // given
        BidRequest request = new BidRequest(BigDecimal.valueOf(150_000), null);
        Auction doneAuction = Auction.of(
                99L, "테스트", BigDecimal.valueOf(200_000), "상품",
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                AuctionProductCategory.ELECTRONICS
        );
        doneAuction.activate();
        doneAuction.close();
        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(doneAuction));

        // when & then
        assertThatThrownBy(() -> processor.placeBid(userDetails, auctionId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuctionErrorEnum.AUCTION_INVALID_STATUS.getMessage());
    }

    @Test
    @DisplayName("본인 경매에 입찰 시 실패")
    void selfBid_fail() {
        // given
        BidRequest request = new BidRequest(BigDecimal.valueOf(150_000), null);
        Auction myAuction = Auction.of(
                1L, "내 경매", BigDecimal.valueOf(200_000), "상품",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                AuctionProductCategory.ELECTRONICS
        );
        myAuction.activate();
        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(myAuction));

        // when & then
        assertThatThrownBy(() -> processor.placeBid(userDetails, auctionId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BidErrorEnum.BID_FORBIDDEN_SELF_BID.getMessage());
    }

    @Test
    @DisplayName("최대 가격 초과 입찰 시 실패")
    void bidExceedsMaxPrice_fail() {
        // given
        BidRequest request = new BidRequest(BigDecimal.valueOf(250_000), null); // max_price(200_000) 초과
        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(activeAuction));

        // when & then
        assertThatThrownBy(() -> processor.placeBid(userDetails, auctionId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BidErrorEnum.BID_PRICE_EXCEEDS_MAX.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 경매에 입찰 시 실패")
    void auctionNotFound_fail() {
        BidRequest request = new BidRequest(BigDecimal.valueOf(150_000), null);
        given(auctionRepository.findById(auctionId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> processor.placeBid(userDetails, auctionId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuctionErrorEnum.AUCTION_NOT_FOUND.getMessage());
    }

}