package com.example.auction.domain.bid.service;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionProductCategory;
import com.example.auction.domain.auction.exception.AuctionErrorEnum;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.auction.result.entity.AuctionResult;
import com.example.auction.domain.auction.result.repository.AuctionResultRepository;
import com.example.auction.domain.bid.dto.response.BidListResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BidQueryServiceTest {

    @InjectMocks
    private BidQueryService queryService;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private AuctionResultRepository resultRepository;

    private CustomUserDetails userDetails;
    private Long auctionId;
    private Pageable pageable;
    private Auction activeAuction;
    private Auction doneAuction;
    private Auction cancelledAuction;

    @BeforeEach
    void setUp() {
        userDetails = new CustomUserDetails(1L, "USER");
        auctionId = 10L;
        pageable = PageRequest.of(0, 20);


        activeAuction = Auction.of(
                99L, "진행중 경매", BigDecimal.valueOf(200_000), "상품",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                AuctionProductCategory.ELECTRONICS
        );
        activeAuction.activate();

        doneAuction = Auction.of(
                99L, "낙찰 경매", BigDecimal.valueOf(200_000), "상품",
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                AuctionProductCategory.ELECTRONICS
        );
        doneAuction.activate();
        doneAuction.close();

        cancelledAuction = Auction.of(
                99L, "취소 경매", BigDecimal.valueOf(200_000), "상품",
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2),
                AuctionProductCategory.ELECTRONICS
        );
        cancelledAuction.cancel();
    }


    // ========================
    // 경매별 입찰 목록 조회
    // ========================

    @Test
    @DisplayName("입찰 목록 조회 성공 - 입찰 2건 반환")
    void getBids_success() {
        // given
        List<Bid> bids = List.of(
                Bid.of(null, BigDecimal.valueOf(100_000), auctionId, 2L, BidAuctionStatus.ACTIVE),
                Bid.of(null, BigDecimal.valueOf(150_000), auctionId, 3L, BidAuctionStatus.ACTIVE)
        );
        Page<Bid> bidPage = new PageImpl<>(bids, pageable, bids.size());

        given(bidRepository.findAllByAuctionId(auctionId, pageable)).willReturn(bidPage);
        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(activeAuction));

        // when
        PageResponse<BidListResponse> response = queryService.getBids(userDetails, auctionId, pageable);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.currentPage()).isEqualTo(0);
    }

    @Test
    @DisplayName("입찰 목록 조회 성공 - 입찰 없으면 빈 리스트 반환")
    void getBids_emptyList() {
        // given
        Page<Bid> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(bidRepository.findAllByAuctionId(auctionId, pageable)).willReturn(emptyPage);
        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(activeAuction));

        // when
        PageResponse<BidListResponse> response = queryService.getBids(userDetails, auctionId, pageable);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
    }


    @Test
    @DisplayName("존재하지 않는 경매 입찰 목록 조회 시 실패")
    void getBids_auctionNotFound_fail() {
        // given
        given(auctionRepository.findById(auctionId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> queryService.getBids(userDetails, auctionId, pageable))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuctionErrorEnum.AUCTION_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("취소된 경매 입찰 목록 조회 시 실패")
    void getBids_cancelledAuction_fail() {
        // given
        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(cancelledAuction));

        // when & then
        assertThatThrownBy(() -> queryService.getBids(userDetails, auctionId, pageable))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuctionErrorEnum.AUCTION_NOT_FOUND.getMessage());
    }


    // ========================
    // 내 입찰 목록 조회
    // ========================
    @Test
    @DisplayName("내 입찰 목록 조회 성공")
    void getMyBids_success() {
        // given
        List<Bid> myBids = List.of(
                Bid.of(null, BigDecimal.valueOf(100_000), auctionId, userDetails.getUserId(), BidAuctionStatus.ACTIVE),
                Bid.of(null, BigDecimal.valueOf(80_000), 20L, userDetails.getUserId(), BidAuctionStatus.ACTIVE)
        );
        Page<Bid> myBidPage = new PageImpl<>(myBids, pageable, myBids.size());

        given(bidRepository.findAllByUserId(userDetails.getUserId(), pageable)).willReturn(myBidPage);

        // when
        PageResponse<BidListResponse> response = queryService.getMyBids(userDetails, pageable);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.content())
                .allMatch(bid -> bid.getAuctionId() != null);
    }

    @Test
    @DisplayName("내 입찰 없으면 빈 리스트 반환")
    void getMyBids_empty() {
        // given
        Page<Bid> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(bidRepository.findAllByUserId(userDetails.getUserId(), pageable)).willReturn(emptyPage);

        // when
        PageResponse<BidListResponse> response = queryService.getMyBids(userDetails, pageable);

        // then
        assertThat(response.content()).isEmpty();
    }

    // ========================
    // 입찰 결과 조회
    // ========================
    @Test
    @DisplayName("입찰 결과 조회 성공 - 최저가 입찰 반환")
    void getWinnerBid_success() {
        // given
        AuctionResult auctionResult = AuctionResult.of(
                BigDecimal.valueOf(80_000), auctionId, 99L, 2L, 100L
        );
        // buyer: 입찰 주최자(구매자), seller: 입찰 참여자(판매자)
        Bid winnerBid = Bid.of(null, BigDecimal.valueOf(80_000), auctionId, 2L, BidAuctionStatus.ACTIVE);

        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(doneAuction));
        given(resultRepository.findByAuctionId(auctionId)).willReturn(Optional.of(auctionResult));
        given(bidRepository.findById(100L)).willReturn(Optional.of(winnerBid));

        // when
        BidResponse response = queryService.getWinnerBid(userDetails, auctionId);

        // then
        assertThat(response.getPrice()).isEqualTo(BigDecimal.valueOf(80_000));
        assertThat(response.getAuctionId()).isEqualTo(auctionId);
    }
    @Test
    @DisplayName("존재하지 않는 경매 결과 조회 시 실패")
    void getWinnerBid_auctionNotFound_fail() {
        // given
        given(auctionRepository.findById(auctionId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> queryService.getWinnerBid(userDetails, auctionId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuctionErrorEnum.AUCTION_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("DONE이 아닌 경매 결과 조회 시 실패 - ACTIVE")
    void getWinnerBid_notDone_active_fail() {
        // given
        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(activeAuction));

        // when & then
        assertThatThrownBy(() -> queryService.getWinnerBid(userDetails, auctionId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BidErrorEnum.AUCTION_RESULT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("DONE이 아닌 경매 결과 조회 시 실패 - NO_BID")
    void getWinnerBid_notDone_noBid_fail() {
        // given
        Auction noBidAuction = Auction.of(
                99L, "유찰 경매", BigDecimal.valueOf(200_000), "상품",
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                AuctionProductCategory.ELECTRONICS
        );
        noBidAuction.activate();
        noBidAuction.noBid();

        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(noBidAuction));

        // when & then
        assertThatThrownBy(() -> queryService.getWinnerBid(userDetails, auctionId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BidErrorEnum.AUCTION_RESULT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("AuctionResult 없으면 실패")
    void getWinnerBid_resultNotFound_fail() {
        // given
        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(doneAuction));
        given(resultRepository.findByAuctionId(auctionId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> queryService.getWinnerBid(userDetails, auctionId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BidErrorEnum.AUCTION_RESULT_NOT_FOUND.getMessage());
    }


    // ========================
    // 현재 최저가 입찰 조회
    // ========================

    @Test
    @DisplayName("현재 최저가 입찰 조회 성공")
    void getCurrentMinBid_success() {
        // given
        Bid minBid = Bid.of(null, BigDecimal.valueOf(80_000), auctionId, 2L, BidAuctionStatus.ACTIVE);

        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(activeAuction));
        given(bidRepository.findFirstByAuctionIdOrderByPriceAsc(auctionId)).willReturn(Optional.of(minBid));

        // when
        BidResponse response = queryService.getCurrentMinBid(userDetails, auctionId);

        // then
        assertThat(response.getPrice()).isEqualTo(BigDecimal.valueOf(80_000));
        assertThat(response.getAuctionId()).isEqualTo(auctionId);
    }

    @Test
    @DisplayName("존재하지 않는 경매 최저가 조회 시 실패")
    void getCurrentMinBid_auctionNotFound_fail() {
        // given
        given(auctionRepository.findById(auctionId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> queryService.getCurrentMinBid(userDetails, auctionId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuctionErrorEnum.AUCTION_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("ACTIVE가 아닌 경매 최저가 조회 시 실패 - DONE")
    void getCurrentMinBid_notActive_done_fail() {
        // given
        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(doneAuction));

        // when & then
        assertThatThrownBy(() -> queryService.getCurrentMinBid(userDetails, auctionId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuctionErrorEnum.AUCTION_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("ACTIVE가 아닌 경매 최저가 조회 시 실패 - CANCELLED")
    void getCurrentMinBid_notActive_cancelled_fail() {
        // given
        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(cancelledAuction));

        // when & then
        assertThatThrownBy(() -> queryService.getCurrentMinBid(userDetails, auctionId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuctionErrorEnum.AUCTION_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("입찰이 없으면 최저가 조회 실패")
    void getCurrentMinBid_noBid_fail() {
        // given
        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(activeAuction));
        given(bidRepository.findFirstByAuctionIdOrderByPriceAsc(auctionId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> queryService.getCurrentMinBid(userDetails, auctionId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BidErrorEnum.BID_NOT_FOUND.getMessage());
    }
}