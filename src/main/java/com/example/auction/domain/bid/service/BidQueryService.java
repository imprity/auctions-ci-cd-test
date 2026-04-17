package com.example.auction.domain.bid.service;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.exception.AuctionErrorEnum;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.auction.result.entity.AuctionResult;
import com.example.auction.domain.auction.result.repository.AuctionResultRepository;
import com.example.auction.domain.bid.dto.response.BidListResponse;
import com.example.auction.domain.bid.dto.response.BidResponse;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.exceptions.BidErrorEnum;
import com.example.auction.domain.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 입찰 조회, 결과 조회, 내입찰조회
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BidQueryService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final AuctionResultRepository resultRepository;


    // 내 입찰 조회
    public PageResponse<BidListResponse> getMyBids(CustomUserDetails userDetails, Pageable pageable) {

        Long userId = userDetails.getUserId();

        // 내 입찰 목록 조회 (페이징)
        Page<BidListResponse> myBidPage = bidRepository.findAllByUserId(userId, pageable)
                .map(BidListResponse::from);

        log.info("[내 입찰 조회] userId={}, page={}, size={}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        return PageResponse.create(myBidPage);
    }

    // 특정 경매의 입찰조회
    public PageResponse<BidListResponse> getBids(CustomUserDetails userDetails, Long auctionId, Pageable pageable) {
        // 경매 존재 여부 및 상태 확인
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND));

        // 취소면 조회 불가
        if (auction.getStatus() == AuctionStatus.CANCELLED) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND);
        }

        Page<BidListResponse> bidPage = bidRepository.findAllByAuctionId(auctionId, pageable)
                .map(BidListResponse::from);

        return PageResponse.create(bidPage);
    }

    // 입찰 결과 조회(1건)
    public BidResponse getWinnerBid(CustomUserDetails userDetails, Long auctionId) {

        // 경매 존재 여부 및 상태 확인
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND));

        // done만 결과 조회 가능
        if (auction.getStatus() != AuctionStatus.DONE) {
            throw new ServiceErrorException(BidErrorEnum.AUCTION_RESULT_NOT_FOUND);
        }

        // 입찰 결과 1건 조회
        AuctionResult auctionResult = resultRepository.findByAuctionId(auctionId)
                .orElseThrow(() -> new ServiceErrorException(BidErrorEnum.AUCTION_RESULT_NOT_FOUND));

        // todo: 현재 auctionResult의 id를 가지고 bidRepository에 다시 가서 찾아오고 있음(사유: description 등 내용이 다름) -> 고도화 과정에서 재검토 필요
        return bidRepository.findById(auctionResult.getBidId())
                .map(BidResponse::of)
                .orElseThrow(() -> new ServiceErrorException(BidErrorEnum.BID_NOT_FOUND));

    }

    public BidResponse getCurrentMinBid(CustomUserDetails userDetails, Long auctionId) {
        // 경매 존재 여부 및 상태 확인
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND));

        // 진행 중이 아니면 조회 불가
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND);
        }

        Bid currentMin = bidRepository.findFirstByAuctionIdOrderByPriceAsc(auctionId)
                .orElseThrow(() -> new ServiceErrorException(BidErrorEnum.BID_NOT_FOUND));

        return BidResponse.of(currentMin);
    }
}
